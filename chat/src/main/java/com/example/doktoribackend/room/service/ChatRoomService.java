package com.example.doktoribackend.room.service;

import com.example.doktoribackend.book.domain.Book;
import com.example.doktoribackend.book.service.BookService;
import com.example.doktoribackend.book.repository.BookRepository;
import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.config.WebSocketSessionRegistry;
import com.example.doktoribackend.exception.BusinessException;
import com.example.doktoribackend.summary.service.RoundSummaryService;
import com.example.doktoribackend.vote.service.VoteService;
import com.example.doktoribackend.quiz.service.QuizService;
import com.example.doktoribackend.room.domain.ChattingRoom;
import com.example.doktoribackend.room.domain.ChattingRoomMember;
import com.example.doktoribackend.room.domain.MemberStatus;
import com.example.doktoribackend.room.domain.Position;
import com.example.doktoribackend.room.domain.RoomRound;
import com.example.doktoribackend.room.domain.RoomStatus;
import com.example.doktoribackend.room.dto.ChatRoomCreateRequest;
import com.example.doktoribackend.room.dto.ChatRoomCreateResponse;
import com.example.doktoribackend.room.dto.ChatRoomJoinRequest;
import com.example.doktoribackend.room.dto.ChatRoomStartResponse;
import com.example.doktoribackend.room.dto.ChatStartMemberItem;
import com.example.doktoribackend.room.dto.NextRoundResponse;
import com.example.doktoribackend.room.dto.WaitingRoomResponse;
import com.example.doktoribackend.room.repository.ChattingRoomMemberRepository;
import com.example.doktoribackend.room.repository.ChattingRoomRepository;
import com.example.doktoribackend.room.repository.RoomRoundRepository;
import com.example.doktoribackend.user.domain.UserInfo;
import com.example.doktoribackend.user.repository.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private static final List<Integer> ALLOWED_CAPACITIES = List.of(2, 4, 6);
    private static final int MAX_ROUND = 3;
    private static final List<MemberStatus> ACTIVE_STATUSES =
            List.of(MemberStatus.WAITING, MemberStatus.JOINED, MemberStatus.DISCONNECTED);

    private final ChattingRoomRepository chattingRoomRepository;
    private final ChattingRoomMemberRepository chattingRoomMemberRepository;
    private final RoomRoundRepository roomRoundRepository;
    private final UserInfoRepository userInfoRepository;
    private final WebSocketSessionRegistry sessionRegistry;
    private final PlatformTransactionManager transactionManager;
    private final VoteService voteService;
    private final BookService bookService;
    private final QuizService quizService;
    private final ChatRoomEventPublisher chatRoomEventPublisher;
    private final BookRepository bookRepository;
    private final ChatRoomQueryService chatRoomQueryService;
    private final RoundSummaryService roundSummaryService;

    public ChatRoomCreateResponse createChatRoom(Long userId, ChatRoomCreateRequest request) {
        validateCapacity(request.capacity());
        Book book = bookService.resolveBook(request.isbn());

        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        return txTemplate.execute(status -> {
            validateNotAlreadyJoined(userId);

            Book managedBook = bookRepository.getReferenceById(book.getId());
            ChattingRoom room = ChattingRoom.create(request, managedBook);
            chattingRoomRepository.save(room);

            quizService.createQuiz(room, request.quiz());
            room.increaseMemberCount();

            UserInfo userInfo = userInfoRepository.findById(userId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
            ChattingRoomMember member = ChattingRoomMember.createHost(
                    room, userId, userInfo.getNickname(), userInfo.getProfileImagePath(), request);
            chattingRoomMemberRepository.save(member);

            return new ChatRoomCreateResponse(room.getId());
        });
    }

    @Transactional
    public void leaveChatRoom(Long roomId, Long userId) {
        ChattingRoom room = findRoom(roomId);

        validateRoomNotEnded(room);

        ChattingRoomMember member = chattingRoomMemberRepository.findByChattingRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_MEMBER_NOT_FOUND));

        if (!member.canLeave()) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_ALREADY_LEFT);
        }

        member.leave();
        room.decreaseMemberCount();

        if (room.getStatus() == RoomStatus.WAITING && member.isHost()) {
            room.cancel();
            leaveAllActiveMembers(roomId);
            chatRoomEventPublisher.broadcastCancelled(roomId);
        } else {
            WaitingRoomResponse response = chatRoomQueryService.buildWaitingRoomResponse(room);
            chatRoomEventPublisher.broadcastWaitingRoomUpdate(roomId, response);
        }
    }

    @Transactional
    public WaitingRoomResponse joinChatRoom(Long roomId, Long userId, ChatRoomJoinRequest request) {
        ChattingRoom room = findRoom(roomId);

        if (room.getStatus() != RoomStatus.WAITING) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_NOT_WAITING);
        }

        Optional<ChattingRoomMember> existingMember =
                chattingRoomMemberRepository.findByChattingRoomIdAndUserId(roomId, userId);

        if (existingMember.isPresent()) {
            ChattingRoomMember member = existingMember.get();

            if (member.getStatus() == MemberStatus.WAITING) {
                return chatRoomQueryService.buildWaitingRoomResponse(room);
            }

            if (member.getStatus() == MemberStatus.LEFT) {
                quizService.validateQuizAnswer(roomId, request.quizAnswer());
                validateRoomNotFull(room);
                validatePositionNotFull(roomId, room.getCapacity(), member.getPosition());
                member.rejoin();
                room.increaseMemberCount();
                WaitingRoomResponse response = chatRoomQueryService.buildWaitingRoomResponse(room);
                chatRoomEventPublisher.broadcastWaitingRoomUpdate(roomId, response);
                return response;
            }

            throw new BusinessException(ErrorCode.CHAT_ROOM_ALREADY_JOINED);
        }

        validateNotAlreadyJoined(userId);
        quizService.validateQuizAnswer(roomId, request.quizAnswer());
        validateRoomNotFull(room);
        validatePositionNotFull(roomId, room.getCapacity(), request.position());

        UserInfo userInfo = userInfoRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        ChattingRoomMember member = ChattingRoomMember.createParticipant(
                room, userId, userInfo.getNickname(), userInfo.getProfileImagePath(), request.position());
        chattingRoomMemberRepository.save(member);

        room.increaseMemberCount();

        WaitingRoomResponse response = chatRoomQueryService.buildWaitingRoomResponse(room);
        chatRoomEventPublisher.broadcastWaitingRoomUpdate(roomId, response);
        return response;
    }

    @Transactional
    public ChatRoomStartResponse startChatRoom(Long roomId, Long userId) {
        ChattingRoom room = findRoom(roomId);

        if (room.getStatus() != RoomStatus.WAITING) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_NOT_WAITING);
        }

        ChattingRoomMember requester = chattingRoomMemberRepository.findByChattingRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_MEMBER_NOT_FOUND));

        if (!requester.isHost()) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_NOT_HOST);
        }

        // TODO: 테스트 후 주석 해제
        // Position oppositePosition = requester.getPosition() == Position.AGREE ? Position.DISAGREE : Position.AGREE;
        // int oppositeCount = chattingRoomMemberRepository
        //         .countByChattingRoomIdAndPositionAndStatusIn(roomId, oppositePosition, ACTIVE_STATUSES);
        // if (oppositeCount < 1) {
        //     throw new BusinessException(ErrorCode.CHAT_ROOM_INSUFFICIENT_MEMBERS);
        // }

        room.startChatting();

        List<ChattingRoomMember> waitingMembers = chattingRoomMemberRepository
                .findByChattingRoomIdAndStatusIn(roomId, List.of(MemberStatus.WAITING));
        for (ChattingRoomMember member : waitingMembers) {
            member.join();
        }

        RoomRound firstRound = createRound(room, 1);

        List<ChattingRoomMember> activeMembers = findActiveMembers(roomId);

        List<ChatStartMemberItem> agreeMembers = activeMembers.stream()
                .filter(m -> m.getPosition() == Position.AGREE)
                .map(chatRoomQueryService::toStartMemberItem)
                .toList();
        List<ChatStartMemberItem> disagreeMembers = activeMembers.stream()
                .filter(m -> m.getPosition() == Position.DISAGREE)
                .map(chatRoomQueryService::toStartMemberItem)
                .toList();

        ChatRoomStartResponse response = new ChatRoomStartResponse(
                room.getTopic(), agreeMembers, disagreeMembers, 1, firstRound.getStartedAt());

        chatRoomEventPublisher.broadcastStarted(roomId, response);

        return response;
    }

    @Transactional
    public void nextRound(Long roomId, Long userId) {
        ChatRoomQueryService.ChattingRoomAndMember context = chatRoomQueryService.findChattingRoomAndMember(roomId, userId);
        ChattingRoom room = context.room();
        ChattingRoomMember requester = context.member();

        if (!requester.isHost()) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_NOT_HOST);
        }

        RoomRound currentRound = roomRoundRepository.findByChattingRoomIdAndEndedAtIsNull(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_ROUND_NOT_FOUND));

        if (currentRound.getRoundNumber() >= MAX_ROUND) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_MAX_ROUND_REACHED);
        }

        int completedRoundNumber = currentRound.getRoundNumber();
        Long completedRoundId = currentRound.getId();
        String topic = room.getTopic();

        currentRound.endRound();

        RoomRound newRound = createRound(room, completedRoundNumber + 1);

        NextRoundResponse response = new NextRoundResponse(newRound.getRoundNumber(), newRound.getStartedAt());
        chatRoomEventPublisher.broadcastNextRound(roomId, response);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                roundSummaryService.generateSummaryAsync(roomId, completedRoundId, topic, completedRoundNumber);
            }
        });
    }

    @Transactional
    public void endChatRoom(Long roomId, Long userId) {
        ChatRoomQueryService.ChattingRoomAndMember context = chatRoomQueryService.findChattingRoomAndMember(roomId, userId);
        ChattingRoomMember requester = context.member();

        if (!requester.isHost()) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_NOT_HOST);
        }

        RoomRound currentRound = roomRoundRepository.findByChattingRoomIdAndEndedAtIsNull(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_ROUND_NOT_FOUND));

        if (currentRound.getRoundNumber() < MAX_ROUND) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_NOT_LAST_ROUND);
        }

        endRoom(context.room(), currentRound);
    }

    @Transactional
    public void endExpiredChatRooms() {
        List<ChattingRoom> expiredRooms = chattingRoomRepository.findExpiredChattingRooms(LocalDateTime.now());
        for (ChattingRoom room : expiredRooms) {
            endRoom(room, null);
        }
    }

    private void endRoom(ChattingRoom room, RoomRound currentRound) {
        RoomRound lastRound = currentRound;
        if (lastRound != null) {
            lastRound.endRound();
        } else {
            lastRound = roomRoundRepository.findByChattingRoomIdAndEndedAtIsNull(room.getId())
                    .orElse(null);
            if (lastRound != null) {
                lastRound.endRound();
            }
        }

        int memberCount = room.getCurrentMemberCount();
        room.endChatting();
        leaveAllActiveMembers(room.getId());
        voteService.createVote(room, memberCount);
        sessionRegistry.removeAllForRoom(room.getId());
        chatRoomEventPublisher.broadcastEnded(room.getId());

        if (lastRound != null) {
            Long roomId = room.getId();
            Long roundId = lastRound.getId();
            String topic = room.getTopic();
            int roundNumber = lastRound.getRoundNumber();

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    roundSummaryService.generateSummaryAsync(roomId, roundId, topic, roundNumber);
                }
            });
        }
    }

    private ChattingRoom findRoom(Long roomId) {
        return chattingRoomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));
    }

    private List<ChattingRoomMember> findActiveMembers(Long roomId) {
        return chattingRoomMemberRepository.findByChattingRoomIdAndStatusIn(roomId, ACTIVE_STATUSES);
    }

    private RoomRound createRound(ChattingRoom room, int roundNumber) {
        RoomRound round = RoomRound.builder()
                .chattingRoom(room)
                .roundNumber(roundNumber)
                .build();
        room.getRounds().add(round);
        return round;
    }


    private void validateRoomNotFull(ChattingRoom room) {
        if (room.getCurrentMemberCount() >= room.getCapacity()) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_FULL);
        }
    }

    private void validatePositionNotFull(Long roomId, int capacity, Position position) {
        int maxPerPosition = capacity / 2;
        int positionCount = chattingRoomMemberRepository
                .countByChattingRoomIdAndPositionAndStatusIn(roomId, position, ACTIVE_STATUSES);
        if (positionCount >= maxPerPosition) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_POSITION_FULL);
        }
    }

    private void validateRoomNotEnded(ChattingRoom room) {
        if (room.getStatus() == RoomStatus.ENDED || room.getStatus() == RoomStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_ALREADY_ENDED);
        }
    }

    private void leaveAllActiveMembers(Long roomId) {
        List<ChattingRoomMember> activeMembers = chattingRoomMemberRepository
                .findByChattingRoomIdAndStatusIn(roomId, ACTIVE_STATUSES);

        for (ChattingRoomMember m : activeMembers) {
            m.leave();
        }
    }

    private void validateCapacity(Integer capacity) {
        if (!ALLOWED_CAPACITIES.contains(capacity)) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_INVALID_CAPACITY);
        }
    }

    private void validateNotAlreadyJoined(Long userId) {
        boolean alreadyJoined = chattingRoomMemberRepository.existsByUserIdAndStatusIn(
                userId, ACTIVE_STATUSES);

        if (alreadyJoined) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_ALREADY_JOINED);
        }
    }
}

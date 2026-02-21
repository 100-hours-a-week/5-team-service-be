package com.example.doktoribackend.room.service;

import com.example.doktoribackend.book.domain.Book;
import com.example.doktoribackend.book.repository.BookRepository;
import com.example.doktoribackend.common.client.KakaoBookClient;
import com.example.doktoribackend.common.client.KakaoBookResponse;
import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.common.s3.ImageUrlResolver;
import com.example.doktoribackend.exception.BusinessException;
import com.example.doktoribackend.quiz.domain.Quiz;
import com.example.doktoribackend.quiz.domain.QuizChoice;
import com.example.doktoribackend.room.domain.ChattingRoom;
import com.example.doktoribackend.room.domain.ChattingRoomMember;
import com.example.doktoribackend.room.domain.MemberStatus;
import com.example.doktoribackend.room.domain.Position;
import com.example.doktoribackend.room.domain.RoomRound;
import com.example.doktoribackend.room.domain.RoomStatus;
import com.example.doktoribackend.room.dto.ChatRoomCreateRequest;
import com.example.doktoribackend.room.dto.ChatRoomCreateResponse;
import com.example.doktoribackend.room.dto.ChatRoomJoinRequest;
import com.example.doktoribackend.room.dto.ChatRoomListItem;
import com.example.doktoribackend.room.dto.ChatRoomListResponse;
import com.example.doktoribackend.room.dto.ChatRoomStartResponse;
import com.example.doktoribackend.room.dto.NextRoundResponse;
import com.example.doktoribackend.room.dto.ChatStartMemberItem;
import com.example.doktoribackend.room.dto.PageInfo;
import com.example.doktoribackend.room.dto.WaitingRoomMemberItem;
import com.example.doktoribackend.room.dto.WaitingRoomResponse;
import com.example.doktoribackend.room.repository.ChattingRoomMemberRepository;
import com.example.doktoribackend.room.repository.ChattingRoomRepository;
import com.example.doktoribackend.room.repository.RoomRoundRepository;
import com.example.doktoribackend.user.domain.UserInfo;
import com.example.doktoribackend.user.repository.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
    private final BookRepository bookRepository;
    private final KakaoBookClient kakaoBookClient;
    private final UserInfoRepository userInfoRepository;
    private final WaitingRoomSseService waitingRoomSseService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ImageUrlResolver imageUrlResolver;

    @Transactional(readOnly = true)
    public ChatRoomListResponse getChatRooms(Long cursorId, int size) {
        List<ChattingRoom> rooms = chattingRoomRepository.findByStatusWithCursor(
                RoomStatus.WAITING, cursorId, PageRequest.of(0, size + 1));
        boolean hasNext = rooms.size() > size;
        List<ChattingRoom> content = hasNext ? rooms.subList(0, size) : rooms;

        List<ChatRoomListItem> items = content.stream()
                .map(ChatRoomListItem::from)
                .toList();

        Long nextCursorId = hasNext ? content.getLast().getId() : null;
        PageInfo pageInfo = new PageInfo(nextCursorId, hasNext, size);

        return new ChatRoomListResponse(items, pageInfo);
    }

    @Transactional
    public ChatRoomCreateResponse createChatRoom(Long userId, ChatRoomCreateRequest request) {
        validateCapacity(request.capacity());
        validateNotAlreadyJoined(userId);

        Book book = resolveBook(request.isbn());

        ChattingRoom room = ChattingRoom.create(request, book);
        chattingRoomRepository.save(room);

        createQuiz(room, request.quiz());

        room.increaseMemberCount();

        UserInfo userInfo = userInfoRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        ChattingRoomMember member = ChattingRoomMember.createHost(
                room, userId, userInfo.getNickname(), userInfo.getProfileImagePath(), request);
        chattingRoomMemberRepository.save(member);

        return new ChatRoomCreateResponse(room.getId());
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
            broadcastCancelledAfterCommit(roomId);
        } else {
            WaitingRoomResponse response = buildWaitingRoomResponse(room);
            broadcastAfterCommit(roomId, response);
        }
    }

    @Transactional
    public WaitingRoomResponse joinChatRoom(Long roomId, Long userId, ChatRoomJoinRequest request) {
        ChattingRoom room = findRoom(roomId);

        if (room.getStatus() != RoomStatus.WAITING) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_NOT_WAITING);
        }

        validateNotAlreadyJoined(userId);
        validateQuizAnswer(room, request.quizAnswer());
        validateRoomNotFull(room);
        validatePositionNotFull(roomId, room.getCapacity(), request.position());

        UserInfo userInfo = userInfoRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        ChattingRoomMember member = ChattingRoomMember.createParticipant(
                room, userId, userInfo.getNickname(), userInfo.getProfileImagePath(), request.position());
        chattingRoomMemberRepository.save(member);

        room.increaseMemberCount();

        WaitingRoomResponse response = buildWaitingRoomResponse(room);
        broadcastAfterCommit(roomId, response);
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

        Position oppositePosition = requester.getPosition() == Position.AGREE ? Position.DISAGREE : Position.AGREE;
        int oppositeCount = chattingRoomMemberRepository
                .countByChattingRoomIdAndPositionAndStatusIn(roomId, oppositePosition, ACTIVE_STATUSES);
        if (oppositeCount < 1) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_INSUFFICIENT_MEMBERS);
        }

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
                .map(m -> ChatStartMemberItem.from(m, imageUrlResolver))
                .toList();
        List<ChatStartMemberItem> disagreeMembers = activeMembers.stream()
                .filter(m -> m.getPosition() == Position.DISAGREE)
                .map(m -> ChatStartMemberItem.from(m, imageUrlResolver))
                .toList();

        ChatRoomStartResponse response = new ChatRoomStartResponse(
                room.getTopic(), agreeMembers, disagreeMembers, 1, firstRound.getStartedAt());

        broadcastStartedAfterCommit(roomId, response);

        return response;
    }

    @Transactional(readOnly = true)
    public WaitingRoomResponse getWaitingRoom(Long roomId, Long userId) {
        ChattingRoom room = findRoom(roomId);

        chattingRoomMemberRepository.findByChattingRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_MEMBER_NOT_FOUND));

        return buildWaitingRoomResponse(room);
    }

    @Transactional(readOnly = true)
    public ChatRoomStartResponse getChatRoomDetail(Long roomId, Long userId) {
        ChattingRoomAndMember context = findChattingRoomAndMember(roomId, userId);
        ChattingRoom room = context.room();

        List<ChattingRoomMember> activeMembers = findActiveMembers(roomId);

        List<ChatStartMemberItem> agreeMembers = activeMembers.stream()
                .filter(m -> m.getPosition() == Position.AGREE)
                .map(m -> ChatStartMemberItem.from(m, imageUrlResolver))
                .toList();
        List<ChatStartMemberItem> disagreeMembers = activeMembers.stream()
                .filter(m -> m.getPosition() == Position.DISAGREE)
                .map(m -> ChatStartMemberItem.from(m, imageUrlResolver))
                .toList();

        RoomRound activeRound = roomRoundRepository.findByChattingRoomIdAndEndedAtIsNull(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_ROUND_NOT_FOUND));

        return new ChatRoomStartResponse(
                room.getTopic(), agreeMembers, disagreeMembers, activeRound.getRoundNumber(), activeRound.getStartedAt());
    }

    @Transactional
    public void nextRound(Long roomId, Long userId) {
        ChattingRoomAndMember context = findChattingRoomAndMember(roomId, userId);
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

        currentRound.endRound();

        RoomRound newRound = createRound(room, currentRound.getRoundNumber() + 1);

        NextRoundResponse response = new NextRoundResponse(newRound.getRoundNumber(), newRound.getStartedAt());
        broadcastNextRoundAfterCommit(roomId, response);
    }

    @Transactional
    public void endChatRoom(Long roomId, Long userId) {
        ChattingRoomAndMember context = findChattingRoomAndMember(roomId, userId);
        ChattingRoomMember requester = context.member();

        if (!requester.isHost()) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_NOT_HOST);
        }

        RoomRound currentRound = roomRoundRepository.findByChattingRoomIdAndEndedAtIsNull(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_ROUND_NOT_FOUND));

        if (currentRound.getRoundNumber() < MAX_ROUND) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_NOT_LAST_ROUND);
        }

        endRoom(context.room());
    }

    @Transactional
    public void endExpiredChatRooms() {
        List<ChattingRoom> chattingRooms = chattingRoomRepository.findByStatus(RoomStatus.CHATTING);
        LocalDateTime now = LocalDateTime.now();

        for (ChattingRoom room : chattingRooms) {
            roomRoundRepository.findByChattingRoomIdAndRoundNumber(room.getId(), 1)
                    .filter(firstRound -> firstRound.getStartedAt().plusMinutes(room.getDuration()).isBefore(now))
                    .ifPresent(firstRound -> endRoom(room));
        }
    }

    private void endRoom(ChattingRoom room) {
        roomRoundRepository.findByChattingRoomIdAndEndedAtIsNull(room.getId())
                .ifPresent(RoomRound::endRound);

        room.endChatting();
        leaveAllActiveMembers(room.getId());
        broadcastEndedAfterCommit(room.getId());
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

    private record ChattingRoomAndMember(ChattingRoom room, ChattingRoomMember member) {}

    private ChattingRoomAndMember findChattingRoomAndMember(Long roomId, Long userId) {
        ChattingRoom room = findRoom(roomId);

        if (room.getStatus() != RoomStatus.CHATTING) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_NOT_CHATTING);
        }

        ChattingRoomMember member = chattingRoomMemberRepository.findByChattingRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_MEMBER_NOT_FOUND));

        return new ChattingRoomAndMember(room, member);
    }

    private void validateQuizAnswer(ChattingRoom room, Integer quizAnswer) {
        Quiz quiz = room.getQuiz();
        if (quiz == null) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_QUIZ_NOT_FOUND);
        }
        if (!quiz.isCorrect(quizAnswer)) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_QUIZ_WRONG_ANSWER);
        }
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

    WaitingRoomResponse buildWaitingRoomResponse(ChattingRoom room) {
        List<ChattingRoomMember> activeMembers = chattingRoomMemberRepository
                .findByChattingRoomIdAndStatusIn(room.getId(), ACTIVE_STATUSES);

        int agreeCount = (int) activeMembers.stream()
                .filter(m -> m.getPosition() == Position.AGREE).count();
        int disagreeCount = (int) activeMembers.stream()
                .filter(m -> m.getPosition() == Position.DISAGREE).count();
        int maxPerPosition = room.getCapacity() / 2;

        List<WaitingRoomMemberItem> members = activeMembers.stream()
                .map(m -> WaitingRoomMemberItem.from(m, imageUrlResolver))
                .toList();

        return new WaitingRoomResponse(room.getId(), agreeCount, disagreeCount, maxPerPosition, members);
    }

    private void broadcastAfterCommit(Long roomId, WaitingRoomResponse response) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                waitingRoomSseService.broadcast(roomId, response);
            }
        });
    }

    private void broadcastCancelledAfterCommit(Long roomId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                waitingRoomSseService.broadcastCancelledAndClose(roomId);
            }
        });
    }

    private void broadcastStartedAfterCommit(Long roomId, ChatRoomStartResponse response) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                waitingRoomSseService.broadcastStartedAndClose(roomId, response);
            }
        });
    }

    private void broadcastNextRoundAfterCommit(Long roomId, NextRoundResponse response) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                messagingTemplate.convertAndSend("/topic/chat-rooms/" + roomId, response);
            }
        });
    }

    private void broadcastEndedAfterCommit(Long roomId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                messagingTemplate.convertAndSend("/topic/chat-rooms/" + roomId, Map.of("type", "ROOM_ENDED"));
            }
        });
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
                userId, List.of(MemberStatus.WAITING, MemberStatus.JOINED));

        if (alreadyJoined) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_ALREADY_JOINED);
        }
    }

    private void createQuiz(ChattingRoom room, ChatRoomCreateRequest.QuizRequest quizRequest) {
        Quiz quiz = Quiz.create(room, quizRequest);

        for (ChatRoomCreateRequest.QuizChoiceRequest choiceRequest : quizRequest.choices()) {
            quiz.addChoice(QuizChoice.create(quiz, choiceRequest));
        }
    }

    private Book resolveBook(String isbn) {
        return bookRepository.findByIsbn(isbn)
                .orElseGet(() -> kakaoBookClient.searchByIsbn(isbn)
                        .map(doc -> bookRepository.save(toBookFromKakao(doc, isbn)))
                        .orElseThrow(() -> new BusinessException(ErrorCode.BOOK_NOT_FOUND)));
    }

    private Book toBookFromKakao(KakaoBookResponse.KakaoBookDocument doc, String isbn) {
        String authors = doc.authors() != null ? String.join(", ", doc.authors()) : null;
        LocalDate publishedAt = parsePublishedAt(doc.datetime());
        return Book.create(isbn, doc.title(), authors, doc.publisher(), doc.thumbnail(), publishedAt);
    }

    private LocalDate parsePublishedAt(String datetime) {
        if (datetime == null || datetime.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(datetime.substring(0, 10));
        } catch (Exception e) {
            return null;
        }
    }
}

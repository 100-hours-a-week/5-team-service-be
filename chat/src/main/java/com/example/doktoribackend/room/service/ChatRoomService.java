package com.example.doktoribackend.room.service;

import com.example.doktoribackend.book.domain.Book;
import com.example.doktoribackend.book.repository.BookRepository;
import com.example.doktoribackend.common.client.KakaoBookClient;
import com.example.doktoribackend.common.client.KakaoBookResponse;
import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.exception.BusinessException;
import com.example.doktoribackend.quiz.domain.Quiz;
import com.example.doktoribackend.quiz.domain.QuizChoice;
import com.example.doktoribackend.room.domain.ChattingRoom;
import com.example.doktoribackend.room.domain.ChattingRoomMember;
import com.example.doktoribackend.room.domain.MemberStatus;
import com.example.doktoribackend.room.domain.RoomStatus;
import com.example.doktoribackend.room.dto.ChatRoomCreateRequest;
import com.example.doktoribackend.room.dto.ChatRoomCreateResponse;
import com.example.doktoribackend.room.dto.ChatRoomListItem;
import com.example.doktoribackend.room.dto.ChatRoomListResponse;
import com.example.doktoribackend.room.dto.PageInfo;
import com.example.doktoribackend.room.repository.ChattingRoomMemberRepository;
import com.example.doktoribackend.room.repository.ChattingRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private static final List<Integer> ALLOWED_CAPACITIES = List.of(2, 4, 6);

    private final ChattingRoomRepository chattingRoomRepository;
    private final ChattingRoomMemberRepository chattingRoomMemberRepository;
    private final BookRepository bookRepository;
    private final KakaoBookClient kakaoBookClient;

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

        ChattingRoomMember member = ChattingRoomMember.createHost(room, userId, request);
        chattingRoomMemberRepository.save(member);

        return new ChatRoomCreateResponse(room.getId());
    }

    @Transactional
    public void leaveChatRoom(Long roomId, Long userId) {
        ChattingRoom room = chattingRoomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

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
        }
    }

    private void validateRoomNotEnded(ChattingRoom room) {
        if (room.getStatus() == RoomStatus.ENDED || room.getStatus() == RoomStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_ALREADY_ENDED);
        }
    }

    private void leaveAllActiveMembers(Long roomId) {
        List<ChattingRoomMember> activeMembers = chattingRoomMemberRepository
                .findByChattingRoomIdAndStatusIn(roomId,
                        List.of(MemberStatus.WAITING, MemberStatus.JOINED, MemberStatus.DISCONNECTED));

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

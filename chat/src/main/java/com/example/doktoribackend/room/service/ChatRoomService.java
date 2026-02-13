package com.example.doktoribackend.room.service;

import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.exception.BusinessException;
import com.example.doktoribackend.quiz.domain.Quiz;
import com.example.doktoribackend.quiz.domain.QuizChoice;
import com.example.doktoribackend.room.domain.ChattingRoom;
import com.example.doktoribackend.room.domain.ChattingRoomMember;
import com.example.doktoribackend.room.domain.MemberStatus;
import com.example.doktoribackend.room.dto.ChatRoomCreateRequest;
import com.example.doktoribackend.room.dto.ChatRoomCreateResponse;
import com.example.doktoribackend.room.repository.ChattingRoomMemberRepository;
import com.example.doktoribackend.room.repository.ChattingRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private static final List<Integer> ALLOWED_CAPACITIES = List.of(2, 4, 6);

    private final ChattingRoomRepository chattingRoomRepository;
    private final ChattingRoomMemberRepository chattingRoomMemberRepository;

    @Transactional
    public ChatRoomCreateResponse createChatRoom(Long userId, ChatRoomCreateRequest request) {
        validateCapacity(request.capacity());
        validateNotAlreadyJoined(userId);

        ChattingRoom room = ChattingRoom.create(request);

        createQuiz(room, request.quiz());
        chattingRoomRepository.save(room);

        room.increaseMemberCount();

        ChattingRoomMember member = ChattingRoomMember.createHost(room, userId, request);
        chattingRoomMemberRepository.save(member);

        return new ChatRoomCreateResponse(room.getId());
    }

    private void validateCapacity(Integer capacity) {
        if (!ALLOWED_CAPACITIES.contains(capacity)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
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
            QuizChoice.create(quiz, choiceRequest);
        }
    }
}

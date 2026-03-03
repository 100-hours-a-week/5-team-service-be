package com.example.doktoribackend.quiz.service;

import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.exception.BusinessException;
import com.example.doktoribackend.quiz.domain.Quiz;
import com.example.doktoribackend.quiz.domain.QuizChoice;
import com.example.doktoribackend.quiz.dto.QuizResponse;
import com.example.doktoribackend.quiz.repository.QuizRepository;
import com.example.doktoribackend.room.domain.ChattingRoom;
import com.example.doktoribackend.room.domain.MemberStatus;
import com.example.doktoribackend.room.domain.Position;
import com.example.doktoribackend.room.domain.RoomStatus;
import com.example.doktoribackend.room.dto.ChatRoomCreateRequest;
import com.example.doktoribackend.room.repository.ChattingRoomMemberRepository;
import com.example.doktoribackend.room.repository.ChattingRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuizService {

    private static final List<MemberStatus> ACTIVE_STATUSES =
            List.of(MemberStatus.WAITING, MemberStatus.JOINED, MemberStatus.DISCONNECTED);

    private final QuizRepository quizRepository;
    private final ChattingRoomRepository chattingRoomRepository;
    private final ChattingRoomMemberRepository chattingRoomMemberRepository;

    public void createQuiz(ChattingRoom room, ChatRoomCreateRequest.QuizRequest quizRequest) {
        Quiz quiz = Quiz.create(room, quizRequest);

        for (ChatRoomCreateRequest.QuizChoiceRequest choiceRequest : quizRequest.choices()) {
            quiz.addChoice(QuizChoice.create(quiz, choiceRequest));
        }

        quizRepository.save(quiz);
    }

    public void validateQuizAnswer(Long roomId, Integer quizAnswer) {
        Quiz quiz = quizRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_QUIZ_NOT_FOUND));
        if (!quiz.isCorrect(quizAnswer)) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_QUIZ_WRONG_ANSWER);
        }
    }

    @Transactional(readOnly = true)
    public QuizResponse getQuiz(Long roomId) {
        ChattingRoom room = chattingRoomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        if (room.getStatus() != RoomStatus.WAITING) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_NOT_WAITING);
        }

        Quiz quiz = quizRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_QUIZ_NOT_FOUND));

        int agreeCount = chattingRoomMemberRepository
                .countByChattingRoomIdAndPositionAndStatusIn(roomId, Position.AGREE, ACTIVE_STATUSES);
        int disagreeCount = chattingRoomMemberRepository
                .countByChattingRoomIdAndPositionAndStatusIn(roomId, Position.DISAGREE, ACTIVE_STATUSES);
        int maxPerPosition = room.getCapacity() / 2;

        return QuizResponse.from(quiz, agreeCount, disagreeCount, maxPerPosition);
    }
}

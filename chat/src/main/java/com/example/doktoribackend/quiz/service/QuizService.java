package com.example.doktoribackend.quiz.service;

import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.exception.BusinessException;
import com.example.doktoribackend.quiz.domain.Quiz;
import com.example.doktoribackend.quiz.domain.QuizChoice;
import com.example.doktoribackend.quiz.dto.QuizResponse;
import com.example.doktoribackend.quiz.repository.QuizRepository;
import com.example.doktoribackend.room.domain.ChattingRoom;
import com.example.doktoribackend.room.domain.RoomStatus;
import com.example.doktoribackend.room.dto.ChatRoomCreateRequest;
import com.example.doktoribackend.room.repository.ChattingRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizRepository quizRepository;
    private final ChattingRoomRepository chattingRoomRepository;

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

        return QuizResponse.from(quiz);
    }
}

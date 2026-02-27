package com.example.doktoribackend.quiz.service;

import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.exception.BusinessException;
import com.example.doktoribackend.quiz.client.AiQuizClient;
import com.example.doktoribackend.quiz.client.AiQuizGenerateRequest;
import com.example.doktoribackend.quiz.client.AiQuizGenerateResponse;
import com.example.doktoribackend.quiz.domain.QuizGenerationLog;
import com.example.doktoribackend.quiz.dto.AiQuizSuggestRequest;
import com.example.doktoribackend.quiz.dto.AiQuizSuggestResponse;
import com.example.doktoribackend.quiz.repository.QuizGenerationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class AiQuizGenerationService {

    private static final int DAILY_LIMIT = 3;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final QuizGenerationLogRepository quizGenerationLogRepository;
    private final AiQuizClient aiQuizClient;
    private final PlatformTransactionManager transactionManager;

    public AiQuizSuggestResponse suggest(Long userId, AiQuizSuggestRequest request) {
        checkDailyLimit(userId);

        AiQuizGenerateResponse aiResponse = aiQuizClient.generate(
                new AiQuizGenerateRequest(request.author(), request.title(), request.roomId()));

        saveLog(userId);

        return AiQuizSuggestResponse.from(aiResponse);
    }

    private void checkDailyLimit(Long userId) {
        LocalDate today = LocalDate.now(KST);
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        TransactionTemplate readTx = new TransactionTemplate(transactionManager);
        readTx.setReadOnly(true);

        Integer count = readTx.execute(status ->
                quizGenerationLogRepository.countTodayByUserId(userId, start, end));

        if (count != null && count >= DAILY_LIMIT) {
            throw new BusinessException(ErrorCode.AI_QUIZ_GENERATION_LIMIT_EXCEEDED);
        }
    }

    private void saveLog(Long userId) {
        TransactionTemplate writeTx = new TransactionTemplate(transactionManager);
        writeTx.executeWithoutResult(status ->
                quizGenerationLogRepository.save(QuizGenerationLog.of(userId)));
    }
}

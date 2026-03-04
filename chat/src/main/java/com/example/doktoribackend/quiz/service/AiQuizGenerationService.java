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
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiQuizGenerationService {

    private static final int DAILY_LIMIT = 3;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final QuizGenerationLogRepository quizGenerationLogRepository;
    private final AiQuizClient aiQuizClient;
    private final PlatformTransactionManager transactionManager;

    public AiQuizSuggestResponse suggest(Long userId, AiQuizSuggestRequest request) {
        Long logId = checkAndReserveSlot(userId);

        try {
            AiQuizGenerateResponse aiResponse = aiQuizClient.generate(
                    new AiQuizGenerateRequest(request.author(), request.title()));
            return AiQuizSuggestResponse.from(aiResponse);
        } catch (Exception e) {
            releaseSlot(logId);
            throw e;
        }
    }

    private Long checkAndReserveSlot(Long userId) {
        TransactionTemplate writeTx = new TransactionTemplate(transactionManager);
        return writeTx.execute(status -> {
            LocalDate today = LocalDate.now(KST);
            LocalDateTime start = today.atStartOfDay();
            LocalDateTime end = today.plusDays(1).atStartOfDay();

            List<QuizGenerationLog> todayLogs =
                    quizGenerationLogRepository.findTodayByUserIdWithLock(userId, start, end);

            if (todayLogs.size() >= DAILY_LIMIT) {
                throw new BusinessException(ErrorCode.AI_QUIZ_GENERATION_LIMIT_EXCEEDED);
            }

            return quizGenerationLogRepository.save(QuizGenerationLog.of(userId)).getId();
        });
    }

    private void releaseSlot(Long logId) {
        TransactionTemplate writeTx = new TransactionTemplate(transactionManager);
        writeTx.executeWithoutResult(status -> quizGenerationLogRepository.deleteById(logId));
    }
}

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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class AiQuizGenerationServiceTest {

    @Mock
    private QuizGenerationLogRepository quizGenerationLogRepository;

    @Mock
    private AiQuizClient aiQuizClient;

    @Mock
    private PlatformTransactionManager transactionManager;

    @InjectMocks
    private AiQuizGenerationService aiQuizGenerationService;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        TransactionSynchronizationManager.initSynchronization();
        lenient().when(transactionManager.getTransaction(any()))
                .thenReturn(mock(TransactionStatus.class));
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    private AiQuizSuggestRequest validRequest() {
        return new AiQuizSuggestRequest("손원평", "아몬드");
    }

    private AiQuizGenerateResponse aiResponse() {
        List<AiQuizGenerateResponse.ChoiceItem> choices = List.of(
                new AiQuizGenerateResponse.ChoiceItem(1, "윤재"),
                new AiQuizGenerateResponse.ChoiceItem(2, "곤이"),
                new AiQuizGenerateResponse.ChoiceItem(3, "선아"),
                new AiQuizGenerateResponse.ChoiceItem(4, "데니스")
        );
        AiQuizGenerateResponse.Quiz quiz = new AiQuizGenerateResponse.Quiz("주인공의 이름은?", 1);
        return new AiQuizGenerateResponse(quiz, choices);
    }

    @Nested
    @DisplayName("성공 케이스")
    class Success {

        @Test
        @DisplayName("일일 횟수 미만이면 AI 호출 후 로그를 저장하고 응답을 반환한다")
        void suggest_underLimit_returnsResponse() {
            given(quizGenerationLogRepository.countTodayByUserId(
                    any(Long.class), any(LocalDateTime.class), any(LocalDateTime.class)))
                    .willReturn(0);
            given(aiQuizClient.generate(any(AiQuizGenerateRequest.class)))
                    .willReturn(aiResponse());
            given(quizGenerationLogRepository.save(any(QuizGenerationLog.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            AiQuizSuggestResponse result = aiQuizGenerationService.suggest(USER_ID, validRequest());

            assertThat(result.question()).isEqualTo("주인공의 이름은?");
            assertThat(result.correctChoiceNumber()).isEqualTo(1);
            assertThat(result.choices()).hasSize(4);
            assertThat(result.choices().getFirst().choiceNumber()).isEqualTo(1);
            assertThat(result.choices().getFirst().choiceText()).isEqualTo("윤재");

            then(aiQuizClient).should().generate(any(AiQuizGenerateRequest.class));
            then(quizGenerationLogRepository).should().save(any(QuizGenerationLog.class));
        }

        @Test
        @DisplayName("일일 2회 사용 후 추가 요청하면 성공한다 (한도=3)")
        void suggest_twoLogsExist_succeeds() {
            given(quizGenerationLogRepository.countTodayByUserId(
                    any(Long.class), any(LocalDateTime.class), any(LocalDateTime.class)))
                    .willReturn(2);
            given(aiQuizClient.generate(any(AiQuizGenerateRequest.class)))
                    .willReturn(aiResponse());
            given(quizGenerationLogRepository.save(any(QuizGenerationLog.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            AiQuizSuggestResponse result = aiQuizGenerationService.suggest(USER_ID, validRequest());

            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("일일 한도 초과")
    class LimitExceeded {

        @Test
        @DisplayName("일일 3회를 모두 소진하면 AI_QUIZ_GENERATION_LIMIT_EXCEEDED를 던진다")
        void suggest_limitReached_throwsLimitExceeded() {
            given(quizGenerationLogRepository.countTodayByUserId(
                    any(Long.class), any(LocalDateTime.class), any(LocalDateTime.class)))
                    .willReturn(3);

            assertThatThrownBy(() -> aiQuizGenerationService.suggest(USER_ID, validRequest()))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.AI_QUIZ_GENERATION_LIMIT_EXCEEDED));

            then(aiQuizClient).should(never()).generate(any());
            then(quizGenerationLogRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("일일 횟수를 초과하면 AI를 호출하지 않는다")
        void suggest_overLimit_doesNotCallAi() {
            given(quizGenerationLogRepository.countTodayByUserId(
                    any(Long.class), any(LocalDateTime.class), any(LocalDateTime.class)))
                    .willReturn(5);

            assertThatThrownBy(() -> aiQuizGenerationService.suggest(USER_ID, validRequest()))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.AI_QUIZ_GENERATION_LIMIT_EXCEEDED));

            then(aiQuizClient).should(never()).generate(any());
        }
    }

    @Nested
    @DisplayName("AI 호출 실패")
    class AiFailure {

        @Test
        @DisplayName("AI 호출이 실패하면 로그를 저장하지 않고 예외를 전파한다")
        void suggest_aiFails_doesNotSaveLog() {
            given(quizGenerationLogRepository.countTodayByUserId(
                    any(Long.class), any(LocalDateTime.class), any(LocalDateTime.class)))
                    .willReturn(0);
            given(aiQuizClient.generate(any(AiQuizGenerateRequest.class)))
                    .willThrow(new BusinessException(ErrorCode.AI_QUIZ_GENERATION_FAILED));

            assertThatThrownBy(() -> aiQuizGenerationService.suggest(USER_ID, validRequest()))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.AI_QUIZ_GENERATION_FAILED));

            then(quizGenerationLogRepository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("요청 데이터 전달 검증")
    class RequestPropagation {

        @Test
        @DisplayName("요청의 author, title이 AiQuizGenerateRequest에 그대로 전달된다")
        void suggest_propagatesRequestFields() {
            AiQuizSuggestRequest request = new AiQuizSuggestRequest("작가명", "책제목");

            given(quizGenerationLogRepository.countTodayByUserId(
                    any(Long.class), any(LocalDateTime.class), any(LocalDateTime.class)))
                    .willReturn(0);
            given(aiQuizClient.generate(any(AiQuizGenerateRequest.class)))
                    .willReturn(aiResponse());
            given(quizGenerationLogRepository.save(any(QuizGenerationLog.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            aiQuizGenerationService.suggest(USER_ID, request);

            then(aiQuizClient).should().generate(
                    new AiQuizGenerateRequest("작가명", "책제목"));
        }
    }
}

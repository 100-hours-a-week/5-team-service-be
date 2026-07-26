package com.example.doktoribackend.quiz.client;

import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

/**
 * 재시도·서킷 브레이커는 Resilience4j 설정이 담당하므로, 이 클라이언트는 원시 예외를
 * 그대로 전파해야 recordExceptions 가 실패로 집계할 수 있다. BusinessException 으로의
 * 변환은 재시도가 소진된 뒤 fallback 에서만 일어난다.
 */
@ExtendWith(MockitoExtension.class)
class AiQuizClientTest {

    @Mock
    private RestClient aiRestClient;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    @InjectMocks
    private AiQuizClient aiQuizClient;

    private AiQuizGenerateRequest request;

    @BeforeEach
    void setUp() {
        request = new AiQuizGenerateRequest("손원평", "아몬드");
    }

    /** fallback 테스트는 HTTP 호출을 하지 않으므로 이 스텁이 필요한 곳에서만 세운다. */
    private void stubRestClientChain() {
        doReturn(requestBodyUriSpec).when(aiRestClient).post();
        doReturn(requestBodySpec).when(requestBodyUriSpec).uri(anyString());
        doReturn(requestBodySpec).when(requestBodySpec).contentType(any(MediaType.class));
        doReturn(requestBodySpec).when(requestBodySpec).body((Object) any());
        doReturn(responseSpec).when(requestBodySpec).retrieve();
    }

    private AiQuizGenerateResponse validResponse() {
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
        @DisplayName("AI 서버가 유효한 응답을 반환하면 AiQuizGenerateResponse를 반환한다")
        void generate_success() {
            stubRestClientChain();
            doReturn(validResponse()).when(responseSpec).body(AiQuizGenerateResponse.class);

            AiQuizGenerateResponse result = aiQuizClient.generate(request);

            assertThat(result.quiz().question()).isEqualTo("주인공의 이름은?");
            assertThat(result.quiz().correctChoiceNumber()).isEqualTo(1);
            assertThat(result.quizChoices()).hasSize(4);
            assertThat(result.quizChoices().get(0).choiceNumber()).isEqualTo(1);
            assertThat(result.quizChoices().get(0).choiceText()).isEqualTo("윤재");
        }
    }

    @Nested
    @DisplayName("실패 케이스")
    class Failure {

        @Test
        @DisplayName("5xx 예외는 서킷 집계를 위해 원시 예외 그대로 전파한다")
        void generate_serverError_propagatesRawException() {
            stubRestClientChain();
            doThrow(HttpServerErrorException.create(INTERNAL_SERVER_ERROR, "err", null, null, null))
                    .when(responseSpec).body(AiQuizGenerateResponse.class);

            assertThatThrownBy(() -> aiQuizClient.generate(request))
                    .isInstanceOf(HttpServerErrorException.class);
        }

        @Test
        @DisplayName("AI 서버 응답 body가 null이면 BusinessException(AI_QUIZ_GENERATION_FAILED)을 던진다")
        void generate_nullBody_throwsBusinessException() {
            stubRestClientChain();
            doReturn(null).when(responseSpec).body(AiQuizGenerateResponse.class);

            assertThatThrownBy(() -> aiQuizClient.generate(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.AI_QUIZ_GENERATION_FAILED));
        }
    }

    @Nested
    @DisplayName("fallback")
    class Fallback {

        @Test
        @DisplayName("재시도 소진 후 fallback은 BusinessException(AI_QUIZ_GENERATION_FAILED)으로 변환한다")
        void generateFallback_convertsToBusinessException() {
            assertThatThrownBy(() -> aiQuizClient.generateFallback(
                    request, HttpServerErrorException.create(INTERNAL_SERVER_ERROR, "err", null, null, null)))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.AI_QUIZ_GENERATION_FAILED));
        }
    }
}

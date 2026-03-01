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
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

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

        doReturn(requestBodyUriSpec).when(aiRestClient).post();
        doReturn(requestBodySpec).when(requestBodyUriSpec).uri(anyString());
        doReturn(requestBodySpec).when(requestBodySpec).contentType(any(MediaType.class));
        doReturn(requestBodySpec).when(requestBodySpec).body((Object) any());
        doReturn(responseSpec).when(requestBodySpec).retrieve();
        doReturn(responseSpec).when(responseSpec).onStatus(any(), any());
    }

    private AiQuizGenerateResponse validResponse() {
        List<AiQuizGenerateResponse.ChoiceItem> choices = List.of(
                new AiQuizGenerateResponse.ChoiceItem(1, "윤재"),
                new AiQuizGenerateResponse.ChoiceItem(2, "곤이"),
                new AiQuizGenerateResponse.ChoiceItem(3, "선아"),
                new AiQuizGenerateResponse.ChoiceItem(4, "데니스")
        );
        return new AiQuizGenerateResponse("주인공의 이름은?", 1, 0L, choices);
    }

    @Nested
    @DisplayName("성공 케이스")
    class Success {

        @Test
        @DisplayName("AI 서버가 유효한 응답을 반환하면 AiQuizGenerateResponse를 반환한다")
        void generate_success() {
            doReturn(validResponse()).when(responseSpec).body(AiQuizGenerateResponse.class);

            AiQuizGenerateResponse result = aiQuizClient.generate(request);

            assertThat(result.question()).isEqualTo("주인공의 이름은?");
            assertThat(result.correctChoiceNumber()).isEqualTo(1);
            assertThat(result.choices()).hasSize(4);
            assertThat(result.choices().get(0).choiceNumber()).isEqualTo(1);
            assertThat(result.choices().get(0).choiceText()).isEqualTo("윤재");
        }
    }

    @Nested
    @DisplayName("실패 케이스")
    class Failure {

        @Test
        @DisplayName("AI 서버 오류 시 BusinessException(AI_QUIZ_GENERATION_FAILED)을 던진다")
        void generate_aiError_throwsBusinessException() {
            doThrow(new BusinessException(ErrorCode.AI_QUIZ_GENERATION_FAILED))
                    .when(responseSpec).body(AiQuizGenerateResponse.class);

            assertThatThrownBy(() -> aiQuizClient.generate(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.AI_QUIZ_GENERATION_FAILED));
        }

        @Test
        @DisplayName("AI 서버 응답 body가 null이면 BusinessException(AI_QUIZ_GENERATION_FAILED)을 던진다")
        void generate_nullBody_throwsBusinessException() {
            doReturn(null).when(responseSpec).body(AiQuizGenerateResponse.class);

            assertThatThrownBy(() -> aiQuizClient.generate(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.AI_QUIZ_GENERATION_FAILED));
        }

        @Test
        @DisplayName("네트워크 예외 발생 시 BusinessException(AI_QUIZ_GENERATION_FAILED)을 던진다")
        void generate_networkException_throwsBusinessException() {
            doThrow(new RuntimeException("Connection refused"))
                    .when(responseSpec).body(AiQuizGenerateResponse.class);

            assertThatThrownBy(() -> aiQuizClient.generate(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.AI_QUIZ_GENERATION_FAILED));
        }
    }
}

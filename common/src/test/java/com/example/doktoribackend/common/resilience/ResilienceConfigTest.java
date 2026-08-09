package com.example.doktoribackend.common.resilience;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.annotation.Retry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

/**
 * resilience4j-defaults.yml 의 설정이 의도대로 동작하는지 검증한다.
 * 임계치·recordExceptions·ignoreExceptions 같은 값은 설정 파일에만 존재하므로
 * 코드가 아니라 설정을 대상으로 테스트한다.
 */
@SpringBootTest(
        classes = ResilienceConfigTest.TestApp.class,
        properties = {
                "spring.config.import=classpath:resilience4j-defaults.yml",
                "spring.main.web-application-type=none"
        }
)
class ResilienceConfigTest {

    @Autowired
    private FlakyBookClient flakyBookClient;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    private CircuitBreaker kakaoBookBreaker;

    @BeforeEach
    void setUp() {
        kakaoBookBreaker = circuitBreakerRegistry.circuitBreaker("kakaoBook");
        kakaoBookBreaker.reset();
        flakyBookClient.reset();
    }

    @Test
    @DisplayName("Retry: 5xx는 재시도 대상이므로 maxAttempts(3)만큼 호출된다")
    void retry_serverError_retriesUpToMaxAttempts() {
        flakyBookClient.failWith(HttpServerErrorException.create(INTERNAL_SERVER_ERROR, "err", null, null, null));

        assertThatThrownBy(() -> flakyBookClient.call())
                .isInstanceOf(HttpServerErrorException.class);

        assertThat(flakyBookClient.invocations()).isEqualTo(3);
    }

    @Test
    @DisplayName("Retry: 4xx는 재시도해도 소용없으므로 1회만 호출된다")
    void retry_clientError_doesNotRetry() {
        flakyBookClient.failWith(HttpClientErrorException.create(BAD_REQUEST, "bad", null, null, null));

        assertThatThrownBy(() -> flakyBookClient.call())
                .isInstanceOf(HttpClientErrorException.class);

        assertThat(flakyBookClient.invocations()).isEqualTo(1);
    }

    @Test
    @DisplayName("CircuitBreaker: 4xx는 집계 대상이 아니므로 반복해도 서킷이 열리지 않는다")
    void circuitBreaker_clientError_doesNotOpenCircuit() {
        flakyBookClient.failWith(HttpClientErrorException.create(BAD_REQUEST, "bad", null, null, null));

        for (int i = 0; i < 30; i++) {
            callIgnoringException();
        }

        assertThat(kakaoBookBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    @DisplayName("CircuitBreaker: 5xx가 임계치를 넘으면 서킷이 Open으로 전환된다")
    void circuitBreaker_serverError_opensCircuit() {
        flakyBookClient.failWith(HttpServerErrorException.create(INTERNAL_SERVER_ERROR, "err", null, null, null));

        for (int i = 0; i < 10; i++) {
            callIgnoringException();
        }

        assertThat(kakaoBookBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    @DisplayName("CircuitBreaker: Open 상태에서는 대상 메서드를 호출하지 않고 즉시 차단한다")
    void circuitBreaker_open_blocksCallWithoutInvokingTarget() {
        flakyBookClient.failWith(HttpServerErrorException.create(INTERNAL_SERVER_ERROR, "err", null, null, null));
        for (int i = 0; i < 10; i++) {
            callIgnoringException();
        }
        assertThat(kakaoBookBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        int invocationsBeforeBlockedCall = flakyBookClient.invocations();

        assertThatThrownBy(() -> flakyBookClient.call())
                .isInstanceOf(CallNotPermittedException.class);

        assertThat(flakyBookClient.invocations()).isEqualTo(invocationsBeforeBlockedCall);
    }

    @Test
    @DisplayName("Retry: 서킷이 Open이면 CallNotPermittedException을 재시도하지 않는다 (지연 증폭 방지)")
    void retry_doesNotRetryCallNotPermittedException() {
        flakyBookClient.failWith(HttpServerErrorException.create(INTERNAL_SERVER_ERROR, "err", null, null, null));
        for (int i = 0; i < 10; i++) {
            callIgnoringException();
        }
        assertThat(kakaoBookBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        long startedAt = System.nanoTime();
        assertThatThrownBy(() -> flakyBookClient.call())
                .isInstanceOf(CallNotPermittedException.class);
        long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000;

        // 재시도가 걸렸다면 지수 백오프(200ms, 400ms)로 최소 600ms 는 소요된다.
        assertThat(elapsedMillis).isLessThan(300);
    }

    private void callIgnoringException() {
        try {
            flakyBookClient.call();
        } catch (Exception ignored) {
            // 서킷 상태만 관찰하므로 예외는 무시한다.
        }
    }

    @SpringBootApplication(exclude = {
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class
    })
    static class TestApp {
        @Bean
        FlakyBookClient flakyBookClient() {
            return new FlakyBookClient();
        }
    }

    /**
     * kakaoBook 인스턴스 설정을 그대로 적용받는 테스트용 대역.
     * 실제 HTTP 호출 없이 설정값의 동작만 확인한다.
     */
    static class FlakyBookClient {

        private int invocations;
        private RuntimeException failure;

        @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name = "kakaoBook")
        @Retry(name = "kakaoBook")
        public String call() {
            invocations++;
            if (failure != null) {
                throw failure;
            }
            return "ok";
        }

        void failWith(RuntimeException failure) {
            this.failure = failure;
        }

        void reset() {
            this.invocations = 0;
            this.failure = null;
        }

        int invocations() {
            return invocations;
        }
    }
}

package com.example.doktoribackend.zoom.service;

import com.example.doktoribackend.config.ZoomConfig;
import com.example.doktoribackend.zoom.exception.ZoomAuthenticationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ZoomAccessTokenProviderTest {

    private static final String TOKEN_URL = "https://zoom.us/oauth/token";

    private MockRestServiceServer server;
    private MutableClock clock;
    private ZoomAccessTokenProvider provider;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        clock = new MutableClock(Instant.parse("2026-07-25T00:00:00Z"));
        ZoomConfig config = new ZoomConfig("test-account", "test-client", "test-secret",
                "https://api.zoom.us/v2");
        provider = new ZoomAccessTokenProvider(builder.build(), config, clock);
    }

    @Test
    @DisplayName("getAccessToken: 토큰을 발급받고 Basic 인증 헤더를 보낸다")
    void getAccessToken_success_sendsBasicAuth() {
        String expectedBasic = "Basic " + Base64.getEncoder()
                .encodeToString("test-client:test-secret".getBytes());

        server.expect(once(), requestTo(TOKEN_URL))
                .andExpect(header("Authorization", expectedBasic))
                .andRespond(withSuccess("""
                        {"access_token": "token-1", "expires_in": 3600}
                        """, MediaType.APPLICATION_JSON));

        assertThat(provider.getAccessToken()).isEqualTo("token-1");
        server.verify();
    }

    @Test
    @DisplayName("getAccessToken: 유효한 토큰이 캐시에 있으면 재발급하지 않는다")
    void getAccessToken_cached_doesNotReissue() {
        server.expect(once(), requestTo(TOKEN_URL))
                .andRespond(withSuccess("""
                        {"access_token": "token-1", "expires_in": 3600}
                        """, MediaType.APPLICATION_JSON));

        List<String> tokens = List.of(
                provider.getAccessToken(),
                provider.getAccessToken(),
                provider.getAccessToken()
        );

        assertThat(tokens).containsExactly("token-1", "token-1", "token-1");
        // 기대 횟수(1회)를 초과하면 MockRestServiceServer 가 실패하므로 verify 통과가 캐시 동작의 증거다.
        server.verify();
    }

    @Test
    @DisplayName("getAccessToken: 만료 임박(잔여 5분 미만)이면 새 토큰을 발급받는다")
    void getAccessToken_nearExpiry_reissues() {
        server.expect(once(), requestTo(TOKEN_URL))
                .andRespond(withSuccess("""
                        {"access_token": "token-1", "expires_in": 3600}
                        """, MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo(TOKEN_URL))
                .andRespond(withSuccess("""
                        {"access_token": "token-2", "expires_in": 3600}
                        """, MediaType.APPLICATION_JSON));

        assertThat(provider.getAccessToken()).isEqualTo("token-1");

        clock.advance(Duration.ofSeconds(3600 - 60));

        assertThat(provider.getAccessToken()).isEqualTo("token-2");
        server.verify();
    }

    @Test
    @DisplayName("getAccessToken: 발급 실패는 ZoomAuthenticationException으로 변환한다")
    void getAccessToken_failure_throwsAuthenticationException() {
        server.expect(once(), requestTo(TOKEN_URL))
                .andRespond(withServerError());

        assertThatThrownBy(() -> provider.getAccessToken())
                .isInstanceOf(ZoomAuthenticationException.class);
    }

    private static final class MutableClock extends Clock {

        private Instant now;

        private MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            this.now = this.now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}

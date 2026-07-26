package com.example.doktoribackend.zoom.service;

import com.example.doktoribackend.config.ZoomConfig;
import com.example.doktoribackend.zoom.exception.ZoomAuthenticationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;

/**
 * Zoom Server-to-Server OAuth 액세스 토큰 발급 및 캐싱.
 *
 * <p>기존 구현은 미팅 생성 때마다 토큰을 새로 발급받아 외부 호출이 두 배였다.
 * 만료 5분 전까지 재사용한다.
 *
 * <p>토큰 발급과 미팅 생성은 서로 다른 실패 모드를 가지므로 서킷을 분리한다.
 * 미팅 생성 실패로 토큰 발급까지 차단되면 복구가 늦어진다.
 */
@Slf4j
@Component
public class ZoomAccessTokenProvider {

    private static final String TOKEN_URL = "https://zoom.us/oauth/token";
    private static final Duration EXPIRY_MARGIN = Duration.ofMinutes(5);

    private final RestClient restClient;
    private final ZoomConfig zoomConfig;
    private final Clock clock;

    private volatile String cachedToken;
    private volatile Instant expiresAt;

    public ZoomAccessTokenProvider(RestClient zoomRestClient, ZoomConfig zoomConfig, Clock clock) {
        this.restClient = zoomRestClient;
        this.zoomConfig = zoomConfig;
        this.clock = clock;
    }

    public synchronized String getAccessToken() {
        if (isCacheValid()) {
            return cachedToken;
        }
        issueToken();
        return cachedToken;
    }

    private boolean isCacheValid() {
        return cachedToken != null
                && expiresAt != null
                && clock.instant().isBefore(expiresAt.minus(EXPIRY_MARGIN));
    }

    private void issueToken() {
        String auth = zoomConfig.getClientId() + ":" + zoomConfig.getClientSecret();
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "account_credentials");
        body.add("account_id", zoomConfig.getAccountId());

        try {
            Map<String, Object> responseBody = restClient.post()
                    .uri(TOKEN_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .header("Authorization", "Basic " + encodedAuth)
                    .body(body)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            Objects.requireNonNull(responseBody, "Zoom 토큰 응답이 비어 있습니다.");

            this.cachedToken = (String) responseBody.get("access_token");
            this.expiresAt = clock.instant().plusSeconds(expiresInSeconds(responseBody));

            if (this.cachedToken == null) {
                throw new ZoomAuthenticationException("Zoom 응답에 access_token 이 없습니다.");
            }
        } catch (ZoomAuthenticationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Zoom Access Token 발급 실패 - {}: {}", e.getClass().getSimpleName(), e.getMessage());
            throw new ZoomAuthenticationException("Zoom 인증에 실패했습니다.", e);
        }
    }

    private long expiresInSeconds(Map<String, Object> responseBody) {
        Object expiresIn = responseBody.get("expires_in");
        if (expiresIn instanceof Number number) {
            return number.longValue();
        }
        return Duration.ofHours(1).toSeconds();
    }
}

package com.example.doktoribackend.auth;

import com.example.doktoribackend.auth.dto.KakaoTokenResponse;
import com.example.doktoribackend.auth.dto.KakaoUserResponse;
import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.exception.BusinessException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 카카오 OAuth 클라이언트.
 *
 * <p>로그인은 대체 공급자가 없어 fallback 이 성립하지 않는다. 서킷의 역할은 복구가 아니라
 * 빠른 실패이며, 카카오 장애(503)와 사용자 인증 실패(401)를 구분해 응답한다.
 *
 * <p>{@link #exchangeToken}에는 재시도를 걸지 않는다. 인가 코드는 일회용이라 재사용 시
 * 카카오가 거부한다. 타임아웃으로 응답을 못 받았어도 코드는 이미 소비됐을 수 있어,
 * 재시도하면 확정적으로 실패한다. {@link #fetchUser}는 멱등이라 재시도 대상이다.
 */
@Component
@Slf4j
public class KakaoOAuthClient {

    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final String authorizeUrl;
    private final String tokenUrl;
    private final String userInfoUrl;

    public KakaoOAuthClient(
            RestClient kakaoOAuthRestClient,
            @Value("${kakao.oauth.client-id}") String clientId,
            @Value("${kakao.oauth.client-secret}") String clientSecret,
            @Value("${kakao.oauth.redirect-uri}") String redirectUri,
            @Value("${kakao.oauth.authorize-url}") String authorizeUrl,
            @Value("${kakao.oauth.token-url}") String tokenUrl,
            @Value("${kakao.oauth.user-info-url}") String userInfoUrl
    ) {
        this.restClient = kakaoOAuthRestClient;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.authorizeUrl = authorizeUrl;
        this.tokenUrl = tokenUrl;
        this.userInfoUrl = userInfoUrl;
    }

    public String buildAuthorizeUrl(String state) {
        return UriComponentsBuilder.fromUriString(authorizeUrl)
                .queryParam("response_type", "code")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope", "profile_nickname profile_image")
                .queryParam("state", state)
                .build()
                .toUriString();
    }

    @CircuitBreaker(name = "kakaoAuth", fallbackMethod = "exchangeTokenFallback")
    public KakaoTokenResponse exchangeToken(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", clientId);
        form.add("redirect_uri", redirectUri);
        form.add("code", code);
        if (clientSecret != null && !clientSecret.isBlank()) {
            form.add("client_secret", clientSecret);
        }

        KakaoTokenResponse response = restClient.post()
                .uri(tokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(KakaoTokenResponse.class);

        if (response == null) {
            throw new BusinessException(ErrorCode.KAKAO_TOKEN_FETCH_FAILED);
        }
        return response;
    }

    KakaoTokenResponse exchangeTokenFallback(String code, Throwable throwable) {
        throw toBusinessException("카카오 토큰 발급", throwable, ErrorCode.KAKAO_TOKEN_FETCH_FAILED);
    }

    @CircuitBreaker(name = "kakaoAuth")
    @Retry(name = "kakaoUserInfo", fallbackMethod = "fetchUserFallback")
    public KakaoUserResponse fetchUser(String accessToken) {
        KakaoUserResponse response = restClient.get()
                .uri(userInfoUrl)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(KakaoUserResponse.class);

        if (response == null) {
            throw new BusinessException(ErrorCode.KAKAO_USER_INFO_FETCH_FAILED);
        }
        return response;
    }

    KakaoUserResponse fetchUserFallback(String accessToken, Throwable throwable) {
        throw toBusinessException("카카오 사용자 정보 조회", throwable, ErrorCode.KAKAO_USER_INFO_FETCH_FAILED);
    }

    /**
     * 서킷이 열린 상태(카카오 장애)는 503 으로, 그 외 실패는 기존 401 로 구분해 응답한다.
     */
    private BusinessException toBusinessException(String operation, Throwable throwable, ErrorCode defaultCode) {
        if (throwable instanceof BusinessException businessException) {
            return businessException;
        }
        if (throwable instanceof CallNotPermittedException) {
            log.warn("{} 차단 - 카카오 인증 서킷 Open", operation);
            return new BusinessException(ErrorCode.KAKAO_AUTH_UNAVAILABLE);
        }
        log.warn("{} 실패 - {}: {}", operation, throwable.getClass().getSimpleName(), throwable.getMessage());
        return new BusinessException(defaultCode);
    }
}

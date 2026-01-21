package com.example.doktoribackend.auth;

import com.example.doktoribackend.auth.dto.KakaoTokenResponse;
import com.example.doktoribackend.auth.dto.KakaoUserResponse;
import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
@Slf4j
public class KakaoOAuthClient {

    private final WebClient webClient;

    @Value("${kakao.oauth.client-id}")
    private String clientId;

    @Value("${kakao.oauth.client-secret}")
    private String clientSecret;

    @Value("${kakao.oauth.redirect-uri}")
    private String redirectUri;

    @Value("${kakao.oauth.authorize-url}")
    private String authorizeUrl;

    @Value("${kakao.oauth.token-url}")
    private String tokenUrl;

    @Value("${kakao.oauth.user-info-url}")
    private String userInfoUrl;

    public String buildAuthorizeUrl(String state) {
        return UriComponentsBuilder.fromUriString(authorizeUrl)
                .queryParam("response_type", "code")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope", "profile_nickname profile_image account_email")
                .queryParam("state", state)
                .build()
                .toUriString();
    }

    public KakaoTokenResponse exchangeToken(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", clientId);
        form.add("redirect_uri", redirectUri);
        form.add("code", code);
        if (clientSecret != null && !clientSecret.isBlank()) {
            form.add("client_secret", clientSecret);
        }

        try {
            return webClient.post()
                    .uri(tokenUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .bodyValue(form)
                    .retrieve()
                    .bodyToMono(KakaoTokenResponse.class)
                    .blockOptional()
                    .orElseThrow(() -> new BusinessException(ErrorCode.KAKAO_TOKEN_FETCH_FAILED));
        } catch (WebClientResponseException ex) {
            log.warn("Kakao token exchange failed: status={}, body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new BusinessException(ErrorCode.KAKAO_TOKEN_FETCH_FAILED);
        } catch (Exception ex) {
            log.warn("Kakao token exchange failed: {}", ex.getMessage());
            throw new BusinessException(ErrorCode.KAKAO_TOKEN_FETCH_FAILED);
        }
    }

    public KakaoUserResponse fetchUser(String accessToken) {
        try {
            return webClient.get()
                    .uri(userInfoUrl)
                    .headers(h -> h.setBearerAuth(accessToken))
                    .retrieve()
                    .bodyToMono(KakaoUserResponse.class)
                    .blockOptional()
                    .orElseThrow(() -> new BusinessException(ErrorCode.KAKAO_USER_INFO_FETCH_FAILED));
        } catch (WebClientResponseException ex) {
            log.warn("Kakao user info failed: status={}, body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new BusinessException(ErrorCode.KAKAO_USER_INFO_FETCH_FAILED);
        } catch (Exception ex) {
            log.warn("Kakao user info failed: {}", ex.getMessage());
            throw new BusinessException(ErrorCode.KAKAO_USER_INFO_FETCH_FAILED);
        }
    }
}

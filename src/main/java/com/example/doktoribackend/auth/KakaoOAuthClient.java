package com.example.doktoribackend.auth;

import com.example.doktoribackend.auth.dto.KakaoTokenResponse;
import com.example.doktoribackend.auth.dto.KakaoUserResponse;
import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
@Slf4j
public class KakaoOAuthClient {

    private final RestTemplate restTemplate;

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
                .queryParam("scope", "profile_nickname profile_image")
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

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);

        try {
            ResponseEntity<KakaoTokenResponse> response = restTemplate.exchange(
                    tokenUrl,
                    HttpMethod.POST,
                    request,
                    KakaoTokenResponse.class
            );

            if (response.getBody() == null) {
                throw new BusinessException(ErrorCode.KAKAO_TOKEN_FETCH_FAILED);
            }
            return response.getBody();
        } catch (RestClientException ex) {
            log.warn("Kakao token exchange failed: {}", ex.getMessage());
            throw new BusinessException(ErrorCode.KAKAO_TOKEN_FETCH_FAILED);
        }
    }

    public KakaoUserResponse fetchUser(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<KakaoUserResponse> response = restTemplate.exchange(
                    userInfoUrl,
                    HttpMethod.GET,
                    request,
                    KakaoUserResponse.class
            );

            if (response.getBody() == null) {
                throw new BusinessException(ErrorCode.KAKAO_USER_INFO_FETCH_FAILED);
            }
            return response.getBody();
        } catch (RestClientException ex) {
            log.warn("Kakao user info failed: {}", ex.getMessage());
            throw new BusinessException(ErrorCode.KAKAO_USER_INFO_FETCH_FAILED);
        }
    }
}

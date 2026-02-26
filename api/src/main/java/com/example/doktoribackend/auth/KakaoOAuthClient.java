package com.example.doktoribackend.auth;

import com.example.doktoribackend.auth.dto.KakaoTokenResponse;
import com.example.doktoribackend.auth.dto.KakaoUserResponse;
import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

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
            @Value("${kakao.oauth.client-id}") String clientId,
            @Value("${kakao.oauth.client-secret}") String clientSecret,
            @Value("${kakao.oauth.redirect-uri}") String redirectUri,
            @Value("${kakao.oauth.authorize-url}") String authorizeUrl,
            @Value("${kakao.oauth.token-url}") String tokenUrl,
            @Value("${kakao.oauth.user-info-url}") String userInfoUrl
    ) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.authorizeUrl = authorizeUrl;
        this.tokenUrl = tokenUrl;
        this.userInfoUrl = userInfoUrl;
        this.restClient = RestClient.builder().build();
    }

    public String buildAuthorizeUrl(String state) {
        return org.springframework.web.util.UriComponentsBuilder.fromUriString(authorizeUrl)
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

        try {
            KakaoTokenResponse response = restClient.post()
                    .uri(tokenUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        log.warn("Kakao token exchange failed: status={}", res.getStatusCode());
                        throw new BusinessException(ErrorCode.KAKAO_TOKEN_FETCH_FAILED);
                    })
                    .body(KakaoTokenResponse.class);

            if (response == null) {
                throw new BusinessException(ErrorCode.KAKAO_TOKEN_FETCH_FAILED);
            }
            return response;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Kakao token exchange failed: {}", ex.getMessage());
            throw new BusinessException(ErrorCode.KAKAO_TOKEN_FETCH_FAILED);
        }
    }

    public KakaoUserResponse fetchUser(String accessToken) {
        try {
            KakaoUserResponse response = restClient.get()
                    .uri(userInfoUrl)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        log.warn("Kakao user info failed: status={}", res.getStatusCode());
                        throw new BusinessException(ErrorCode.KAKAO_USER_INFO_FETCH_FAILED);
                    })
                    .body(KakaoUserResponse.class);

            if (response == null) {
                throw new BusinessException(ErrorCode.KAKAO_USER_INFO_FETCH_FAILED);
            }
            return response;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Kakao user info failed: {}", ex.getMessage());
            throw new BusinessException(ErrorCode.KAKAO_USER_INFO_FETCH_FAILED);
        }
    }
}

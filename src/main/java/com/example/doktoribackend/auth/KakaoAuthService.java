package com.example.doktoribackend.auth;

import com.example.doktoribackend.auth.dto.KakaoTokenResponse;
import com.example.doktoribackend.auth.dto.KakaoUserResponse;
import com.example.doktoribackend.auth.dto.LoginResult;
import com.example.doktoribackend.common.util.CookieUtil;
import com.example.doktoribackend.security.jwt.JwtTokenProvider;
import com.example.doktoribackend.user.domain.User;
import com.example.doktoribackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletResponse;

@Service
@RequiredArgsConstructor
public class KakaoAuthService {

    private final KakaoOAuthClient kakaoOAuthClient;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${kakao.oauth.frontend-redirect:http://localhost:3000/oauth/callback}")
    private String frontendRedirect;

    public String buildAuthorizeUrl(String state) {
        return kakaoOAuthClient.buildAuthorizeUrl(state);
    }

    @Transactional
    public LoginResult handleCallback(String code, HttpServletResponse response) {
        KakaoTokenResponse tokenResponse = kakaoOAuthClient.exchangeToken(code);
        KakaoUserResponse userResponse = kakaoOAuthClient.fetchUser(tokenResponse.getAccessToken());

        Long kakaoId = userResponse.getId();
        String email = userResponse.getKakaoAccount() != null ? userResponse.getKakaoAccount().getEmail() : null;
        String rawNickname = userResponse.getKakaoAccount() != null && userResponse.getKakaoAccount().getProfile() != null
                ? userResponse.getKakaoAccount().getProfile().getNickname()
                : null;
        String nickname = (rawNickname == null || rawNickname.isBlank())
                ? "kakao-" + kakaoId
                : rawNickname;

        User user = userRepository.findByKakaoIdAndDeletedAtIsNull(kakaoId)
                .orElseGet(() -> userRepository.findByEmailAndDeletedAtIsNull(email)
                        .orElseGet(() -> userRepository.save(User.createKakaoUser(kakaoId, email, nickname))));

        user.updateProfile(email, nickname);
        user.linkKakaoId(kakaoId);

        String accessToken = jwtTokenProvider.createAccessToken(user);
        String refreshToken = jwtTokenProvider.createRefreshToken(user);
        CookieUtil.addRefreshTokenCookie(response, refreshToken, jwtTokenProvider.getRefreshExpSeconds());

        return new LoginResult(accessToken);
    }

    public String buildFrontendRedirect(String accessToken, String state) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(frontendRedirect)
                .queryParam("accessToken", accessToken);
        if (state != null && !state.isBlank()) {
            builder.queryParam("state", state);
        }
        return builder.build().toUriString();
    }
}

package com.example.doktoribackend.auth.service;

import com.example.doktoribackend.auth.KakaoOAuthClient;
import com.example.doktoribackend.auth.dto.KakaoTokenResponse;
import com.example.doktoribackend.auth.dto.KakaoUserResponse;
import com.example.doktoribackend.auth.dto.OAuthProvider;
import com.example.doktoribackend.auth.dto.TokenResponse;
import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.exception.BusinessException;
import com.example.doktoribackend.user.domain.User;
import com.example.doktoribackend.user.domain.UserAccount;
import com.example.doktoribackend.user.domain.preference.UserPreference;
import com.example.doktoribackend.user.domain.UserStat;
import com.example.doktoribackend.user.repository.UserAccountRepository;
import com.example.doktoribackend.user.repository.UserPreferenceRepository;
import com.example.doktoribackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class KakaoAuthService implements OAuthService {

    private final KakaoOAuthClient kakaoOAuthClient;
    private final UserRepository userRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final TokenService tokenService;

    @Value("${kakao.oauth.frontend-redirect}")
    private String frontendRedirect;

    @Override
    public String buildAuthorizeUrl(String state) {
        return kakaoOAuthClient.buildAuthorizeUrl(state);
    }

    @Override
    @Transactional
    public TokenResponse handleCallback(String code) {
        KakaoTokenResponse tokenResponse = kakaoOAuthClient.exchangeToken(code);

        KakaoUserResponse userResponse = kakaoOAuthClient.fetchUser(tokenResponse.accessToken());
        Long kakaoId = userResponse.id();

        UserAccount existingAccount = userAccountRepository
                .findByProviderAndProviderId(OAuthProvider.KAKAO, String.valueOf(kakaoId))
                .orElse(null);

        String nickname = extractNickname(userResponse, kakaoId);
        String profileImagePath = extractProfileImage(userResponse);
        if (existingAccount != null) {
            return handleExistingUser(existingAccount, nickname, profileImagePath);
        }

        return handleNewUser(userResponse, nickname, profileImagePath);
    }

    @Override
    public String buildFrontendRedirect(String state) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(frontendRedirect);
        if (state != null && !state.isBlank()) {
            builder.queryParam("state", state);
        }
        return builder.build().toUriString();
    }

    @Override
    public OAuthProvider getProvider() {
        return OAuthProvider.KAKAO;
    }


    private TokenResponse handleExistingUser(UserAccount userAccount,
                                             String nickname,
                                             String profileImagePath) {
        User user = userAccount.getUser();

        if (user.isDeleted()) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        user.updateNickname(nickname);
        if (profileImagePath != null && !profileImagePath.isBlank()) {
            user.updateProfileImage(profileImagePath);
        }
        ensurePreference(user);

        return tokenService.issueTokens(user);
    }

    private TokenResponse handleNewUser(KakaoUserResponse userResponse,
                                        String nickname,
                                        String profileImagePath) {
        Long kakaoId = userResponse.id();

        User newUser = createUser(nickname, profileImagePath, String.valueOf(kakaoId));

        return tokenService.issueTokens(newUser);
    }

    private User createUser(String nickname, String profileImagePath, String providerId) {
        User user = User.builder()
                .nickname(nickname)
                .profileImagePath(profileImagePath)
                .build();

        UserAccount userAccount = UserAccount.builder()
                .user(user)
                .provider(OAuthProvider.KAKAO)
                .providerId(providerId)
                .build();

        UserPreference userPreference = UserPreference.builder()
                .user(user)
                .build();

        UserStat userStat = UserStat.builder()
                .user(user)
                .build();

        user.linkAccount(userAccount);
        user.linkPreference(userPreference);
        user.linkStat(userStat);

        return userRepository.save(user);
    }

    private String extractNickname(KakaoUserResponse userResponse, Long kakaoId) {
        String rawNickname = null;

        if (userResponse.kakaoAccount() != null
                && userResponse.kakaoAccount().profile() != null) {
            rawNickname = userResponse.kakaoAccount().profile().nickname();
        }

        if (rawNickname == null || rawNickname.isBlank()) {
            return "kakao-" + kakaoId;
        }

        return rawNickname;
    }

    private String extractProfileImage(KakaoUserResponse userResponse) {
        if (userResponse.kakaoAccount() != null
                && userResponse.kakaoAccount().profile() != null) {
            return userResponse.kakaoAccount().profile().profileImageUrl();
        }
        return null;
    }

    private void ensurePreference(User user) {
        UserPreference preference = user.getUserPreference();
        if (preference == null) {
            UserPreference created = UserPreference.builder()
                    .user(user)
                    .build();
            user.linkPreference(created);
            userPreferenceRepository.save(created);
        }
    }
}

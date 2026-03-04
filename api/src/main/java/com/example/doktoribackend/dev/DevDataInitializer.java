package com.example.doktoribackend.dev;

import com.example.doktoribackend.auth.dto.OAuthProvider;
import com.example.doktoribackend.user.domain.User;
import com.example.doktoribackend.user.domain.UserAccount;
import com.example.doktoribackend.user.domain.UserStat;
import com.example.doktoribackend.user.domain.preference.UserPreference;
import com.example.doktoribackend.user.repository.UserAccountRepository;
import com.example.doktoribackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Profile("dev")
@Component
@RequiredArgsConstructor
public class DevDataInitializer implements ApplicationRunner {

    static final String DEV_PROVIDER_ID_PREFIX = "dev_test_";
    private static final int TEST_USER_COUNT = 100;

    private final UserRepository userRepository;
    private final UserAccountRepository userAccountRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userAccountRepository.existsByProviderAndProviderId(OAuthProvider.KAKAO, DEV_PROVIDER_ID_PREFIX + "1")) {
            log.info("[Dev] Test users already exist, skipping seed");
            return;
        }

        for (int i = 1; i <= TEST_USER_COUNT; i++) {
            createTestUser(i);
        }

        log.info("[Dev] Created {} test users", TEST_USER_COUNT);
    }

    private void createTestUser(int index) {
        User user = User.builder()
                .nickname("testuser_" + index)
                .profileImagePath("images/profiles/273ddbd8-028c-4521-87b3-338f2e7de116.jpg")
                .build();

        UserAccount userAccount = UserAccount.builder()
                .user(user)
                .provider(OAuthProvider.KAKAO)
                .providerId(DEV_PROVIDER_ID_PREFIX + index)
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

        userRepository.save(user);
    }
}

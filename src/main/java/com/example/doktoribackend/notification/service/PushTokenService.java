package com.example.doktoribackend.notification.service;

import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.exception.BusinessException;
import com.example.doktoribackend.notification.domain.Platform;
import com.example.doktoribackend.notification.domain.PushProvider;
import com.example.doktoribackend.notification.domain.UserPushToken;
import com.example.doktoribackend.notification.repository.UserPushTokenRepository;
import com.example.doktoribackend.user.domain.User;
import com.example.doktoribackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PushTokenService {

    private final UserPushTokenRepository userPushTokenRepository;
    private final UserRepository userRepository;

    @Transactional
    public void registerToken(Long userId, String token, Platform platform) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Optional<UserPushToken> existingToken = userPushTokenRepository.findByToken(token);
        if (existingToken.isPresent() && !existingToken.get().getUserId().equals(userId)) {
            userPushTokenRepository.delete(existingToken.get());
        }

        Optional<UserPushToken> userToken = userPushTokenRepository.findById(userId);

        if (userToken.isPresent()) {
            userToken.get().updateToken(token, platform);
        } else {
            UserPushToken newToken = UserPushToken.builder()
                    .user(user)
                    .platform(platform)
                    .provider(PushProvider.FCM)
                    .token(token)
                    .build();
            userPushTokenRepository.save(newToken);
        }
    }

}

package com.example.doktoribackend.dev;

import com.example.doktoribackend.auth.dto.OAuthProvider;
import com.example.doktoribackend.notification.domain.NotificationTypeCode;
import com.example.doktoribackend.notification.service.NotificationService;
import com.example.doktoribackend.security.jwt.JwtTokenProvider;
import com.example.doktoribackend.user.domain.UserAccount;
import com.example.doktoribackend.user.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Profile("dev")
@RestController
@RequestMapping("/dev")
@RequiredArgsConstructor
public class DevController {

    private final UserAccountRepository userAccountRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final NotificationService notificationService;

    @GetMapping("/tokens")
    public ResponseEntity<List<DevTokenResponse>> getTokens() {
        List<UserAccount> testAccounts = userAccountRepository
                .findAllByProviderAndProviderIdStartingWith(OAuthProvider.KAKAO, DevDataInitializer.DEV_PROVIDER_ID_PREFIX);

        List<DevTokenResponse> tokens = testAccounts.stream()
                .map(account -> {
                    Long userId = account.getUserId();
                    String nickname = account.getUser().getNickname();
                    String accessToken = jwtTokenProvider.createAccessToken(userId, nickname);
                    return new DevTokenResponse(userId, nickname, accessToken);
                })
                .toList();

        return ResponseEntity.ok(tokens);
    }

    @PostMapping("/trigger-notification/{userId}")
    public ResponseEntity<Void> triggerNotification(@PathVariable Long userId) {
        notificationService.createAndSend(
                userId,
                NotificationTypeCode.BOOK_REPORT_CHECKED,
                Map.of("meetingTitle", "부하테스트 모임", "meetingId", "0")
        );
        return ResponseEntity.ok().build();
    }

    record DevTokenResponse(Long userId, String nickname, String accessToken) {}
}

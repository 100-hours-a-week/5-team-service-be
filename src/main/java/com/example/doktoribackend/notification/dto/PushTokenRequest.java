package com.example.doktoribackend.notification.dto;

import com.example.doktoribackend.notification.domain.Platform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PushTokenRequest(
        @NotBlank(message = "FCM 토큰은 필수입니다")
        @Size(max = 512, message = "토큰은 512자를 초과할 수 없습니다")
        String token,

        @NotNull(message = "플랫폼은 필수입니다")
        Platform platform
) {
}

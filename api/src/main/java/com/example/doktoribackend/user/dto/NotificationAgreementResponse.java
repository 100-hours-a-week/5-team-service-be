package com.example.doktoribackend.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record NotificationAgreementResponse(
        @Schema(description = "알림 수신 동의 여부", example = "true")
        boolean notificationAgreement
) {}

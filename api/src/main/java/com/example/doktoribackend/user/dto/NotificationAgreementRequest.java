package com.example.doktoribackend.user.dto;

import jakarta.validation.constraints.NotNull;

public record NotificationAgreementRequest(
        @NotNull(message = "알림 수신 여부는 필수입니다.")
        Boolean notificationAgreement
) {}

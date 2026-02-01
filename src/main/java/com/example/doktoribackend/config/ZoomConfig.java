package com.example.doktoribackend.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "zoom")
@Validated
public class ZoomConfig {

    @NotBlank(message = "Zoom Account ID는 필수입니다")
    private final String accountId;

    @NotBlank(message = "Zoom Client ID는 필수입니다")
    private final String clientId;

    @NotBlank(message = "Zoom Client Secret은 필수입니다")
    private final String clientSecret;

    @NotBlank(message = "Zoom API Base URL은 필수입니다")
    private final String apiBaseUrl;
}
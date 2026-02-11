package com.example.doktoribackend.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AccessTokenResponse", description = "액세스 토큰 응답")
public record AccessTokenResponse(
        @Schema(description = "액세스 토큰", example = "eyJhbGciOiJIUzI1NiI...")
        String accessToken
) {}


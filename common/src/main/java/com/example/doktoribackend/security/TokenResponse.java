package com.example.doktoribackend.security;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
@Schema(name = "TokenResponse", description = "토큰 응답")
public class TokenResponse {
    @Schema(description = "액세스 토큰", example = "eyJhbGciOiJIUzI1NiI...")
    private final String accessToken;
}
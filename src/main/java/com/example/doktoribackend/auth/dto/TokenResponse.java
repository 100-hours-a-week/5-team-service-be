package com.example.doktoribackend.auth.dto;

public record TokenResponse(
        String accessToken,

        String refreshToken
) { }


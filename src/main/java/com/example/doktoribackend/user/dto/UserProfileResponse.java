package com.example.doktoribackend.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 프로필 응답")
public record UserProfileResponse(

        @Schema(description = "닉네임", example = "startup")
        String nickname,

        @Schema(description = "프로필 이미지 경로", example = "https://image.kr/img.jpg")
        String profileImagePath,

        @Schema(description = "프로필 정보 입력 완료 여부", example = "true")
        boolean profileCompleted,

        @Schema(description = "온보딩 완료 여부", example = "true")
        boolean onboardingCompleted,

        @Schema(description = "리더 소개", nullable = true)
        String leaderIntro,

        @Schema(description = "멤버 소개", nullable = true)
        String memberIntro
) {}
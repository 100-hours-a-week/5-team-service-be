package com.example.doktoribackend.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;


@Schema(description = "사용자 프로필 수정 요청")
public record UpdateUserProfileRequest (

    @Schema(description = "닉네임", example = "newNickname")
    String nickname,

    @Schema(description = "프로필 이미지 경로", example = "https://new.image.kr/img.jpg")
    String profileImagePath,

    @Schema(description = "리더 소개", nullable = true, example = "leaderIntro")
    String leaderIntro,

    @Schema(description = "멤버 소개", nullable = true, example = "memberIntro")
    String memberIntro
){ }

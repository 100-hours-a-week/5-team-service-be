package com.example.doktoribackend.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import static com.example.doktoribackend.common.constants.ValidationConstant.*;

@Schema(description = "사용자 프로필 수정 요청")
public record UpdateUserProfileRequest(

        @Schema(description = "닉네임", example = "newNickname")
        @NotBlank(message = "닉네임은 필수입니다")
        @Size(max = NICKNAME_MAX_LENGTH, message = "닉네임은 20자를 초과할 수 없습니다")
        @Pattern(
                regexp = "^[^\\p{Cntrl}\\p{So}\\p{Cs}]*$",
                message = "닉네임에 제어문자 및 이모지를 사용할 수 없습니다"
        )
        String nickname,

        @Schema(description = "프로필 이미지 경로", example = "https://image.kr/img.jpg", nullable = true)
        @Size(max = PROFILE_IMAGE_PATH_MAX_LENGTH, message = "프로필 이미지 경로는 512자를 초과할 수 없습니다")
        String profileImagePath,

        @Schema(description = "리더 소개", nullable = true, example = "리더 소개입니다")
        @Size(max = INTRO_MAX_LENGTH, message = "소개는 300자를 초과할 수 없습니다")
        @Pattern(
                regexp = "^[^\\p{Cntrl}\\p{So}\\p{Cs}]*$",
                message = "소개에 제어문자 및 이모지를 사용할 수 없습니다"
        )
        String leaderIntro,

        @Schema(description = "멤버 소개", nullable = true, example = "멤버 소개입니다")
        @Size(max = INTRO_MAX_LENGTH, message = "소개는 300자를 초과할 수 없습니다")
        @Pattern(
                regexp = "^[^\\p{Cntrl}\\p{So}\\p{Cs}]*$",
                message = "소개에 제어문자 및 이모지를 사용할 수 없습니다"
        )
        String memberIntro
) {}
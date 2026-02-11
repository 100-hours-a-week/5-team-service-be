package com.example.doktoribackend.exception;

import com.example.doktoribackend.common.error.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor(force = true)
@Schema(name = "ErrorResponseDto", description = "공통 에러 래퍼")
public class ErrorResponseDto {
    @Schema(description = "결과 코드", example = "USER_NOT_FOUND")
    private final String code;

    @Schema(description = "메시지", example = "사용자를 찾을 수 없습니다.")
    private final String message;

    @Schema(description = "필드 단위 오류", nullable = true)
    private final List<FieldErrorDetail> errors;

    public static ErrorResponseDto from(CustomException e) {
        return new ErrorResponseDto(e.getErrorCode().getCode(), e.getMessage(), null);
    }

    public static ErrorResponseDto of(String code, String message) {
        return new ErrorResponseDto(code, message, null);
    }

    public static ErrorResponseDto of(ErrorCode errorCode) {
        return new ErrorResponseDto(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static ErrorResponseDto of(ErrorCode errorCode, List<FieldErrorDetail> fieldErrors) {
        return new ErrorResponseDto(errorCode.getCode(), errorCode.getMessage(), fieldErrors);
    }

    @Getter
    @AllArgsConstructor
    public static class FieldErrorDetail {
        @Schema(description = "필드명", example = "nickname")
        private final String field;

        @Schema(description = "오류 이유 코드", example = "LENGTH")
        private final String reason;

        @Schema(description = "오류 메시지", example = "닉네임은 1~20자 사이여야 합니다.")
        private final String message;
    }
}

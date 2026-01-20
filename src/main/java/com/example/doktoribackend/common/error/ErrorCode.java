package com.example.doktoribackend.common.error;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    // Common
    INVALID_INPUT_VALUE(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_FAILED", "요청 값이 유효하지 않습니다."),
    INVALID_REQUEST_BODY(HttpStatus.BAD_REQUEST, "INVALID_REQUEST_BODY", "요청 본문을 읽을 수 없습니다."),
    INVALID_PARAMETER_TYPE(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER_TYPE", "요청 파라미터 타입이 올바르지 않습니다."),
    MISSING_PARAMETER(HttpStatus.BAD_REQUEST, "MISSING_PARAMETER", "필수 요청 파라미터가 누락되었습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "지원하지 않는 HTTP 메서드입니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "리소스를 찾을 수 없습니다."),
    CONFLICT(HttpStatus.CONFLICT, "CONFLICT", "요청이 현재 리소스 상태와 충돌합니다."),
    OPTIMISTIC_LOCK_FAILURE(HttpStatus.CONFLICT, "OPTIMISTIC_LOCK_FAILURE", "리소스가 동시에 수정되었습니다. 다시 시도해주세요."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다."),

    // OAuth
    KAKAO_TOKEN_FETCH_FAILED(HttpStatus.UNAUTHORIZED, "KAKAO_TOKEN_FETCH_FAILED", "카카오 토큰 발급에 실패했습니다."),
    KAKAO_USER_INFO_FETCH_FAILED(HttpStatus.UNAUTHORIZED, "KAKAO_USER_INFO_FETCH_FAILED", "카카오 사용자 정보 조회에 실패했습니다."),
    UNSUPPORTED_PROVIDER(HttpStatus.BAD_REQUEST, "UNSUPPORTED_PROVIDER", "지원하지 않는 OAuth 제공자입니다."),
    INVALID_OAUTH_STATE(HttpStatus.UNAUTHORIZED, "INVALID_OAUTH_STATE", "OAuth 상태 값이 일치하지 않습니다."),

    // Auth
    AUTH_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_UNAUTHORIZED", "인증이 필요합니다."),
    AUTH_FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH_FORBIDDEN", "접근 권한이 없습니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED", "토큰이 만료되었습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "유효하지 않은 리프레시 토큰입니다."),
    INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_ACCESS_TOKEN", "유효하지 않은 액세스 토큰입니다."),
    NOT_EXIST_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "NOT_EXIST_REFRESH_TOKEN", "리프레시 토큰이 존재하지 않습니다."),
    INVALID_TOKEN_REUSE_DETECTED(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN_REUSE_DETECTED", "리프레시 토큰 재사용이 감지되었습니다. 모든 토큰이 무효화됩니다."),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_EXPIRED"," 리프레시 토큰이 만료되었습니다."),
    INVALID_USER_ID(HttpStatus.BAD_REQUEST, "INVALID_USER_ID", "유효하지 않은 사용자 ID입니다."),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "존재하지 않는 사용자입니다."),

    // Policy
    POLICY_NOT_FOUND(HttpStatus.NOT_FOUND, "POLICY_NOT_FOUND", "정책을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}

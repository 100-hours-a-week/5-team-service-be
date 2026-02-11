package com.example.doktoribackend.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Value
@Builder
@Schema(name = "ApiResult", description = "공통 응답 래퍼")
public class ApiResult<T> {
    public static final String DEFAULT_SUCCESS_MESSAGE = "OK";

    @Schema(description = "메시지", example = "OK")
    String message;
    @Schema(description = "응답 데이터")
    T data;

    public static <T> ApiResult<T> ok(T data) {
        return ApiResult.<T>builder().message(DEFAULT_SUCCESS_MESSAGE).data(data).build();
    }

    public static ApiResult<Void> ok() {
        return ApiResult.<Void>builder().message(DEFAULT_SUCCESS_MESSAGE).build();
    }

    public static <T> ApiResult<T> ok(String message, T data) {
        return ApiResult.<T>builder().message(message).data(data).build();
    }

    public static ApiResult<Void> ok(String message) {
        return ApiResult.<Void>builder().message(message).build();
    }
}

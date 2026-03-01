package com.example.doktoribackend.exception;

import com.example.doktoribackend.common.error.ErrorCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponseDto> handleCustomException(CustomException ex) {
        log.warn("[{}] {}: {}", ex.getStatus().value(), ex.getErrorCode(), ex.getMessage());
        return ResponseEntity
                .status(ex.getStatus())
                .body(ErrorResponseDto.from(ex));
    }

    // === 422 검증 위반 ===
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        List<ErrorResponseDto.FieldErrorDetail> errors = fieldErrors(ex.getBindingResult());
        log.warn("[422] Validation failed: {}", summarize(errors));
        return toResponse(ErrorCode.INVALID_INPUT_VALUE, errors);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponseDto> handleBindException(BindException ex) {
        List<ErrorResponseDto.FieldErrorDetail> errors = fieldErrors(ex.getBindingResult());
        log.warn("[422] Bind failed: {}", summarize(errors));
        return toResponse(ErrorCode.INVALID_INPUT_VALUE, errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponseDto> handleConstraintViolation(ConstraintViolationException ex) {
        List<ErrorResponseDto.FieldErrorDetail> errors = ex.getConstraintViolations().stream()
                .map(this::violationToFieldError)
                .toList();
        log.warn("[422] Constraint violation: {}", summarize(errors));
        return toResponse(ErrorCode.INVALID_INPUT_VALUE, errors);
    }

    // === 400 요청 자체가 잘못 됨 ===
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDto> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("[400] Request body not readable: {}", rootCause(ex).getMessage());
        return toResponse(ErrorCode.INVALID_REQUEST_BODY);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDto> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("[400] Parameter type mismatch: {}={}, required {}",
                ex.getName(), ex.getValue(), ex.getRequiredType());
        return toResponse(ErrorCode.INVALID_PARAMETER_TYPE);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponseDto> handleMissingServletRequestParameter(MissingServletRequestParameterException ex) {
        log.warn("[400] Missing parameter: {}", ex.getParameterName());
        return toResponse(ErrorCode.MISSING_PARAMETER);
    }

    // == 405 HTTP 메서드 미지원 ==
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponseDto> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        log.warn("[405] Method not allowed: {}", ex.getMethod());
        return toResponse(ErrorCode.METHOD_NOT_ALLOWED);
    }

    // === 404 경로 없음 ===
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleNoHandlerFound(NoHandlerFoundException ex) {
        log.warn("[404] No handler for {} {}", ex.getHttpMethod(), ex.getRequestURL());
        return toResponse(ErrorCode.NOT_FOUND);
    }

    // === 409 무결성/충돌 ===
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDto> handleDataIntegrity(DataIntegrityViolationException ex) {
        Throwable root = rootCause(ex);
        log.warn("[409] Data integrity violation: {} - {}", root.getClass().getSimpleName(), root.getMessage());
        return toResponse(ErrorCode.CONFLICT);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponseDto> handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        log.warn("[409] Optimistic lock failure: {}", rootCause(ex).getMessage());
        return toResponse(ErrorCode.OPTIMISTIC_LOCK_FAILURE);
    }

    // === 500 기타 ===
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleUnknown(Exception ex) {
        log.error("[500] Unhandled exception", ex);
        return toResponse(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    private Throwable rootCause(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null && cur.getCause() != cur) cur = cur.getCause();
        return cur;
    }

    private ResponseEntity<ErrorResponseDto> toResponse(ErrorCode errorCode) {
        return ResponseEntity.status(errorCode.getStatus())
                .body(ErrorResponseDto.of(errorCode));
    }

    private ResponseEntity<ErrorResponseDto> toResponse(ErrorCode errorCode,
                                                        List<ErrorResponseDto.FieldErrorDetail> errors) {
        return ResponseEntity.status(errorCode.getStatus())
                .body(ErrorResponseDto.of(errorCode, errors));
    }

    private List<ErrorResponseDto.FieldErrorDetail> fieldErrors(BindingResult bindingResult) {
        return bindingResult.getFieldErrors().stream()
                .map(this::fieldErrorToDetail)
                .toList();
    }

    private ErrorResponseDto.FieldErrorDetail fieldErrorToDetail(FieldError fieldError) {
        String reason = fieldError.getCode();
        String message = fieldError.getDefaultMessage();

        return new ErrorResponseDto.FieldErrorDetail(
                fieldError.getField(),
                reason,
                message
        );
    }

    private ErrorResponseDto.FieldErrorDetail violationToFieldError(ConstraintViolation<?> violation) {
        String reason = violation.getMessageTemplate();
        String message = violation.getMessage();

        return new ErrorResponseDto.FieldErrorDetail(
                violation.getPropertyPath() != null ? violation.getPropertyPath().toString() : "",
                reason,
                message
        );
    }

    private String summarize(List<ErrorResponseDto.FieldErrorDetail> errors) {
        return errors.stream()
                .limit(3)
                .map(err -> String.format("%s: %s (%s)", err.getField(), err.getReason(), err.getMessage()))
                .collect(Collectors.joining(", "));
    }
}

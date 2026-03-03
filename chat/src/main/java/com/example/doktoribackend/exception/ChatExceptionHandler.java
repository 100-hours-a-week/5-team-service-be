package com.example.doktoribackend.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@Slf4j
@RestControllerAdvice
@Order(1)
public class ChatExceptionHandler {

    @ExceptionHandler(AlreadyJoinedRoomException.class)
    public ResponseEntity<ErrorResponseDto> handleAlreadyJoinedRoom(AlreadyJoinedRoomException ex) {
        log.warn("[{}] {}: {}", ex.getStatus().value(), ex.getErrorCode(), ex.getMessage());
        return ResponseEntity
                .status(ex.getStatus())
                .body(ErrorResponseDto.of(ex.getErrorCode(), Map.of("roomId", ex.getRoomId())));
    }
}

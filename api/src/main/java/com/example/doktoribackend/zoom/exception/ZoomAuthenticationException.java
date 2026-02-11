package com.example.doktoribackend.zoom.exception;

public class ZoomAuthenticationException extends RuntimeException {

    public ZoomAuthenticationException(String message) {
        super(message);
    }

    public ZoomAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}

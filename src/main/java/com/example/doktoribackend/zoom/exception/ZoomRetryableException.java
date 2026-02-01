package com.example.doktoribackend.zoom.exception;

public class ZoomRetryableException extends RuntimeException {

    public ZoomRetryableException(String message) {
        super(message);
    }

    public ZoomRetryableException(String message, Throwable cause) {
        super(message, cause);
    }
}

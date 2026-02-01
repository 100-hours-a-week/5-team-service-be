package com.example.doktoribackend.exception;


import com.example.doktoribackend.common.error.ErrorCode;

public class BusinessException extends CustomException {
    public BusinessException(ErrorCode errorCode) {
        super(errorCode);
    }
}


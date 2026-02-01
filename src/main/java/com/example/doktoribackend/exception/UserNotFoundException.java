package com.example.doktoribackend.exception;

import com.example.doktoribackend.common.error.ErrorCode;

public class UserNotFoundException extends CustomException {
    public UserNotFoundException() {
        super(ErrorCode.USER_NOT_FOUND);
    }
}

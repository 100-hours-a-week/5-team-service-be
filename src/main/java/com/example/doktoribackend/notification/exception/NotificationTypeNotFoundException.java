package com.example.doktoribackend.notification.exception;

import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.exception.BusinessException;

public class NotificationTypeNotFoundException extends BusinessException {

    public NotificationTypeNotFoundException() {
        super(ErrorCode.NOTIFICATION_TYPE_NOT_FOUND);
    }
}

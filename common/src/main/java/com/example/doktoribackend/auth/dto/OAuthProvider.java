package com.example.doktoribackend.auth.dto;

import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.exception.BusinessException;

public enum OAuthProvider {
    KAKAO;

    public static OAuthProvider fromString(String provider) {
        try {
            return OAuthProvider.valueOf(provider.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_PROVIDER);
        }
    }
}

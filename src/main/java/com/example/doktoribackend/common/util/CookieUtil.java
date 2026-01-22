package com.example.doktoribackend.common.util;


import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import static com.example.doktoribackend.common.constants.CookieConstant.OAUTH_STATE;
import static com.example.doktoribackend.common.constants.CookieConstant.OAUTH_STATE_PATH;
import static com.example.doktoribackend.common.constants.CookieConstant.REFRESH_COOKIE_PATH;
import static com.example.doktoribackend.common.constants.CookieConstant.REFRESH_TOKEN;


public class CookieUtil {

    public static void addRefreshTokenCookie(HttpServletResponse response, String refreshToken, long maxAgeSeconds) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN, refreshToken)
                .httpOnly(true)
                .secure(false)
                .path(REFRESH_COOKIE_PATH)
                .maxAge(maxAgeSeconds)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public static String resolveRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (REFRESH_TOKEN.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    public static void removeRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN, "")
                .httpOnly(true)
                .secure(false)
                .path(REFRESH_COOKIE_PATH)
                .maxAge(0)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public static void addStateCookie(HttpServletResponse response, String state) {
        ResponseCookie cookie = ResponseCookie.from(OAUTH_STATE, state)
                .httpOnly(true)
                .secure(false)
                .path(OAUTH_STATE_PATH)
                .maxAge(600)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public static String resolveState(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (OAUTH_STATE.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    public static void removeStateCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(OAUTH_STATE, "")
                .httpOnly(true)
                .secure(false)
                .path(OAUTH_STATE_PATH)
                .maxAge(0)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}

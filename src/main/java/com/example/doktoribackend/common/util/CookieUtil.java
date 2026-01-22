package com.example.doktoribackend.common.util;


import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import static com.example.doktoribackend.common.constants.CookieConstant.OAUTH_STATE;
import static com.example.doktoribackend.common.constants.CookieConstant.OAUTH_STATE_PATH;
import static com.example.doktoribackend.common.constants.CookieConstant.REFRESH_COOKIE_PATH;
import static com.example.doktoribackend.common.constants.CookieConstant.REFRESH_TOKEN;


@Component
public class CookieUtil {

    private final boolean secure;
    private final String sameSite;

    public CookieUtil(
            @Value("${app.cookie.secure}") boolean secure,
            @Value("${app.cookie.same-site}") String sameSite) {
        this.secure = secure;
        this.sameSite = sameSite;
    }

    public void addRefreshTokenCookie(HttpServletResponse response, String refreshToken, long maxAgeSeconds) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN, refreshToken)
                .httpOnly(true)
                .secure(this.secure)
                .path(REFRESH_COOKIE_PATH)
                .maxAge(maxAgeSeconds)
                .sameSite(this.sameSite)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public String resolveRefreshToken(HttpServletRequest request) {
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

    public void removeRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN, "")
                .httpOnly(true)
                .secure(secure)
                .path(REFRESH_COOKIE_PATH)
                .maxAge(0)
                .sameSite(sameSite)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void addStateCookie(HttpServletResponse response, String state) {
        ResponseCookie cookie = ResponseCookie.from(OAUTH_STATE, state)
                .httpOnly(true)
                .secure(secure)
                .path(OAUTH_STATE_PATH)
                .maxAge(600)
                .sameSite(sameSite)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public String resolveState(HttpServletRequest request) {
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

    public void removeStateCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(OAUTH_STATE, "")
                .httpOnly(true)
                .secure(secure)
                .path(OAUTH_STATE_PATH)
                .maxAge(0)
                .sameSite(sameSite)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}

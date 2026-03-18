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
    private final String domain;

    public CookieUtil(
            @Value("${app.cookie.secure}") boolean secure,
            @Value("${app.cookie.same-site}") String sameSite,
            @Value("${app.cookie.domain:}") String domain) {
        this.secure = secure;
        this.sameSite = sameSite;
        this.domain = domain;
    }

    public void addRefreshTokenCookie(HttpServletResponse response, String refreshToken, long maxAgeSeconds) {
        addCookie(response, REFRESH_TOKEN, refreshToken, REFRESH_COOKIE_PATH, maxAgeSeconds);
    }

    public String resolveRefreshToken(HttpServletRequest request) {
        return resolveCookieValue(request, REFRESH_TOKEN);
    }

    public void removeRefreshTokenCookie(HttpServletResponse response) {
        addCookie(response, REFRESH_TOKEN, "", REFRESH_COOKIE_PATH, 0);
    }

    public void addStateCookie(HttpServletResponse response, String state) {
        addCookie(response, OAUTH_STATE, state, OAUTH_STATE_PATH, 600);
    }

    public String resolveState(HttpServletRequest request) {
        return resolveCookieValue(request, OAUTH_STATE);
    }

    public void removeStateCookie(HttpServletResponse response) {
        addCookie(response, OAUTH_STATE, "", OAUTH_STATE_PATH, 0);
    }

    private void addCookie(HttpServletResponse response, String name, String value, String path, long maxAge) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .path(path)
                .maxAge(maxAge)
                .sameSite(sameSite);
        if (!domain.isEmpty()) {
            builder.domain(domain);
        }
        response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
    }

    private String resolveCookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}

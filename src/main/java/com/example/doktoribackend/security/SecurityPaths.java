package com.example.doktoribackend.security;

public final class SecurityPaths {

    private SecurityPaths() {}

    public static final String[] PUBLIC_AUTH = {
            "/auth/signup",
            "/auth/login",
            "/auth/kakao",
            "/auth/kakao/callback",
            "/api/auth/kakao",
            "/api/auth/kakao/callback",
            "/auth/refresh",
            "/prompts",
            "/relationships",
            "/chat-styles",
            "/health"
    };

    public static final String[] CSRF_IGNORED = {
            "/auth/login",
            "/auth/signup",
            "/auth/kakao",
            "/auth/kakao/callback",
            "/api/auth/kakao",
            "/api/auth/kakao/callback",
            "/auth/refresh",
            "/auth/logout",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/prompts",
            "/users",
            "/targets",
            "/relationships",
            "/health",
            "/prompts/images",
    };

    public static final String[] PUBLIC_DOCS = {
            "/v1/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };
}

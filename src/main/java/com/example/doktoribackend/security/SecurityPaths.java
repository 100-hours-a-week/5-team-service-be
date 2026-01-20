package com.example.doktoribackend.security;

public final class SecurityPaths {

    private SecurityPaths() {}

    public static final String[] PUBLIC_AUTH = {
            "/oauth/**",
            "/auth/**",
            "/health"
    };

    public static final String[] PUBLIC_DOCS = {
            "/v1/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };
}

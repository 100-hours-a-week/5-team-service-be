package com.example.doktoribackend.security;

public final class SecurityPaths {

    private SecurityPaths() {}

    public static final String[] PUBLIC_AUTH = {
            "/oauth/**",
            "/auth/**",
            "/health",
            "/actuator/**",
            "/policies/reading-genres"
    };
}

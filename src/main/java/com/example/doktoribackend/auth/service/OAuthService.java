package com.example.doktoribackend.auth.service;

import com.example.doktoribackend.auth.dto.OAuthProvider;
import com.example.doktoribackend.auth.dto.TokenResponse;

public interface OAuthService {

    String buildAuthorizeUrl(String state);

    TokenResponse handleCallback(String code);

    String buildFrontendRedirect(String state);

    OAuthProvider getProvider();
}

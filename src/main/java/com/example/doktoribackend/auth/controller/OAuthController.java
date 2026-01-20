package com.example.doktoribackend.auth.controller;

import com.example.doktoribackend.auth.component.OAuthServiceFactory;
import com.example.doktoribackend.auth.dto.OAuthProvider;
import com.example.doktoribackend.auth.service.OAuthService;
import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.common.util.CookieUtil;
import com.example.doktoribackend.auth.dto.TokenResponse;
import com.example.doktoribackend.exception.CustomException;
import com.example.doktoribackend.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
@RequestMapping("/oauth")
public class OAuthController {

    private final OAuthServiceFactory oauthFactory;
    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping("/{provider}")
    public RedirectView redirectToOAuth(
            @PathVariable String provider,
            @RequestParam(required = false) String state,
            HttpServletResponse response) {

        OAuthProvider oauthProvider = OAuthProvider.fromString(provider);

        OAuthService service = oauthFactory.getService(oauthProvider);
        String resolvedState = (state == null || state.isBlank()) ? UUID.randomUUID().toString() : state;
        CookieUtil.addStateCookie(response, resolvedState);
        return new RedirectView(service.buildAuthorizeUrl(resolvedState));
    }

    @GetMapping("/{provider}/callback")
    public RedirectView handleCallback(
            @PathVariable String provider,
            @RequestParam String code,
            @RequestParam(required = false) String state,
            HttpServletRequest request,
            HttpServletResponse response) {

        OAuthProvider oauthProvider = OAuthProvider.fromString(provider);
        OAuthService service = oauthFactory.getService(oauthProvider);

        String savedState = CookieUtil.resolveState(request);
        if (savedState == null || !savedState.equals(state)) {
            throw new CustomException(ErrorCode.INVALID_OAUTH_STATE);
        }

        TokenResponse tokens = service.handleCallback(code);

        CookieUtil.addRefreshTokenCookie(
                response,
                tokens.refreshToken(),
                jwtTokenProvider.getRefreshExpSeconds()
        );
        CookieUtil.removeStateCookie(response);

        String redirectUrl = service.buildFrontendRedirect(state);
        return new RedirectView(redirectUrl);
    }
}

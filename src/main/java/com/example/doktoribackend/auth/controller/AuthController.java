package com.example.doktoribackend.auth.controller;

import com.example.doktoribackend.auth.service.TokenService;
import com.example.doktoribackend.common.response.ApiResult;
import com.example.doktoribackend.common.util.CookieUtil;
import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.auth.dto.AccessTokenResponse;
import com.example.doktoribackend.auth.dto.TokenResponse;
import com.example.doktoribackend.exception.CustomException;
import com.example.doktoribackend.security.jwt.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "인증/인가 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenService tokenService;

    @Operation(
            summary = "Access Token 갱신",
            description = "Refresh Token을 사용하여 새로운 Access Token, Refresh Token을 발급받습니다. "
    )
    @PostMapping("/tokens")
    public ResponseEntity<ApiResult<AccessTokenResponse>> refresh(
            HttpServletRequest request,
            HttpServletResponse response) {

        String refreshToken = CookieUtil.resolveRefreshToken(request);

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new CustomException(ErrorCode.NOT_EXIST_REFRESH_TOKEN);
        }

        TokenResponse refreshed = tokenService.refreshTokens(refreshToken);

        CookieUtil.addRefreshTokenCookie(
                response,
                refreshed.refreshToken(),
                jwtTokenProvider.getRefreshExpSeconds()
        );

        AccessTokenResponse accessTokenResponse = new AccessTokenResponse(refreshed.accessToken());

        return ResponseEntity.ok(ApiResult.ok(accessTokenResponse));
    }

    @Operation(
            summary = "로그 아웃",
            description = "Refresh Token을 비활성화하고 쿠키를 삭제합니다 "
    )
    @DeleteMapping("/tokens")
    public ResponseEntity<ApiResult<Void>> logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = CookieUtil.resolveRefreshToken(request);
        if (refreshToken != null && !refreshToken.isBlank()) {
            tokenService.logout(refreshToken);
        }
        CookieUtil.removeRefreshTokenCookie(response);
        return ResponseEntity.noContent().build();
    }
}

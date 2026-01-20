package com.example.doktoribackend.auth;

import com.example.doktoribackend.auth.dto.LoginResult;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final KakaoAuthService kakaoAuthService;

    @GetMapping("/auth/kakao")
    public RedirectView redirectToKakao(@RequestParam(required = false) String state) {
        String authorizeUrl = kakaoAuthService.buildAuthorizeUrl(state);
        return new RedirectView(authorizeUrl);
    }

    @GetMapping("/auth/kakao/callback")
    public RedirectView handleCallback(@RequestParam String code,
                                       @RequestParam(required = false) String state,
                                       HttpServletResponse response) {
        LoginResult loginResult = kakaoAuthService.handleCallback(code, response);
        String redirectUrl = kakaoAuthService.buildFrontendRedirect(loginResult.getAccessToken(), state);
        return new RedirectView(redirectUrl);
    }
}

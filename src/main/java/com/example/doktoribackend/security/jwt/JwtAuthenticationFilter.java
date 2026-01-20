package com.example.doktoribackend.security.jwt;

import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.exception.BusinessException;
import com.example.doktoribackend.security.CustomUserDetails;
import com.example.doktoribackend.security.CustomUserDetailsService;
import com.example.doktoribackend.security.SecurityPaths;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
@Getter
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();

        for (String pattern : SecurityPaths.PUBLIC_AUTH) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    // 모든 요청마다 이 메서드가 호출되고, 여기서 JWT 검증과 인증 처리를 한 뒤 다음 필터로 넘어감
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if(header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        try {
            Long userId = jwtTokenProvider.getUserIdFromAccessToken(token);
            String email = jwtTokenProvider.getEmailFromAccessToken(token);
            String role = jwtTokenProvider.getRoleFromAccessToken(token);
            String nickname = jwtTokenProvider.getNicknameFromAccessToken(token);

            // 이미 다른 필터나 로직에서 인증을 처리한 경우가 아니라면
            if (userId != null &&
                    SecurityContextHolder.getContext().getAuthentication() == null) {

                CustomUserDetails userDetails =
                        CustomUserDetails.fromClaims(userId, email, nickname, role);

                // Authentication 객체 생성
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                // 부가 정보 설정 : IP 주소 등
                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // SecurityContext에 인증 정보 저장
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

        } catch (BusinessException ex) {
            if (ex.getErrorCode() == ErrorCode.INVALID_ACCESS_TOKEN) {
                throw new BusinessException(ErrorCode.AUTH_UNAUTHORIZED);
            }
        }
        // 다음 필터/서블릿으로 넘김
        filterChain.doFilter(request, response);
    }
}

package com.example.doktoribackend.security.jwt;

import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.exception.BusinessException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Component
@Getter
public class JwtTokenProvider {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.access-exp-minutes}")
    private long accessExpMinutes;

    @Value("${app.jwt.refresh-exp-seconds}")
    private long refreshExpSeconds;

    private Key getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(Long userId, String nickname) {
        Instant now = Instant.now();
        Instant exp = now.plus(accessExpMinutes, ChronoUnit.MINUTES);

        return Jwts.builder()
                .setSubject(userId.toString())
                .claim("userId", userId)
                .claim("nickname", nickname)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String createRefreshToken(Long userId, String tokenId) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(refreshExpSeconds);

        return Jwts.builder()
                .setId(tokenId)
                .setSubject(userId.toString())
                .claim("type", "refresh")
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String getNicknameFromAccessToken(String accessToken) {
        Claims claims = parseClaims(accessToken);
        return claims.get("nickname", String.class);
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED);
        } catch (JwtException e) {
            throw new BusinessException(ErrorCode.INVALID_ACCESS_TOKEN);
        }
    }

    private Claims parseRefreshClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        } catch (JwtException e) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
    }

    public Long getUserIdFromAccessToken(String accessToken) {
        Claims claims = parseClaims(accessToken);
        return Long.parseLong(claims.getSubject());
    }

    public Claims validateRefreshToken(String refreshToken) {
        Claims claims = parseRefreshClaims(refreshToken);

        if (!"refresh".equals(claims.get("type", String.class))) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        Date expiration = claims.getExpiration();
        if (expiration != null && expiration.toInstant().isBefore(Instant.now())) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        return claims;
    }

    public String getTokenIdFromRefreshToken(String refreshToken) {
        Claims claims = validateRefreshToken(refreshToken);
        String jti = claims.getId();
        if (jti == null || jti.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        return jti;
    }
}

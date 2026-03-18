package com.example.doktoribackend.auth.service;

import com.example.doktoribackend.auth.domain.RefreshTokenRedis;
import com.example.doktoribackend.auth.repository.RefreshTokenRedisRepository;
import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.exception.BusinessException;
import com.example.doktoribackend.exception.UserNotFoundException;
import com.example.doktoribackend.auth.dto.TokenResponse;
import com.example.doktoribackend.security.jwt.JwtTokenProvider;
import com.example.doktoribackend.user.domain.User;
import com.example.doktoribackend.user.repository.UserRepository;
import com.example.doktoribackend.notification.repository.UserPushTokenRepository;
import com.github.f4b6a3.tsid.TsidCreator;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRedisRepository refreshTokenRedisRepository;
    private final UserRepository userRepository;
    private final UserPushTokenRepository userPushTokenRepository;

    /**
     * 토큰 발급 (로그인 시)
     * - 기존 토큰 모두 삭제 후 새 토큰 발급
     */
    public TokenResponse issueTokens(User user) {
        revokeAllUserTokens(user.getId());
        return createTokenPair(user);
    }

    /**
     * 토큰 갱신 (RTR 적용)
     * - 기존 토큰 삭제 → 새 토큰 발급
     */
    public TokenResponse refreshTokens(String refreshToken) {
        Claims claims = jwtTokenProvider.validateRefreshToken(refreshToken);
        String tokenId = jwtTokenProvider.getTokenIdFromRefreshToken(refreshToken);
        Long userId = Long.parseLong(claims.getSubject());

        // Redis에서 토큰 조회
        RefreshTokenRedis stored = refreshTokenRedisRepository.findById(tokenId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_EXIST_REFRESH_TOKEN));

        // 토큰 소유자 검증
        if (!stored.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 사용자 조회
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(UserNotFoundException::new);

        // 기존 토큰 삭제 (RTR: 한 번 사용한 토큰은 즉시 무효화)
        refreshTokenRedisRepository.deleteById(tokenId);

        // 새 토큰 발급
        return createTokenPair(user);
    }

    /**
     * 로그아웃
     * - RefreshToken 삭제
     */
    public void logout(String refreshToken) {
        String tokenId = jwtTokenProvider.getTokenIdFromRefreshToken(refreshToken);

        RefreshTokenRedis stored = refreshTokenRedisRepository.findById(tokenId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

        // Redis에서 토큰 삭제
        refreshTokenRedisRepository.deleteById(tokenId);

        // 푸시 토큰도 삭제
        userPushTokenRepository.deleteById(stored.getUserId());
    }

    /**
     * 사용자의 모든 토큰 무효화 (회원 탈퇴 시 호출)
     * - Redis에서 해당 사용자의 모든 RefreshToken 즉시 삭제
     */
    public void revokeAllUserTokens(Long userId) {
        List<RefreshTokenRedis> tokens = refreshTokenRedisRepository.findAllByUserId(userId);
        if (!tokens.isEmpty()) {
            refreshTokenRedisRepository.deleteAll(tokens);
            log.info("사용자 {}의 RefreshToken {}개 삭제 완료", userId, tokens.size());
        }
    }

    /**
     * 오버로드: User 객체로 호출 시
     */
    public void revokeAllUserTokens(User user) {
        revokeAllUserTokens(user.getId());
    }

    private TokenResponse createTokenPair(User user) {
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getNickname());

        String tokenId = TsidCreator.getTsid().toString();
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), tokenId);

        saveRefreshToken(tokenId, user.getId());

        return new TokenResponse(accessToken, refreshToken);
    }

    /**
     * Redis에 RefreshToken 저장
     * - TTL 설정으로 만료 시 자동 삭제
     */
    private void saveRefreshToken(String tokenId, Long userId) {
        RefreshTokenRedis token = RefreshTokenRedis.builder()
                .tokenId(tokenId)
                .userId(userId)
                .ttl(jwtTokenProvider.getRefreshExpSeconds())
                .build();

        refreshTokenRedisRepository.save(token);
        log.debug("RefreshToken 저장 완료: userId={}, ttl={}초", userId, jwtTokenProvider.getRefreshExpSeconds());
    }
}
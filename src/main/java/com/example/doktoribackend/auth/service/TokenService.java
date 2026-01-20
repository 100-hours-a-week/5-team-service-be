package com.example.doktoribackend.auth.service;

import com.example.doktoribackend.auth.domain.RefreshToken;
import com.example.doktoribackend.auth.repository.RefreshTokenRepository;
import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.exception.BusinessException;
import com.example.doktoribackend.exception.UserNotFoundException;
import com.example.doktoribackend.auth.dto.TokenResponse;
import com.example.doktoribackend.security.jwt.JwtTokenProvider;
import com.example.doktoribackend.user.domain.User;
import com.example.doktoribackend.user.repository.UserRepository;
import com.github.f4b6a3.tsid.TsidCreator;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Transactional
    public TokenResponse issueTokens(User user) {
        revokeAllUserTokens(user.getId());
        return createTokenPair(user);
    }

    @Transactional
    public TokenResponse refreshTokens(String refreshToken) {
        try {
            Claims claims = jwtTokenProvider.validateRefreshToken(refreshToken);
            String tokenId = jwtTokenProvider.getTokenIdFromRefreshToken(refreshToken);
            Long userId = Long.parseLong(claims.getSubject());

            RefreshToken stored = refreshTokenRepository.findById(tokenId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

            validateRefreshToken(stored, userId);

            User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                    .orElseThrow(UserNotFoundException::new);
            stored.revoke();
            return createTokenPair(user);

        } catch (OptimisticLockingFailureException e) {
            log.warn("Concurrent refresh token request detected");
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_EXPIRED);
        }
    }


    @Transactional
    public void logout(String refreshToken) {
        try {
            String tokenId = jwtTokenProvider.getTokenIdFromRefreshToken(refreshToken);

            RefreshToken stored = refreshTokenRepository.findById(tokenId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

            if (!stored.isRevoked()) {
                stored.revoke();
            }
        } catch (OptimisticLockingFailureException e) {
            log.debug("Token already revoked during logout");
        }
    }

    @Transactional
    public void revokeAllUserTokens(Long userId) {
        List<RefreshToken> tokens = refreshTokenRepository
                .findAllByUserIdAndRevokedFalse(userId);

        if (!tokens.isEmpty()) {
            tokens.forEach(RefreshToken::revoke);
        }
    }

    private void validateRefreshToken(RefreshToken stored, Long userId) {
        if (stored.isRevoked()) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN_REUSE_DETECTED);
        }
        if (stored.isExpired()) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_EXPIRED);
        }
        if (!stored.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
    }

    private TokenResponse createTokenPair(User user) {
        String accessToken = jwtTokenProvider.createAccessToken(user);

        String tokenId = TsidCreator.getTsid().toString();
        String refreshToken = jwtTokenProvider.createRefreshToken(user, tokenId);

        saveRefreshToken(tokenId, user.getId());

        return new TokenResponse(accessToken, refreshToken);
    }

    private void saveRefreshToken(String tokenId, Long userId) {
        LocalDateTime expiresAt = LocalDateTime.now()
                .plusSeconds(jwtTokenProvider.getRefreshExpSeconds());

        RefreshToken entity = RefreshToken.builder()
                .tokenId(tokenId)
                .userId(userId)
                .expiresAt(expiresAt)
                .build();

        refreshTokenRepository.save(entity);
    }
}
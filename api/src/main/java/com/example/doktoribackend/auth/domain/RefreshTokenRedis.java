package com.example.doktoribackend.auth.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.redis.core.index.Indexed;

import java.util.concurrent.TimeUnit;

/**
 * Redis에 저장되는 RefreshToken
 * Key: refreshToken:{tokenId}
 * TTL: refresh token 만료 시간과 동일
 */
@Getter
@NoArgsConstructor
@RedisHash("refreshToken")
public class RefreshTokenRedis {

    @Id
    private String tokenId;

    @Indexed
    private Long userId;

    @TimeToLive(unit = TimeUnit.SECONDS)
    private Long ttl;

    @Builder
    public RefreshTokenRedis(String tokenId, Long userId, Long ttl) {
        this.tokenId = tokenId;
        this.userId = userId;
        this.ttl = ttl;
    }

    /**
     * 토큰이 유효한지 확인 (Redis에 존재하면 유효)
     * TTL이 만료되면 자동 삭제되므로 별도 만료 체크 불필요
     */
    public boolean isValid() {
        return this.ttl != null && this.ttl > 0;
    }
}
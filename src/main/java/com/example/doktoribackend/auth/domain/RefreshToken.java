package com.example.doktoribackend.auth.domain;

import com.example.doktoribackend.common.domain.BaseTimeEntity;
import com.example.doktoribackend.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends BaseTimeEntity {

    @Id
    @Column(name = "token_id", length = 13, nullable = false, updatable = false)
    private String tokenId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean revoked = false;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @Builder
    public RefreshToken(String tokenId, User user, LocalDateTime expiresAt) {
        this.tokenId = tokenId;
        this.user = user;
        this.expiresAt = expiresAt;
        this.revoked = false;
    }
    public void revoke() {
        this.revoked = true;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt)
                || LocalDateTime.now().isEqual(expiresAt);
    }

    public boolean isValid() {
        return !revoked && !isExpired();
    }

    public Long getUserId() {
        return user != null ? user.getId() : null;
    }
}
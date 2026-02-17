package com.example.doktoribackend.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "users")
@Immutable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserInfo {

    @Id
    private Long id;

    @Column(nullable = false, length = 20)
    private String nickname;

    @Column(name = "profile_image_path", length = 512)
    private String profileImagePath;
}

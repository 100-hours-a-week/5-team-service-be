package com.example.doktoribackend.user.policy;

import com.example.doktoribackend.common.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "reading_purposes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReadingPurpose extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name= "priority", nullable = false)
    private Integer priority;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}

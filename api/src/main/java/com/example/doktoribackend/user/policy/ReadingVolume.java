package com.example.doktoribackend.user.policy;

import com.example.doktoribackend.common.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "reading_volumes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReadingVolume extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name= "priority", nullable = false, columnDefinition = "TINYINT")
    private Integer priority;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}

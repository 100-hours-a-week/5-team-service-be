package com.example.doktoribackend.reading.domain;

import com.example.doktoribackend.common.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "reading_genres", indexes = {
        @Index(name = "idx_reading_genre_deleted_priority", columnList = "deleted_at,priority")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReadingGenre extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20, unique = true)
    private String code;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name= "priority", nullable = false, columnDefinition = "TINYINT")
    private Byte priority;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}

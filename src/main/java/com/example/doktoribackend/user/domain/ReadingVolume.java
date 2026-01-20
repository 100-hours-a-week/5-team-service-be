package com.example.doktoribackend.user.domain;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "reading_volume")
@Getter
public class ReadingVolume {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String code;

    private String name;
}

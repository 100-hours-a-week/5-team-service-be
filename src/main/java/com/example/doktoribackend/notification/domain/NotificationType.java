package com.example.doktoribackend.notification.domain;

import com.example.doktoribackend.common.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_types")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationType extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 50)
    private NotificationTypeCode code;

    @Column(nullable = false, length = 80)
    private String title;

    @Column(name = "message_template", nullable = false, length = 300)
    private String messageTemplate;

    @Column(name = "link_template", length = 255)
    private String linkTemplate;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public NotificationType(NotificationTypeCode code, String title, String messageTemplate, String linkTemplate) {
        this.code = code;
        this.title = title;
        this.messageTemplate = messageTemplate;
        this.linkTemplate = linkTemplate;
    }
}

package com.example.doktoribackend.analytics.domain;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Builder
@Document(collection = "user_behavior_logs")
public class UserBehaviorLog {

    @Id
    private String id;

    private Long userId;
    private String sessionId;
    private Long meetingId;

    private Integer impressionCount;
    private Integer detailClickCount;
    private Long detailDwellTimeMs;

    private Boolean hasJoinRequest;

    private Instant sentAt;
    private Instant createdAt;
}

package com.example.doktoribackend.meeting.domain;

import com.example.doktoribackend.common.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "meeting_round_discussion_topics",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_meeting_round_topic_no",
                columnNames = {"meeting_round_id", "topic_no"}
        ),
        indexes = @Index(name = "idx_meeting_round_topic", columnList = "meeting_round_id, topic_no")
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class MeetingRoundDiscussionTopic extends BaseTimeEntity {

    private static final int MAX_TOPIC_LENGTH = 120;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_round_id", nullable = false)
    private MeetingRound meetingRound;

    @Column(name = "topic_no", nullable = false, columnDefinition = "TINYINT UNSIGNED")
    private Integer topicNo;

    @Column(nullable = false, length = 120)
    private String topic;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TopicSource source;

    public static MeetingRoundDiscussionTopic create(
            MeetingRound meetingRound,
            Integer topicNo,
            String topic,
            TopicSource source
    ) {
        String truncatedTopic = truncateIfNeeded(topic);
        return MeetingRoundDiscussionTopic.builder()
                .meetingRound(meetingRound)
                .topicNo(topicNo)
                .topic(truncatedTopic)
                .source(source)
                .build();
    }

    public void update(String topic, TopicSource source) {
        this.topic = truncateIfNeeded(topic);
        this.source = source;
    }

    private static String truncateIfNeeded(String topic) {
        if (topic != null && topic.length() > MAX_TOPIC_LENGTH) {
            return topic.substring(0, MAX_TOPIC_LENGTH);
        }
        return topic;
    }
}

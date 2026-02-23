package com.example.doktoribackend.meeting.repository;

import com.example.doktoribackend.meeting.domain.MeetingRoundDiscussionTopic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MeetingRoundDiscussionTopicRepository extends JpaRepository<MeetingRoundDiscussionTopic, Long> {

    @Query("SELECT t FROM MeetingRoundDiscussionTopic t " +
            "WHERE t.meetingRound.id = :meetingRoundId " +
            "AND t.topicNo = :topicNo")
    Optional<MeetingRoundDiscussionTopic> findByMeetingRoundIdAndTopicNo(
            @Param("meetingRoundId") Long meetingRoundId,
            @Param("topicNo") Integer topicNo
    );
}

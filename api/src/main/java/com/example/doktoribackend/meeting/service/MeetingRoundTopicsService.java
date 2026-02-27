package com.example.doktoribackend.meeting.service;

import com.example.doktoribackend.meeting.dto.UpdateTopicsRequest;
import com.example.doktoribackend.meeting.dto.UpdateTopicsResponse;
import com.example.doktoribackend.meeting.repository.MeetingRoundDiscussionTopicRepository;
import com.example.doktoribackend.meeting.repository.MeetingRoundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MeetingRoundTopicsService {

    private final MeetingRoundRepository meetingRoundRepository;
    private final MeetingRoundDiscussionTopicRepository discussionTopicRepository;

    @Transactional
    public UpdateTopicsResponse updateTopics(Long userId, Long roundId, UpdateTopicsRequest request) {
        // TODO: 커밋 2에서 구현
        throw new UnsupportedOperationException("Not implemented yet");
    }
}

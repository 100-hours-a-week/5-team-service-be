package com.example.doktoribackend.meeting.service;

import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.exception.BusinessException;
import com.example.doktoribackend.meeting.domain.MeetingRound;
import com.example.doktoribackend.meeting.domain.MeetingRoundDiscussionTopic;
import com.example.doktoribackend.meeting.domain.TopicSource;
import com.example.doktoribackend.meeting.dto.UpdateTopicsRequest;
import com.example.doktoribackend.meeting.dto.UpdateTopicsResponse;
import com.example.doktoribackend.meeting.repository.MeetingRoundDiscussionTopicRepository;
import com.example.doktoribackend.meeting.repository.MeetingRoundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MeetingRoundTopicsService {

    private static final Set<Integer> REQUIRED_TOPIC_NOS = Set.of(1, 2, 3);

    private final MeetingRoundRepository meetingRoundRepository;
    private final MeetingRoundDiscussionTopicRepository discussionTopicRepository;

    @Transactional
    public UpdateTopicsResponse updateTopics(Long userId, Long roundId, UpdateTopicsRequest request) {
        // 1. 회차 조회 (Meeting 포함)
        MeetingRound meetingRound = meetingRoundRepository.findByIdWithBookAndMeeting(roundId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROUND_NOT_FOUND));

        // 2. 권한 체크 (모임장만)
        if (!meetingRound.getMeeting().isLeader(userId)) {
            throw new BusinessException(ErrorCode.TOPIC_UPDATE_FORBIDDEN);
        }

        // 3. topicNo 중복 체크
        Set<Integer> topicNos = new HashSet<>();
        for (UpdateTopicsRequest.TopicItem item : request.topics()) {
            if (!topicNos.add(item.topicNo())) {
                throw new BusinessException(ErrorCode.DUPLICATE_TOPIC_NO);
            }
        }

        // 4. topicNo 1, 2, 3 모두 있는지 체크
        if (!topicNos.equals(REQUIRED_TOPIC_NOS)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        // 5. 기존 topics 삭제
        discussionTopicRepository.deleteByMeetingRoundId(roundId);

        // 6. 새 topics 저장
        List<MeetingRoundDiscussionTopic> newTopics = request.topics().stream()
                .map(item -> MeetingRoundDiscussionTopic.create(
                        meetingRound,
                        item.topicNo(),
                        item.topic(),
                        TopicSource.valueOf(item.source())
                ))
                .toList();
        discussionTopicRepository.saveAll(newTopics);

        // 7. 응답 생성
        List<UpdateTopicsResponse.TopicItem> responseItems = newTopics.stream()
                .sorted((a, b) -> a.getTopicNo().compareTo(b.getTopicNo()))
                .map(t -> UpdateTopicsResponse.TopicItem.builder()
                        .topicNo(t.getTopicNo())
                        .topic(t.getTopic())
                        .source(t.getSource().name())
                        .build())
                .toList();

        return UpdateTopicsResponse.builder()
                .topics(responseItems)
                .build();
    }
}

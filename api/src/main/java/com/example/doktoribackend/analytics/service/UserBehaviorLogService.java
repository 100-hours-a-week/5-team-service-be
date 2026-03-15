package com.example.doktoribackend.analytics.service;

import com.example.doktoribackend.analytics.domain.UserBehaviorLog;
import com.example.doktoribackend.analytics.dto.BehaviorLogRequest;
import com.example.doktoribackend.analytics.repository.UserBehaviorLogRepository;
import com.example.doktoribackend.meeting.repository.MeetingMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserBehaviorLogService {

    private final UserBehaviorLogRepository userBehaviorLogRepository;
    private final MeetingMemberRepository meetingMemberRepository;

    public void saveBehaviorLogs(Long userId, BehaviorLogRequest request) {
        List<Long> meetingIds = request.getItems().stream()
                .map(BehaviorLogRequest.BehaviorLogItem::getMeetingId)
                .toList();

        Set<Long> joinedMeetingIds = meetingMemberRepository
                .findMeetingIdsByUserIdAndMeetingIds(userId, meetingIds);

        List<UserBehaviorLog> logs = request.getItems().stream()
                .map(item -> UserBehaviorLog.builder()
                        .userId(userId)
                        .sessionId(request.getSessionId())
                        .meetingId(item.getMeetingId())
                        .impressionCount(item.getImpressionCount())
                        .detailClickCount(item.getDetailClickCount())
                        .detailDwellTimeMs(item.getDetailDwellTimeMs())
                        .hasJoinRequest(joinedMeetingIds.contains(item.getMeetingId()))
                        .sentAt(request.getSentAt())
                        .createdAt(Instant.now())
                        .build())
                .collect(Collectors.toList());

        userBehaviorLogRepository.saveAll(logs);
        log.info("Saved {} behavior logs for userId: {}", logs.size(), userId);
    }
}

package com.example.doktoribackend.notification.service;

import com.example.doktoribackend.meeting.domain.MeetingRound;
import com.example.doktoribackend.meeting.repository.MeetingMemberRepository;
import com.example.doktoribackend.meeting.repository.MeetingRoundRepository;
import com.example.doktoribackend.notification.domain.NotificationTypeCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationSchedulerService {

    private final MeetingRoundRepository meetingRoundRepository;
    private final MeetingMemberRepository meetingMemberRepository;
    private final NotificationService notificationService;

    private static final String PARAM_MEETING_ID = "meetingId";

    @Scheduled(cron = "0 0 * * * *")
    public void sendReviewDeadline24hNotifications() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);
        LocalDateTime from = now.plusHours(47);
        LocalDateTime to = now.plusHours(48);

        log.info("[Scheduler] 독후감 마감 24시간 전 알림 스케줄러 시작 - 실행시간: {}, 대상 범위: {} ~ {}",
                now, from, to);

        List<MeetingRound> rounds = meetingRoundRepository.findRoundsStartingBetween(from, to);

        if (rounds.isEmpty()) {
            return;
        }

        int totalSent = 0;
        for (MeetingRound round : rounds) {
            int sentCount = sendNotificationToMembers(
                    round,
                    NotificationTypeCode.BOOK_REPORT_DEADLINE_24H_BEFORE,
                    Map.of(PARAM_MEETING_ID, round.getMeeting().getId().toString(),
                            "meetingTitle", round.getMeeting().getTitle())
            );
            totalSent += sentCount;

            log.info("[Scheduler] 독후감 마감 24시간 전 알림 발송 - MeetingRoundId: {}, 대상 인원: {}명",
                    round.getId(), sentCount);
        }

        log.info("[Scheduler] 독후감 마감 24시간 전 알림 완료 - 총 대상 회차: {}개, 총 알림 발송: {}명",
                rounds.size(), totalSent);
    }

    @Scheduled(cron = "0 0,30 * * * *")
    public void sendReviewDeadline30mNotifications() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        LocalDateTime from = now.plusHours(24);
        LocalDateTime to = now.plusHours(24).plusMinutes(30);

        log.info("[Scheduler] 독후감 마감 30분 전 알림 스케줄러 시작 - 실행시간: {}, 대상 범위: {} ~ {}",
                now, from, to);

        List<MeetingRound> rounds = meetingRoundRepository.findRoundsStartingBetween(from, to);

        if (rounds.isEmpty()) {
            return;
        }

        int totalSent = 0;
        for (MeetingRound round : rounds) {
            int sentCount = sendNotificationToMembers(
                    round,
                    NotificationTypeCode.BOOK_REPORT_DEADLINE_30M_BEFORE,
                    Map.of(PARAM_MEETING_ID, round.getMeeting().getId().toString(),
                            "meetingTitle", round.getMeeting().getTitle())
            );
            totalSent += sentCount;

            log.info("[Scheduler] 독후감 마감 30분 전 알림 발송 - MeetingRoundId: {}, 대상 인원: {}명",
                    round.getId(), sentCount);
        }

        log.info("[Scheduler] 독후감 마감 30분 전 알림 완료 - 총 대상 회차: {}개, 총 알림 발송: {}명",
                rounds.size(), totalSent);
    }

    private int sendNotificationToMembers(
            MeetingRound round,
            NotificationTypeCode typeCode,
            Map<String, String> parameters
    ) {
        Long meetingId = round.getMeeting().getId();
        List<Long> memberUserIds = meetingMemberRepository.findApprovedMemberUserIds(meetingId);

        if (!memberUserIds.isEmpty()) {
            notificationService.createAndSendBatch(memberUserIds, typeCode, parameters);
        }

        return memberUserIds.size();
    }
}

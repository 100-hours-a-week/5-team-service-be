package com.example.doktoribackend.zoom.service;

import com.example.doktoribackend.meeting.domain.MeetingRound;
import com.example.doktoribackend.meeting.domain.MeetingStatus;
import com.example.doktoribackend.meeting.repository.MeetingMemberRepository;
import com.example.doktoribackend.meeting.repository.MeetingRoundRepository;
import com.example.doktoribackend.notification.domain.NotificationTypeCode;
import com.example.doktoribackend.notification.service.NotificationService;
import com.example.doktoribackend.zoom.exception.ZoomAuthenticationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ZoomLinkSchedulerService {

    private final MeetingRoundRepository meetingRoundRepository;
    private final MeetingMemberRepository meetingMemberRepository;
    private final ZoomService zoomService;
    private final ZoomLinkUpdateService zoomLinkUpdateService;
    private final NotificationService notificationService;

    private static final int BATCH_SIZE = 10;
    private static final long RATE_LIMIT_DELAY_MS = 100L;
    private static final int MEETING_DURATION_MINUTES = 30;
    private static final long MAX_EXECUTION_SECONDS = 50L;
    private static final int LOOK_AHEAD_MINUTES = 10;

    private static final List<MeetingStatus> TARGET_STATUSES = Arrays.asList(
            MeetingStatus.FINISHED,
            MeetingStatus.RECRUITING
    );

    @Scheduled(cron = "0 * * * * *")
    public void createZoomLinksForUpcomingMeetings() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        LocalDateTime targetTime = now.plusMinutes(LOOK_AHEAD_MINUTES);

        log.info("[Scheduler] Zoom 링크 생성 스케줄러 시작 - 실행시간: {}, 대상 범위: {} ~ {}",
                now, now, targetTime);

        long startTimeMillis = System.currentTimeMillis();

        try {
            processBatchZoomLinkCreation(now, targetTime, startTimeMillis);
        } catch (ZoomAuthenticationException e) {
            log.error("[Scheduler] Zoom 인증 실패로 스케줄러 중단 - Error: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("[Scheduler] 스케줄러 실행 중 예상치 못한 오류 발생", e);
        }
    }

    private void processBatchZoomLinkCreation(LocalDateTime now, LocalDateTime targetTime, long startTimeMillis) {
        List<Long> meetingRoundIds = meetingRoundRepository.findMeetingRoundIdsForZoomLinkCreation(
                TARGET_STATUSES, now, targetTime);

        if (meetingRoundIds.isEmpty()) {
            return;
        }

        int totalCount = meetingRoundIds.size();
        int successCount = 0;
        int failCount = 0;
        int processedCount = 0;

        for (int i = 0; i < meetingRoundIds.size(); i += BATCH_SIZE) {
            long elapsedSeconds = Duration.ofMillis(System.currentTimeMillis() - startTimeMillis).toSeconds();
            if (elapsedSeconds >= MAX_EXECUTION_SECONDS) {
                log.warn("[Scheduler] 실행 시간 {}초 초과로 중단 - 처리: {}/{}, 성공: {}, 실패: {}",
                        MAX_EXECUTION_SECONDS, processedCount, totalCount, successCount, failCount);
                break;
            }

            int endIndex = Math.min(i + BATCH_SIZE, meetingRoundIds.size());
            List<Long> batchIds = meetingRoundIds.subList(i, endIndex);

            List<MeetingRound> batch = meetingRoundRepository.findByIdsWithMeeting(batchIds);

            for (MeetingRound meetingRound : batch) {
                boolean success = processZoomLinkCreation(meetingRound);
                processedCount++;

                if (success) {
                    successCount++;
                } else {
                    failCount++;
                }
                sleep();
            }
        }

        long totalDurationMs = System.currentTimeMillis() - startTimeMillis;
        log.info("[Scheduler] 배치 처리 완료 - Total: {}, Success: {}, Failed: {}, Duration: {}ms",
                totalCount, successCount, failCount, totalDurationMs);
    }

    private boolean processZoomLinkCreation(MeetingRound meetingRound) {
        Long meetingRoundId = meetingRound.getId();
        Long meetingId = meetingRound.getMeeting().getId();
        LocalDateTime startAt = meetingRound.getStartAt();
        String meetingTitle = meetingRound.getMeeting().getTitle();

        if (meetingRound.getMeetingLink() != null) {
            return true;
        }

        if (startAt.isBefore(LocalDateTime.now())) {
            return false;
        }

        String topic = String.format("%s - %d회차", meetingTitle, meetingRound.getRoundNo());

        try {
            String joinUrl = zoomService.createMeeting(topic, startAt, MEETING_DURATION_MINUTES);
            zoomLinkUpdateService.saveMeetingLink(meetingRoundId, joinUrl);
            sendRoundStartNotification(meetingRound);
            return true;
        } catch (ZoomAuthenticationException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[Scheduler] Zoom 링크 생성 실패 (다음 실행에서 재시도) - MeetingRoundId: {}, MeetingId: {}, StartAt: {}, Error: {}",
                    meetingRoundId, meetingId, startAt, e.getMessage());
            return false;
        }
    }

    private void sendRoundStartNotification(MeetingRound meetingRound) {
        try {
            Long meetingId = meetingRound.getMeeting().getId();
            List<Long> memberUserIds = meetingMemberRepository.findApprovedMemberUserIds(meetingId);

            if (!memberUserIds.isEmpty()) {
                notificationService.createAndSendBatch(
                        memberUserIds,
                        NotificationTypeCode.ROUND_START_10M_BEFORE,
                        Map.of("meetingId", meetingId.toString(),
                                "meetingTitle", meetingRound.getMeeting().getTitle())
                );
                log.info("[Scheduler] 토론 시작 알림 발송 - MeetingRoundId: {}, 대상 인원: {}명",
                        meetingRound.getId(), memberUserIds.size());
            }
        } catch (Exception e) {
            log.error("[Scheduler] 토론 시작 알림 발송 실패 - MeetingRoundId: {}, Error: {}",
                    meetingRound.getId(), e.getMessage());
        }
    }

    private void sleep() {
        try {
            Thread.sleep(RATE_LIMIT_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[Scheduler] Thread interrupted during sleep");
        }
    }
}

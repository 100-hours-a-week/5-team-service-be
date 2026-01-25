package com.example.doktoribackend.zoom.service;

import com.example.doktoribackend.meeting.domain.MeetingRound;
import com.example.doktoribackend.meeting.domain.MeetingStatus;
import com.example.doktoribackend.meeting.repository.MeetingRoundRepository;
import com.example.doktoribackend.zoom.exception.ZoomAuthenticationException;
import com.example.doktoribackend.zoom.exception.ZoomRetryableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ZoomLinkSchedulerService {

    private final MeetingRoundRepository meetingRoundRepository;
    private final ZoomService zoomService;
    private final ZoomLinkUpdateService zoomLinkUpdateService;

    private static final int BATCH_SIZE = 10;
    private static final int MAX_RETRY_COUNT = 3;
    private static final long BASE_RETRY_DELAY_MS = 30_000L;
    private static final long RATE_LIMIT_DELAY_MS = 100L;
    private static final int MEETING_DURATION_MINUTES = 30;
    private static final long MAX_EXECUTION_MINUTES = 8L;

    private static final List<MeetingStatus> TARGET_STATUSES = Arrays.asList(
            MeetingStatus.FINISHED,
            MeetingStatus.RECRUITING
    );

    @Scheduled(cron = "0 20,50 * * * *")
    public void createZoomLinksForUpcomingMeetings() {
        LocalDateTime executionTime = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        LocalDateTime targetTime = executionTime.plusMinutes(10);

        log.info("[Scheduler] Zoom 링크 생성 스케줄러 시작 - 실행시간: {}, 대상 시작시간: {}",
                executionTime, targetTime);

        long startTimeMillis = System.currentTimeMillis();

        try {
            processBatchZoomLinkCreation(targetTime, startTimeMillis);
        } catch (ZoomAuthenticationException e) {
            log.error("[Scheduler] Zoom 인증 실패로 스케줄러 중단 - Error: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("[Scheduler] 스케줄러 실행 중 예상치 못한 오류 발생", e);
        }
    }

    private void processBatchZoomLinkCreation(LocalDateTime targetTime, long startTimeMillis) {
        List<Long> meetingRoundIds = meetingRoundRepository.findMeetingRoundIdsForZoomLinkCreation(
                TARGET_STATUSES, targetTime);

        if (meetingRoundIds.isEmpty()) {
            log.info("[Scheduler] 대상 MeetingRound 없음 - 대상 시작시간: {}", targetTime);
            return;
        }

        int totalCount = meetingRoundIds.size();
        int successCount = 0;
        int failCount = 0;
        int processedCount = 0;

        for (int i = 0; i < meetingRoundIds.size(); i += BATCH_SIZE) {
            long elapsedMinutes = Duration.ofMillis(System.currentTimeMillis() - startTimeMillis).toMinutes();
            if (elapsedMinutes >= MAX_EXECUTION_MINUTES) {
                log.error("[Scheduler] 실행 시간 {}분 초과로 강제 종료 - 처리: {}/{}, 성공: {}, 실패: {}",
                        MAX_EXECUTION_MINUTES, processedCount, totalCount, successCount, failCount);
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
                sleep(RATE_LIMIT_DELAY_MS);
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
        String lastError = null;
        int attemptCount = 0;

        for (int retry = 0; retry < MAX_RETRY_COUNT; retry++) {
            attemptCount = retry + 1;

            try {
                String joinUrl = zoomService.createMeeting(topic, startAt, MEETING_DURATION_MINUTES);
                zoomLinkUpdateService.saveMeetingLink(meetingRoundId, joinUrl);
                return true;

            } catch (ZoomAuthenticationException e) {
                throw e;
            } catch (ZoomRetryableException e) {
                lastError = e.getMessage();
                log.warn("[Scheduler] Zoom 링크 생성 실패 - MeetingRoundId: {}, Retry: {}/{}, Error: {}",
                        meetingRoundId, attemptCount, MAX_RETRY_COUNT, e.getMessage());

                if (retry < MAX_RETRY_COUNT - 1) {
                    sleep(calculateRetryDelay(retry));
                }
            } catch (Exception e) {
                lastError = e.getMessage();
                log.error("[Scheduler] Zoom 링크 생성 실패 - MeetingRoundId: {}, Retry: {}/{}, Error: {}",
                        meetingRoundId, attemptCount, MAX_RETRY_COUNT, e.getMessage(), e);

                if (retry < MAX_RETRY_COUNT - 1) {
                    sleep(calculateRetryDelay(retry));
                }
            }
        }
        log.error("[ERROR] Zoom 링크 생성 최종 실패 - MeetingRoundId: {}, MeetingId: {}, StartAt: {}, Reason: {}, Retry: {}/{}",
                meetingRoundId, meetingId, startAt, lastError, attemptCount, MAX_RETRY_COUNT);

        return false;
    }

    private long calculateRetryDelay(int retry) {
        return BASE_RETRY_DELAY_MS * (1L << retry);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[Scheduler] Thread interrupted during sleep");
        }
    }
}

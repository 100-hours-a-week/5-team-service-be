package com.example.doktoribackend.scheduler;

import com.example.doktoribackend.meeting.domain.MeetingRound;
import com.example.doktoribackend.meeting.repository.MeetingMemberRepository;
import com.example.doktoribackend.meeting.repository.MeetingRepository;
import com.example.doktoribackend.meeting.repository.MeetingRoundRepository;
import com.example.doktoribackend.notification.domain.NotificationTypeCode;
import com.example.doktoribackend.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class MeetingScheduler {

    private static final int BATCH_SIZE = 100;

    private final MeetingRepository meetingRepository;
    private final MeetingRoundRepository meetingRoundRepository;
    private final MeetingMemberRepository meetingMemberRepository;
    private final NotificationService notificationService;

    /**
     * 매일 자정에 모집 마감일이 지난 모임들의 상태를 FINISHED로 변경
     * cron: 초 분 시 일 월 요일
     * "0 0 0 * * *" = 매일 00:00:00
     */
    @Scheduled(cron = "0 0 0 * * *")
    @SchedulerLock(name = "updateExpiredRecruitmentStatus", lockAtMostFor = "23h")
    @Transactional
    public void updateExpiredRecruitmentStatus() {
        LocalDate today = LocalDate.now();

        int updatedCount = meetingRepository.bulkUpdateExpiredToFinished(today);

        log.info("모집 마감일이 지난 {} 개의 모임 상태를 FINISHED로 업데이트했습니다.", updatedCount);
    }

    /**
     * 5분마다 종료 시간이 지난 회차의 상태를 DONE으로 변경하고 후기 작성 알림 발송
     */
    @Scheduled(cron = "0 */5 * * * *")
    @SchedulerLock(name = "completeExpiredRounds", lockAtMostFor = "4m")
    @Transactional
    public void completeExpiredRounds() {
        LocalDateTime now = LocalDateTime.now();
        int totalProcessed = 0;

        List<MeetingRound> batch;
        do {
            batch = meetingRoundRepository.findExpiredScheduledRounds(now, PageRequest.of(0, BATCH_SIZE));

            for (MeetingRound round : batch) {
                round.complete();

                List<Long> memberUserIds = meetingMemberRepository.findApprovedMemberUserIds(
                        round.getMeeting().getId()
                );

                if (!memberUserIds.isEmpty()) {
                    notificationService.createAndSendBatch(
                            memberUserIds,
                            NotificationTypeCode.ROUND_COMPLETED_REVIEW_REQUEST,
                            Map.of(
                                    "meetingTitle", round.getMeeting().getTitle(),
                                    "roundNo", String.valueOf(round.getRoundNo()),
                                    "meetingId", String.valueOf(round.getMeeting().getId()),
                                    "roundId", String.valueOf(round.getId())
                            )
                    );
                }
            }

            totalProcessed += batch.size();
        } while (batch.size() == BATCH_SIZE);

        if (totalProcessed > 0) {
            log.info("종료 시간이 지난 {} 개의 회차 상태를 DONE으로 업데이트하고 후기 알림을 발송했습니다.", totalProcessed);
        }
    }
}

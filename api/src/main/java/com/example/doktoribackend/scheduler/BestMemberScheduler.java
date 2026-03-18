package com.example.doktoribackend.scheduler;

import com.example.doktoribackend.meeting.domain.MeetingRound;
import com.example.doktoribackend.meeting.repository.MeetingRoundRepository;
import com.example.doktoribackend.notification.domain.NotificationTypeCode;
import com.example.doktoribackend.notification.service.NotificationService;
import com.example.doktoribackend.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class BestMemberScheduler {

    private static final int BATCH_SIZE = 100;

    private final MeetingRoundRepository meetingRoundRepository;
    private final ReviewRepository reviewRepository;
    private final NotificationService notificationService;
    private final TransactionTemplate transactionTemplate;

    /**
     * 5분마다 실행: DONE 상태이고 베스트 모임원 미산정 회차에 대해
     * endAt + 24시간 경과 또는 모든 멤버 후기 작성 완료 시 베스트 모임원 산정
     */
    @Scheduled(cron = "0 */5 * * * *")
    @SchedulerLock(name = "determineBestMember", lockAtMostFor = "4m")
    public void determineBestMember() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);

        processBatches(cutoff, true);
        processBatches(cutoff, false);
    }

    private void processBatches(LocalDateTime cutoff, boolean timeExpired) {
        int processed;
        do {
            processed = processBatch(cutoff, timeExpired);
        } while (processed == BATCH_SIZE);
    }

    private int processBatch(LocalDateTime cutoff, boolean timeExpired) {
        Integer result = transactionTemplate.execute(status -> {
            List<MeetingRound> batch = timeExpired
                    ? meetingRoundRepository.findDoneRoundsEligibleForBestMember(
                            cutoff, PageRequest.of(0, BATCH_SIZE))
                    : meetingRoundRepository.findDoneRoundsWithAllReviewsCompleted(
                            cutoff, PageRequest.of(0, BATCH_SIZE));

            for (MeetingRound round : batch) {
                List<Long> bestMemberIds = reviewRepository.findBestMemberIdsByMeetingRoundId(round.getId());

                if (!bestMemberIds.isEmpty()) {
                    notificationService.createAndSendBatch(
                            bestMemberIds,
                            NotificationTypeCode.BEST_MEMBER_SELECTED,
                            Map.of(
                                    "meetingTitle", round.getMeeting().getTitle(),
                                    "roundNo", String.valueOf(round.getRoundNo()),
                                    "meetingId", String.valueOf(round.getMeeting().getId()),
                                    "roundId", String.valueOf(round.getId())
                            )
                    );
                }

                round.markBestMemberDetermined();
                log.info("회차 {}의 베스트 모임원 산정 완료. 선정된 멤버: {}", round.getId(), bestMemberIds);
            }

            return batch.size();
        });

        return result != null ? result : 0;
    }
}

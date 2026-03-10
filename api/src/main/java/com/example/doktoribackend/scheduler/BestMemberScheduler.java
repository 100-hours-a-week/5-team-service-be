package com.example.doktoribackend.scheduler;

import com.example.doktoribackend.meeting.domain.MeetingRound;
import com.example.doktoribackend.meeting.repository.MeetingMemberRepository;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class BestMemberScheduler {

    private static final int BATCH_SIZE = 100;

    private final MeetingRoundRepository meetingRoundRepository;
    private final MeetingMemberRepository meetingMemberRepository;
    private final ReviewRepository reviewRepository;
    private final NotificationService notificationService;

    /**
     * 5분마다 실행: DONE 상태이고 베스트 모임원 미산정 회차에 대해
     * 모든 멤버 후기 작성 완료 또는 endAt + 24시간 경과 시 베스트 모임원 산정
     */
    @Scheduled(cron = "0 */5 * * * *")
    @SchedulerLock(name = "determineBestMember", lockAtMostFor = "4m")
    @Transactional
    public void determineBestMember() {
        LocalDateTime now = LocalDateTime.now();

        List<MeetingRound> batch;
        do {
            batch = meetingRoundRepository.findDoneRoundsNotBestMemberDetermined(
                    PageRequest.of(0, BATCH_SIZE)
            );

            for (MeetingRound round : batch) {
                if (!shouldDetermineBestMember(round, now)) {
                    continue;
                }

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
        } while (batch.size() == BATCH_SIZE);
    }

    private boolean shouldDetermineBestMember(MeetingRound round, LocalDateTime now) {
        if (now.isAfter(round.getEndAt().plusHours(24))) {
            return true;
        }
        List<Long> approvedMemberIds = meetingMemberRepository.findApprovedMemberUserIds(
                round.getMeeting().getId()
        );
        long reviewCount = reviewRepository.countByMeetingRoundIdAndDeletedAtIsNull(round.getId());

        return !approvedMemberIds.isEmpty() && reviewCount >= approvedMemberIds.size();
    }
}

package com.example.doktoribackend.scheduler;

import com.example.doktoribackend.meeting.domain.Meeting;
import com.example.doktoribackend.meeting.domain.MeetingRound;
import com.example.doktoribackend.meeting.domain.MeetingRoundStatus;
import com.example.doktoribackend.meeting.repository.MeetingRepository;
import com.example.doktoribackend.meeting.repository.MeetingRoundRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MeetingScheduler {

    private final MeetingRepository meetingRepository;
    private final MeetingRoundRepository meetingRoundRepository;

    /**
     * 매일 자정에 모집 마감일이 지난 모임들의 상태를 FINISHED로 변경
     * cron: 초 분 시 일 월 요일
     * "0 0 0 * * *" = 매일 00:00:00
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void updateExpiredRecruitmentStatus() {
        LocalDate today = LocalDate.now();

        List<Meeting> expiredMeetings = meetingRepository.findExpiredRecruitingMeetings(today);

        if (expiredMeetings.isEmpty()) {
            log.info("모집 마감일이 지난 모임이 없습니다.");
            return;
        }

        int updatedCount = 0;
        for (Meeting meeting : expiredMeetings) {
            meeting.updateStatusToFinished();
            updatedCount++;
        }

        log.info("모집 마감일이 지난 {} 개의 모임 상태를 FINISHED로 업데이트했습니다.", updatedCount);
    }

    /**
     * 매 시간 정각에 종료 시간이 지난 회차의 상태를 DONE으로 변경
     * "0 0 * * * *" = 매 시간 00분 00초
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void completeExpiredRounds() {
        LocalDateTime now = LocalDateTime.now();

        List<MeetingRound> expiredRounds = meetingRoundRepository.findByStatusAndEndAtBefore(
                MeetingRoundStatus.SCHEDULED, now);

        if (expiredRounds.isEmpty()) {
            return;
        }

        for (MeetingRound round : expiredRounds) {
            round.complete();
        }

        log.info("종료 시간이 지난 {} 개의 회차 상태를 DONE으로 업데이트했습니다.", expiredRounds.size());
    }
}

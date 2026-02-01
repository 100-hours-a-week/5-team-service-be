package com.example.doktoribackend.scheduler;

import com.example.doktoribackend.meeting.domain.Meeting;
import com.example.doktoribackend.meeting.repository.MeetingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MeetingScheduler {

    private final MeetingRepository meetingRepository;

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
}

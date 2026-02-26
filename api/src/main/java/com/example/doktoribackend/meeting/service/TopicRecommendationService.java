package com.example.doktoribackend.meeting.service;

import com.example.doktoribackend.bookReport.domain.BookReport;
import com.example.doktoribackend.bookReport.domain.BookReportStatus;
import com.example.doktoribackend.bookReport.repository.BookReportRepository;
import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.exception.BusinessException;
import com.example.doktoribackend.meeting.domain.Meeting;
import com.example.doktoribackend.meeting.domain.MeetingRound;
import com.example.doktoribackend.meeting.domain.TopicRecommendationLog;
import com.example.doktoribackend.meeting.dto.AiTopicRequest;
import com.example.doktoribackend.meeting.dto.AiTopicResponse;
import com.example.doktoribackend.meeting.dto.TopicRecommendationResponse;
import com.example.doktoribackend.meeting.repository.MeetingRepository;
import com.example.doktoribackend.meeting.repository.MeetingRoundRepository;
import com.example.doktoribackend.meeting.repository.TopicRecommendationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TopicRecommendationService {

    private static final int DAILY_AI_RECOMMENDATION_LIMIT = 15;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final MeetingRepository meetingRepository;
    private final MeetingRoundRepository meetingRoundRepository;
    private final TopicRecommendationLogRepository recommendationLogRepository;
    private final BookReportRepository bookReportRepository;
    private final AiTopicRecommendationClient aiClient;

    @Transactional
    public TopicRecommendationResponse recommendTopic(
            Long userId,
            Long meetingId,
            Integer roundNo
    ) {
        // 1. 모임 조회 (비관적 락으로 동시성 제어)
        Meeting meeting = meetingRepository.findByIdWithLock(meetingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEETING_NOT_FOUND));

        // 2. 권한 체크 (모임장만)
        if (!meeting.isLeader(userId)) {
            throw new BusinessException(ErrorCode.TOPIC_RECOMMENDATION_FORBIDDEN);
        }

        // 3. 회차 조회
        MeetingRound meetingRound = meetingRoundRepository.findByMeetingIdAndRoundNo(meetingId, roundNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROUND_NOT_FOUND));

        // 4. 오늘 날짜 범위 계산 (KST 기준, [startOfDay, nextDayStart) 패턴)
        LocalDate today = LocalDate.now(KST);
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime nextDayStart = startOfDay.plusDays(1);

        // 5. 현재 오늘 사용량 조회
        int todayCount = recommendationLogRepository.countTodayByMeetingId(meetingId, startOfDay, nextDayStart);

        // 6. 일일 제한 체크
        if (todayCount >= DAILY_AI_RECOMMENDATION_LIMIT) {
            throw new BusinessException(ErrorCode.TOPIC_RECOMMENDATION_LIMIT_EXCEEDED);
        }

        // 7. 독후감 조회 (APPROVED 상태만)
        List<BookReport> bookReports = bookReportRepository.findByMeetingRoundId(meetingRound.getId())
                .stream()
                .filter(br -> br.getStatus() == BookReportStatus.APPROVED)
                .toList();

        if (bookReports.isEmpty()) {
            throw new BusinessException(ErrorCode.NO_BOOK_REPORTS_FOR_TOPIC);
        }

        // 8. AI 서버 호출
        AiTopicRequest aiRequest = new AiTopicRequest(
                bookReports.stream()
                        .map(br -> new AiTopicRequest.ReportInfo(br.getId(), br.getContent()))
                        .toList()
        );

        AiTopicResponse aiResponse = aiClient.requestTopicRecommendation(meetingRound.getId(), aiRequest);
        String topicText = aiResponse.data().topic();

        // 9. AI 호출 성공 시 로그 저장
        TopicRecommendationLog log = TopicRecommendationLog.create(meeting);
        recommendationLogRepository.save(log);
        todayCount++;

        // 10. remainingCount 계산
        int remainingCount = Math.max(0, DAILY_AI_RECOMMENDATION_LIMIT - todayCount);

        // 11. 응답 생성 (DB 저장 없이 반환만)
        return TopicRecommendationResponse.builder()
                .meetingId(meetingId)
                .roundNo(roundNo)
                .topic(topicText)
                .remainingCount(remainingCount)
                .build();
    }
}

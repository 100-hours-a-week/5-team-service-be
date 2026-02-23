package com.example.doktoribackend.meeting.service;

import com.example.doktoribackend.bookReport.domain.BookReport;
import com.example.doktoribackend.bookReport.domain.BookReportStatus;
import com.example.doktoribackend.bookReport.repository.BookReportRepository;
import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.exception.BusinessException;
import com.example.doktoribackend.meeting.domain.Meeting;
import com.example.doktoribackend.meeting.domain.MeetingRound;
import com.example.doktoribackend.meeting.domain.MeetingRoundDiscussionTopic;
import com.example.doktoribackend.meeting.domain.TopicRecommendationLog;
import com.example.doktoribackend.meeting.domain.TopicSource;
import com.example.doktoribackend.meeting.dto.AiTopicRequest;
import com.example.doktoribackend.meeting.dto.AiTopicResponse;
import com.example.doktoribackend.meeting.dto.TopicRecommendationRequest;
import com.example.doktoribackend.meeting.dto.TopicRecommendationResponse;
import com.example.doktoribackend.meeting.repository.MeetingRepository;
import com.example.doktoribackend.meeting.repository.MeetingRoundDiscussionTopicRepository;
import com.example.doktoribackend.meeting.repository.MeetingRoundRepository;
import com.example.doktoribackend.meeting.repository.TopicRecommendationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TopicRecommendationService {

    private static final int DAILY_AI_RECOMMENDATION_LIMIT = 15;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final MeetingRepository meetingRepository;
    private final MeetingRoundRepository meetingRoundRepository;
    private final MeetingRoundDiscussionTopicRepository discussionTopicRepository;
    private final TopicRecommendationLogRepository recommendationLogRepository;
    private final BookReportRepository bookReportRepository;
    private final AiTopicRecommendationClient aiClient;

    @Transactional
    public TopicRecommendationResponse recommendTopic(
            Long userId,
            Long meetingId,
            Integer roundNo,
            TopicRecommendationRequest request
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

        String topicText;
        TopicSource source;
        boolean shouldSaveLog = false;

        if (request.isAiMode()) {
            // 6-1. AI 모드: 일일 제한 체크
            if (todayCount >= DAILY_AI_RECOMMENDATION_LIMIT) {
                throw new BusinessException(ErrorCode.TOPIC_RECOMMENDATION_LIMIT_EXCEEDED);
            }

            // 6-2. 독후감 조회 (APPROVED 상태만)
            List<BookReport> bookReports = bookReportRepository.findByMeetingRoundId(meetingRound.getId())
                    .stream()
                    .filter(br -> br.getStatus() == BookReportStatus.APPROVED)
                    .toList();

            if (bookReports.isEmpty()) {
                throw new BusinessException(ErrorCode.NO_BOOK_REPORTS_FOR_TOPIC);
            }

            // 6-3. AI 서버 호출
            AiTopicRequest aiRequest = new AiTopicRequest(
                    request.topicNo(),
                    bookReports.stream()
                            .map(br -> new AiTopicRequest.ReportInfo(br.getId(), br.getContent()))
                            .toList()
            );

            AiTopicResponse aiResponse = aiClient.requestTopicRecommendation(meetingRound.getId(), aiRequest);
            topicText = aiResponse.data().topic();
            source = TopicSource.AI;
            shouldSaveLog = true;  // AI 호출 성공 시 로그 저장 플래그

        } else {
            // 7. LEADER 모드: 직접 입력
            topicText = request.topic();
            source = TopicSource.LEADER;
        }

        // 8. 토픽 저장/업데이트 (replace 정책) - 먼저 저장
        MeetingRoundDiscussionTopic savedTopic = saveOrUpdateTopic(meetingRound, request.topicNo(), topicText, source);

        // 9. AI 모드일 때만: 토픽 저장 성공 후 로그 저장
        if (shouldSaveLog) {
            TopicRecommendationLog log = TopicRecommendationLog.create(meeting);
            recommendationLogRepository.save(log);
            todayCount++;  // 로그 저장 후 카운트 증가
        }

        // 10. remainingCount 계산
        int remainingCount = Math.max(0, DAILY_AI_RECOMMENDATION_LIMIT - todayCount);

        // 11. 응답 생성
        return TopicRecommendationResponse.builder()
                .meetingId(meetingId)
                .roundNo(roundNo)
                .topic(TopicRecommendationResponse.TopicInfo.builder()
                        .topicNo(savedTopic.getTopicNo())
                        .topic(savedTopic.getTopic())
                        .source(savedTopic.getSource().name())
                        .build())
                .remainingCount(remainingCount)
                .build();
    }

    private MeetingRoundDiscussionTopic saveOrUpdateTopic(
            MeetingRound meetingRound,
            Integer topicNo,
            String topicText,
            TopicSource source
    ) {
        Optional<MeetingRoundDiscussionTopic> existingTopic =
                discussionTopicRepository.findByMeetingRoundIdAndTopicNo(meetingRound.getId(), topicNo);

        if (existingTopic.isPresent()) {
            // UPDATE
            MeetingRoundDiscussionTopic topic = existingTopic.get();
            topic.update(topicText, source);
            return discussionTopicRepository.save(topic);
        } else {
            // INSERT
            MeetingRoundDiscussionTopic newTopic = MeetingRoundDiscussionTopic.create(
                    meetingRound, topicNo, topicText, source);
            return discussionTopicRepository.save(newTopic);
        }
    }
}

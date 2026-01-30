package com.example.doktoribackend.meeting.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
@Builder
public class MyMeetingDetailResponse {
    // 모임 기본 정보
    private Long meetingId;
    private String meetingImagePath;
    private String title;
    private String readingGenreName;
    private LeaderInfo leaderInfo;
    private String myRole;  // "MEMBER", "LEADER"
    private Integer roundCount;
    private Integer capacity;
    private Integer currentMemberCount;

    // 회차별 정보
    private List<RoundDetail> rounds;
    private Integer currentRoundNo;

    @Getter
    @AllArgsConstructor
    @Builder
    public static class LeaderInfo {
        private String profileImagePath;
        private String nickname;
    }

    @Getter
    @AllArgsConstructor
    @Builder
    public static class RoundDetail {
        private Long roundId;
        private Integer roundNo;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime meetingDate;

        private Integer dDay;  // 음수=지남, 0=오늘, 양수=남음
        private String meetingLink;  // 10분 전부터 공개, 과거는 null
        private Boolean canJoinMeeting;  // 참여 버튼 활성화 여부

        private BookInfo book;
        private BookReportInfo bookReport;

        @Getter
        @AllArgsConstructor
        @Builder
        public static class BookInfo {
            private String title;
            private String authors;
            private String publisher;
            private String thumbnailUrl;
            private LocalDate publishedAt;
        }

        @Getter
        @AllArgsConstructor
        @Builder
        public static class BookReportInfo {
            private String status;  // "PENDING_REVIEW", "APPROVED", "REJECTED", null
            private Long id;

            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
            private LocalDateTime writableFrom;

            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
            private LocalDateTime writableUntil;
        }
    }
}
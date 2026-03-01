package com.example.doktoribackend.bookReport.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;

@Builder
@Schema(description = "모임원 독후감 상세 조회 응답")
public record MemberBookReportDetailResponse(
        @Schema(description = "책 정보")
        BookInfo book,

        @Schema(description = "작성자 정보")
        WriterInfo writer,

        @Schema(description = "독후감 정보")
        BookReportInfo bookReport
) {
    @Builder
    @Schema(description = "책 정보")
    public record BookInfo(
            @Schema(description = "책 제목", example = "유령의 마음으로")
            String title,

            @Schema(description = "저자", example = "임선우")
            String authors,

            @Schema(description = "출판사", example = "민음사")
            String publisher,

            @Schema(description = "표지 이미지 URL", example = "https://image.kr/book/1.jpg")
            String thumbnailUrl,

            @Schema(description = "출판일", example = "2022-01-01")
            LocalDate publishedAt
    ) {
    }

    @Builder
    @Schema(description = "작성자 정보")
    public record WriterInfo(
            @Schema(description = "모임 멤버 ID", example = "11")
            Long meetingMemberId,

            @Schema(description = "닉네임", example = "ella")
            String nickname
    ) {
    }

    @Builder
    @Schema(description = "독후감 정보")
    public record BookReportInfo(
            @Schema(description = "독후감 ID", example = "2")
            Long id,

            @Schema(description = "상태", example = "APPROVED")
            String status,

            @Schema(description = "독후감 내용", example = "책을 읽으며 느꼈던 감정...")
            String content,

            @Schema(description = "거절 사유 (거절 시에만)", example = "null")
            String rejectionReason
    ) {
    }
}

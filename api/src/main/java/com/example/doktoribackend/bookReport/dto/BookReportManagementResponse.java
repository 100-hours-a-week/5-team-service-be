package com.example.doktoribackend.bookReport.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.List;

@Builder
@Schema(description = "독후감 관리 조회 응답")
public record BookReportManagementResponse(
        @Schema(description = "회차 번호", example = "3")
        Integer roundNo,

        @Schema(description = "제출 완료 인원 (APPROVED 상태)", example = "4")
        Integer submittedCount,

        @Schema(description = "전체 모임원 수 (APPROVED 멤버)", example = "6")
        Integer totalCount,

        @Schema(description = "멤버별 독후감 정보")
        List<MemberBookReportInfo> members
) {
    @Builder
    @Schema(description = "멤버별 독후감 정보")
    public record MemberBookReportInfo(
            @Schema(description = "모임 멤버 ID", example = "10")
            Long meetingMemberId,

            @Schema(description = "닉네임", example = "startup")
            String nickname,

            @Schema(description = "독후감 정보 (미제출 시 null)")
            BookReportInfo bookReport,

            @Schema(description = "누적 제출률 (%)", example = "70")
            Integer submissionRate
    ) {
    }

    @Builder
    @Schema(description = "독후감 정보")
    public record BookReportInfo(
            @Schema(description = "독후감 ID", example = "123")
            Long id,

            @Schema(description = "독후감 상태", example = "APPROVED")
            String status,

            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
            @Schema(description = "제출 시간", example = "2026-01-12T18:10:00+09:00")
            OffsetDateTime submittedAt
    ) {
    }
}

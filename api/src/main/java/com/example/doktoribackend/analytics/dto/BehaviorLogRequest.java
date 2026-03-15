package com.example.doktoribackend.analytics.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
public class BehaviorLogRequest {

    @NotBlank(message = "sessionId는 필수입니다.")
    private String sessionId;

    @NotNull(message = "sentAt은 필수입니다.")
    private Instant sentAt;

    @NotEmpty(message = "items는 비어있을 수 없습니다.")
    @Valid
    private List<BehaviorLogItem> items;

    @Getter
    @Setter
    public static class BehaviorLogItem {
        @NotNull(message = "meetingId는 필수입니다.")
        private Long meetingId;

        private Integer impressionCount = 0;
        private Integer detailClickCount = 0;
        private Long detailDwellTimeMs = 0L;
    }
}

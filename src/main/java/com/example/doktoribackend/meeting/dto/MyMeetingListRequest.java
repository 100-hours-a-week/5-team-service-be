package com.example.doktoribackend.meeting.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MyMeetingListRequest {

    private static final int DEFAULT_SIZE = 10;

    @NotBlank(message = "status는 필수입니다.")
    private String status;  // "ACTIVE" or "INACTIVE" (required)

    @Min(1)
    private Long cursorId;

    @Min(1)
    @Max(10)
    private Integer size;

    public int getSizeOrDefault() {
        return size != null ? size : DEFAULT_SIZE;
    }
    
    public boolean isActiveFilter() {
        return "ACTIVE".equals(status);
    }
    
    public boolean isValidStatus() {
        return "ACTIVE".equals(status) || "INACTIVE".equals(status);
    }
}
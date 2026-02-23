package com.example.doktoribackend.meeting.dto;

import java.util.List;

public record AiTopicRequest(
        Integer topicNo,
        List<ReportInfo> reports
) {
    public record ReportInfo(
            Long id,
            String content
    ) {
    }
}

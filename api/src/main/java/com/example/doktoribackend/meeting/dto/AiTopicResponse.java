package com.example.doktoribackend.meeting.dto;

public record AiTopicResponse(
        String status,
        Data data
) {
    public record Data(
            String topic
    ) {
    }

    public boolean isSuccess() {
        return "success".equals(status);
    }
}

package com.example.doktoribackend.meeting.dto;

public record AiTopicResponse(
        String status,
        Data data
) {
    public record Data(
            Integer topicNo,
            String topic
    ) {
    }

    public boolean isSuccess() {
        return "success".equals(status);
    }
}

package com.example.doktoribackend.summary.client;

import java.util.List;

public record AiSummaryRequest(
        String topic,
        String roundNo,
        List<MessageItem> messages
) {
    public record MessageItem(
            String senderNickname,
            String textMessage
    ) {}
}

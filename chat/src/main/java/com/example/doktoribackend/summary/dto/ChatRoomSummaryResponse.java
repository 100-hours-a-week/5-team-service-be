package com.example.doktoribackend.summary.dto;

import java.util.List;

public record ChatRoomSummaryResponse(
        Long roomId,
        String topic,
        List<RoundSummaryItem> rounds
) {
    public record RoundSummaryItem(
            int roundNumber,
            RoundSummaryContent summary
    ) {}

    public record RoundSummaryContent(
            List<String> pro,
            List<String> con,
            List<String> mainIssues,
            List<String> unresolvedIssues
    ) {}
}

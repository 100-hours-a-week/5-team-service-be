package com.example.doktoribackend.summary.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

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
            @JsonAlias("main_issues") List<String> mainIssues,
            @JsonAlias("unresolved_issues") List<String> unresolvedIssues
    ) {}
}

package com.example.doktoribackend.summary.client;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AiSummaryResponse(
        String status,
        SummaryContent summary
) {
    public record SummaryContent(
            List<String> pro,
            List<String> con,
            @JsonProperty("main_issues") List<String> mainIssues,
            @JsonProperty("unresolved_issues") List<String> unresolvedIssues
    ) {}
}

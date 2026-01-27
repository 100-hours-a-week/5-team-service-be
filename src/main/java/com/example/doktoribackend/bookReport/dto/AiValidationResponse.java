package com.example.doktoribackend.bookReport.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiValidationResponse(
        String status,
        @JsonProperty("rejection_reason") String rejectionReason
) {}

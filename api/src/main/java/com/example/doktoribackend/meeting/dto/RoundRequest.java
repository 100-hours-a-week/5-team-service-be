package com.example.doktoribackend.meeting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record RoundRequest(
        @NotNull
        @Min(1)
        @Schema(example = "1")
        Integer roundNo,

        @NotNull
        @Schema(example = "2026-01-12")
        LocalDate date,

        @NotNull
        @Valid
        BookRequest book
) {}

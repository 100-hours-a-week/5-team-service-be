package com.example.doktoribackend.user.policy.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ReadingPolicyResponse(
        @Schema(description = "ID", example = "1")
        Long id,

        @Schema(description = "코드", example = "NOVEL")
        String code,

        @Schema(description = "이름", example = "소설")
        String name
) {
}

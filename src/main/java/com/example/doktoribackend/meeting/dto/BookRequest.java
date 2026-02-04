package com.example.doktoribackend.meeting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BookRequest(
        @NotBlank
        @Pattern(
                regexp = "^(\\d{9}[\\dX]|\\d{13})(\\s\\d{13})?$",
                message = "isbn 형식이 올바르지 않습니다"
        )
        @Schema(example = "9781234567890")
        String isbn,

        @NotBlank
        @Size(max = 255)
        @Schema(example = "아몬드")
        String title,

        @Size(max = 255)
        @Schema(example = "손원평")
        String authors,

        @Size(max = 255)
        @Schema(example = "출판사")
        String publisher
) {}

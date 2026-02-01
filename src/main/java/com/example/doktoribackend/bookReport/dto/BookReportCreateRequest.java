package com.example.doktoribackend.bookReport.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "독후감 제출 요청")
public record BookReportCreateRequest (

    @NotBlank(message = "독후감 내용은 필수입니다.")
    @Size(min = 300, max = 1500, message = "독후감은 300~1500자 사이여야 합니다.")
    String content
){}

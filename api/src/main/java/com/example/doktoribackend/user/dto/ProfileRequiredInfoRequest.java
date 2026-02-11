package com.example.doktoribackend.user.dto;


import com.example.doktoribackend.user.domain.Gender;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ProfileRequiredInfoRequest (
        @NotNull(message = "성별은 필수입니다.")
        Gender gender,

        @NotNull(message = "생년월일은 필수입니다.")
        @Min(value = 1900, message = "출생년도는 1900 이상이어야 합니다.")
        @Max(value = 2100, message = "출생년도는 2100 이하여야 합니다.")
        Integer birthYear,

        @NotNull(message = "알림 수신 여부는 필수입니다.")
        Boolean notificationAgreement
){
}

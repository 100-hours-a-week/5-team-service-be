package com.example.doktoribackend.meeting.dto;

import com.example.doktoribackend.meeting.domain.MeetingRound;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class RoundInfo {
    private Integer roundNo;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    private BookInfo book;

    public static RoundInfo from(MeetingRound round) {
        return RoundInfo.builder()
                .roundNo(round.getRoundNo())
                .date(round.getStartAt().toLocalDate())
                .book(BookInfo.from(round.getBook()))
                .build();
    }
}
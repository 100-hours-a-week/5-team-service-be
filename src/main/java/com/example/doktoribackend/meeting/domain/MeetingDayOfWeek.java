package com.example.doktoribackend.meeting.domain;

import java.time.DayOfWeek;
import java.time.LocalDate;

public enum MeetingDayOfWeek {
    MON,
    TUE,
    WED,
    THU,
    FRI,
    SAT,
    SUN;

    public static MeetingDayOfWeek from(LocalDate date) {
        return from(date.getDayOfWeek());
    }

    public static MeetingDayOfWeek from(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> MON;
            case TUESDAY -> TUE;
            case WEDNESDAY -> WED;
            case THURSDAY -> THU;
            case FRIDAY -> FRI;
            case SATURDAY -> SAT;
            case SUNDAY -> SUN;
        };
    }
}

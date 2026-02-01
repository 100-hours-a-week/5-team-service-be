package com.example.doktoribackend.meeting.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PageInfo {
    private final Long nextCursorId;
    private final boolean hasNext;
    private final int size;
}

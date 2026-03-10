package com.example.doktoribackend.review.dto;

import com.example.doktoribackend.meeting.dto.PageInfo;

import java.util.List;

public record ReviewListResponse(
        List<ReviewResponse> items,
        PageInfo pageInfo
) {
}

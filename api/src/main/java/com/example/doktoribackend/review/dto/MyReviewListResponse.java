package com.example.doktoribackend.review.dto;

import com.example.doktoribackend.meeting.dto.PageInfo;

import java.util.List;

public record MyReviewListResponse(
        List<MyReviewResponse> items,
        PageInfo pageInfo
) {
}

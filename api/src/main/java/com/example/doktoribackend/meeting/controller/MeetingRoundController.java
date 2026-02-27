package com.example.doktoribackend.meeting.controller;

import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.common.response.ApiResult;
import com.example.doktoribackend.exception.BusinessException;
import com.example.doktoribackend.meeting.dto.UpdateTopicsRequest;
import com.example.doktoribackend.meeting.dto.UpdateTopicsResponse;
import com.example.doktoribackend.meeting.service.MeetingRoundTopicsService;
import com.example.doktoribackend.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Meeting Round", description = "모임 회차 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/meeting-rounds")
public class MeetingRoundController implements MeetingRoundTopicsApi {

    private final MeetingRoundTopicsService meetingRoundTopicsService;

    @Override
    @PutMapping("/{roundId}/topics")
    public ResponseEntity<ApiResult<UpdateTopicsResponse>> updateTopics(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long roundId,
            @Valid @RequestBody UpdateTopicsRequest request
    ) {
        if (userDetails == null) {
            throw new BusinessException(ErrorCode.AUTH_UNAUTHORIZED);
        }

        if (roundId == null || roundId <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        UpdateTopicsResponse response = meetingRoundTopicsService.updateTopics(
                userDetails.getId(), roundId, request);
        return ResponseEntity.ok(ApiResult.ok(response));
    }
}

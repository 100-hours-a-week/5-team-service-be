package com.example.doktoribackend.analytics.controller;

import com.example.doktoribackend.analytics.dto.BehaviorLogRequest;
import com.example.doktoribackend.analytics.service.UserBehaviorLogService;
import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.common.response.ApiResult;
import com.example.doktoribackend.exception.BusinessException;
import com.example.doktoribackend.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Analytics", description = "사용자 행동 로그 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/analytics")
public class UserBehaviorLogController {

    private final UserBehaviorLogService userBehaviorLogService;

    @Operation(summary = "행동 로그 저장", description = "사용자 행동 로그를 배치로 저장합니다.")
    @PostMapping("/behavior-logs")
    public ResponseEntity<ApiResult<Void>> saveBehaviorLogs(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody BehaviorLogRequest request
    ) {
        if (userDetails == null) {
            throw new BusinessException(ErrorCode.AUTH_UNAUTHORIZED);
        }

        userBehaviorLogService.saveBehaviorLogs(userDetails.getId(), request);
        return ResponseEntity.ok(ApiResult.ok(null));
    }
}

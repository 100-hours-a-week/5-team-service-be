package com.example.doktoribackend.bookReport.controller;

import com.example.doktoribackend.bookReport.dto.BookReportCreateRequest;
import com.example.doktoribackend.bookReport.dto.BookReportCreateResponse;
import com.example.doktoribackend.bookReport.dto.BookReportDetailResponse;
import com.example.doktoribackend.bookReport.dto.BookReportManagementResponse;
import com.example.doktoribackend.bookReport.dto.MemberBookReportDetailResponse;
import com.example.doktoribackend.bookReport.service.BookReportService;
import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.common.response.ApiResult;
import com.example.doktoribackend.exception.BusinessException;
import com.example.doktoribackend.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "BookReport", description = "독후감 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/meeting-rounds/{roundId}/book-reports")
public class BookReportController implements BookReportManagementApi {

    private final BookReportService bookReportService;

    @Operation(summary = "독후감 생성", description = "독후감을 생성합니다. REJECTED 상태일 경우 재제출됩니다.")
    @PostMapping
    public ResponseEntity<ApiResult<BookReportCreateResponse>> createBookReport(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long roundId,
            @Valid @RequestBody BookReportCreateRequest request
    ) {
        BookReportCreateResponse response = bookReportService.createBookReport(
                userDetails.getId(), roundId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResult.ok(response));
    }

    @Operation(summary = "내 독후감 조회", description = "로그인한 사용자의 독후감을 조회합니다.")
    @GetMapping("/me")
    public ResponseEntity<ApiResult<BookReportDetailResponse>> getMyBookReport(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long roundId
    ) {
        BookReportDetailResponse response = bookReportService.getMyBookReport(
                userDetails.getId(), roundId);
        return ResponseEntity.ok(ApiResult.ok(response));
    }

    @Override
    @GetMapping
    public ResponseEntity<ApiResult<BookReportManagementResponse>> getBookReportManagement(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long roundId
    ) {
        if (userDetails == null) {
            throw new BusinessException(ErrorCode.AUTH_UNAUTHORIZED);
        }

        if (roundId == null || roundId <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        BookReportManagementResponse response = bookReportService.getBookReportManagement(
                userDetails.getId(), roundId);
        return ResponseEntity.ok(ApiResult.ok(response));
    }

    @Override
    @GetMapping("/{bookReportId}")
    public ResponseEntity<ApiResult<MemberBookReportDetailResponse>> getMemberBookReport(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long roundId,
            @PathVariable Long bookReportId
    ) {
        if (userDetails == null) {
            throw new BusinessException(ErrorCode.AUTH_UNAUTHORIZED);
        }

        if (roundId == null || roundId <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        if (bookReportId == null || bookReportId <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        MemberBookReportDetailResponse response = bookReportService.getMemberBookReport(
                userDetails.getId(), roundId, bookReportId);
        return ResponseEntity.ok(ApiResult.ok(response));
    }
}

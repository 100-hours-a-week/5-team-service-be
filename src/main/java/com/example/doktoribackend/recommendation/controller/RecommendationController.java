package com.example.doktoribackend.recommendation.controller;

import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.common.response.ApiResult;
import com.example.doktoribackend.exception.BusinessException;
import com.example.doktoribackend.recommendation.dto.RecommendedMeetingDto;
import com.example.doktoribackend.recommendation.service.RecommendationService;
import com.example.doktoribackend.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Recommendation", description = "모임 추천 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    @Operation(summary = "추천 모임 조회", description = "로그인 사용자를 위한 이번 주 추천 모임을 조회합니다. 최대 4개의 모임이 순위대로 반환됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResult.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "code": "OK",
                                      "message": "요청이 성공적으로 처리되었습니다.",
                                      "data": [
                                        {
                                          "meetingId": 101,
                                          "meetingImagePath": "https://image.kr/meeting/101.jpg",
                                          "title": "함께 읽는 에세이 모임",
                                          "readingGenreId": 4,
                                          "leaderNickname": "startup",
                                          "recruitmentDeadline": "2026-01-15"
                                        },
                                        {
                                          "meetingId": 102,
                                          "meetingImagePath": "https://image.kr/meeting/102.jpg",
                                          "title": "경제/경영 스터디",
                                          "readingGenreId": 2,
                                          "leaderNickname": "ella",
                                          "recruitmentDeadline": "2026-01-10"
                                        },
                                        {
                                          "meetingId": 103,
                                          "meetingImagePath": "https://image.kr/meeting/103.jpg",
                                          "title": "인문 토론 모임",
                                          "readingGenreId": 1,
                                          "leaderNickname": "momo",
                                          "recruitmentDeadline": "2026-01-18"
                                        },
                                        {
                                          "meetingId": 104,
                                          "meetingImagePath": "https://image.kr/meeting/104.jpg",
                                          "title": "소설 완독 클럽",
                                          "readingGenreId": 3,
                                          "leaderNickname": "june",
                                          "recruitmentDeadline": "2026-01-13"
                                        }
                                      ]
                                    }
                                    """))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "code": "AUTH_UNAUTHORIZED",
                                      "message": "인증이 필요합니다."
                                    }
                                    """)))
    })
    @GetMapping("/meetings")
    public ResponseEntity<ApiResult<List<RecommendedMeetingDto>>> getRecommendedMeetings(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        // 인증 확인
        if (userDetails == null) {
            throw new BusinessException(ErrorCode.AUTH_UNAUTHORIZED);
        }

        List<RecommendedMeetingDto> response = recommendationService.getRecommendedMeetings(userDetails.getId());
        return ResponseEntity.ok(ApiResult.ok(response));
    }
}
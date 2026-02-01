package com.example.doktoribackend.recommendation.controller;

import com.example.doktoribackend.common.response.ApiResult;
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

    @Operation(summary = "추천 모임 조회", description = "사용자를 위한 추천 모임을 조회합니다. 로그인 사용자는 개인화된 추천을, 미인증 사용자는 모집중인 인기 모임을 제공합니다. 최대 4개의 모임이 반환됩니다.")
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
                                          "readingGenreName": "에세이",
                                          "leaderNickname": "startup",
                                          "recruitmentDeadline": "2026-01-15"
                                        },
                                        {
                                          "meetingId": 102,
                                          "meetingImagePath": "https://image.kr/meeting/102.jpg",
                                          "title": "경제/경영 스터디",
                                          "readingGenreName": "경제/경영",
                                          "leaderNickname": "ella",
                                          "recruitmentDeadline": "2026-01-10"
                                        }
                                      ]
                                    }
                                    """))),
    })
    @GetMapping("/meetings")
    public ResponseEntity<ApiResult<List<RecommendedMeetingDto>>> getRecommendedMeetings(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<RecommendedMeetingDto> response;

        // 인증 여부에 따라 분기 처리
        if (userDetails == null) {
            // 미인증 사용자: 모집중인 모임 중 최신순, rank 우선순위로 제공
            response = recommendationService.getRecommendedMeetingsForGuest();
        } else {
            // 인증 사용자: 개인화된 추천 제공
            response = recommendationService.getRecommendedMeetings(userDetails.getId());
        }

        return ResponseEntity.ok(ApiResult.ok(response));
    }
}
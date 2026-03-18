package com.example.doktoribackend.review.controller;

import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.exception.BusinessException;
import com.example.doktoribackend.review.dto.ReviewCreateResponse;
import com.example.doktoribackend.review.service.ReviewService;
import com.example.doktoribackend.security.CustomUserDetails;
import com.example.doktoribackend.security.CustomUserDetailsService;
import com.example.doktoribackend.security.jwt.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReviewController.class)
class ReviewControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    ReviewService reviewService;

    @MockitoBean
    JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("POST /reviews: 리뷰를 생성하고 201을 반환한다")
    void createReview_success() throws Exception {
        // given
        given(reviewService.createReview(eq(1L), eq(100L), any()))
                .willReturn(new ReviewCreateResponse(1L));

        String requestBody = """
                {
                    "meetingRating": 4.5,
                    "leaderRating": 4.0,
                    "content": "좋은 모임이었습니다",
                    "bestMemberId": 2,
                    "imageKeys": ["image1.jpg", "image2.jpg"]
                }
                """;

        // when & then
        mockMvc.perform(post("/reviews/meeting-rounds/100")
                        .with(SecurityMockMvcRequestPostProcessors.user(createUserDetails(1L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.reviewId").value(1));
    }

    @Test
    @DisplayName("POST /reviews: meetingRating이 없으면 422를 반환한다")
    void createReview_missingMeetingRating() throws Exception {
        // given
        String requestBody = """
                {
                    "leaderRating": 4.0
                }
                """;

        // when & then
        mockMvc.perform(post("/reviews/meeting-rounds/100")
                        .with(SecurityMockMvcRequestPostProcessors.user(createUserDetails(1L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("POST /reviews: 인증되지 않은 사용자는 401을 반환한다")
    void createReview_unauthenticated() throws Exception {
        // given
        String requestBody = """
                {
                    "meetingRating": 4.5,
                    "leaderRating": 4.0
                }
                """;

        // when & then
        mockMvc.perform(post("/reviews/meeting-rounds/100")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /reviews: 이미 리뷰를 작성했으면 409를 반환한다")
    void createReview_alreadySubmitted() throws Exception {
        // given
        given(reviewService.createReview(eq(1L), eq(100L), any()))
                .willThrow(new BusinessException(ErrorCode.REVIEW_ALREADY_SUBMITTED));

        String requestBody = """
                {
                    "meetingRating": 4.5,
                    "leaderRating": 4.0
                }
                """;

        // when & then
        mockMvc.perform(post("/reviews/meeting-rounds/100")
                        .with(SecurityMockMvcRequestPostProcessors.user(createUserDetails(1L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /reviews: 리뷰 기간 만료 시 409를 반환한다")
    void createReview_periodExpired() throws Exception {
        // given
        given(reviewService.createReview(eq(1L), eq(100L), any()))
                .willThrow(new BusinessException(ErrorCode.REVIEW_PERIOD_EXPIRED));

        String requestBody = """
                {
                    "meetingRating": 4.5,
                    "leaderRating": 4.0
                }
                """;

        // when & then
        mockMvc.perform(post("/reviews/meeting-rounds/100")
                        .with(SecurityMockMvcRequestPostProcessors.user(createUserDetails(1L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict());
    }

    private CustomUserDetails createUserDetails(Long userId) {
        return CustomUserDetails.of(userId, "testUser");
    }
}

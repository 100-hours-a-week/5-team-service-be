package com.example.doktoribackend.zoom.service;

import com.example.doktoribackend.meeting.domain.Meeting;
import com.example.doktoribackend.meeting.domain.MeetingRound;
import com.example.doktoribackend.meeting.repository.MeetingRoundRepository;
import com.example.doktoribackend.zoom.exception.ZoomAuthenticationException;
import com.example.doktoribackend.zoom.exception.ZoomRetryableException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ZoomLinkSchedulerServiceTest {

    @Mock
    private MeetingRoundRepository meetingRoundRepository;

    @Mock
    private ZoomService zoomService;

    @InjectMocks
    private ZoomLinkSchedulerService schedulerService;

    @Nested
    @DisplayName("정상 케이스 테스트")
    class NormalCases {

        @Test
        @DisplayName("Zoom 링크 생성 성공")
        void createZoomLink_Success() {
            // given
            MeetingRound mockMeetingRound = createMockMeetingRound(100L, 1, null, LocalDateTime.now().plusMinutes(15));
            String expectedJoinUrl = "https://zoom.us/j/123456789";

            given(meetingRoundRepository.findMeetingRoundIdsForZoomLinkCreation(anyList(), any()))
                    .willReturn(List.of(100L));
            given(meetingRoundRepository.findByIdsWithMeeting(anyList()))
                    .willReturn(List.of(mockMeetingRound));
            given(zoomService.createMeeting(anyString(), any(), anyInt()))
                    .willReturn(expectedJoinUrl);
            given(meetingRoundRepository.findById(100L))
                    .willReturn(Optional.of(mockMeetingRound));

            // when
            schedulerService.createZoomLinksForUpcomingMeetings();

            // then
            verify(zoomService, times(1)).createMeeting(anyString(), any(), eq(30));
            verify(meetingRoundRepository, times(1)).save(any(MeetingRound.class));
        }

        @Test
        @DisplayName("대상 MeetingRound가 없는 경우")
        void createZoomLink_NoTarget() {
            // given
            given(meetingRoundRepository.findMeetingRoundIdsForZoomLinkCreation(anyList(), any()))
                    .willReturn(Collections.emptyList());

            // when
            schedulerService.createZoomLinksForUpcomingMeetings();

            // then
            verify(zoomService, never()).createMeeting(anyString(), any(), anyInt());
        }

        @Test
        @DisplayName("이미 meetingLink가 존재하는 경우 스킵")
        void createZoomLink_SkipIfLinkExists() {
            // given
            MeetingRound mockMeetingRound = createMockMeetingRound(100L, 1, "https://zoom.us/j/existing", LocalDateTime.now().plusMinutes(15));

            given(meetingRoundRepository.findMeetingRoundIdsForZoomLinkCreation(anyList(), any()))
                    .willReturn(List.of(100L));
            given(meetingRoundRepository.findByIdsWithMeeting(anyList()))
                    .willReturn(List.of(mockMeetingRound));

            // when
            schedulerService.createZoomLinksForUpcomingMeetings();

            // then
            verify(zoomService, never()).createMeeting(anyString(), any(), anyInt());
        }
    }

    @Nested
    @DisplayName("재시도 로직 테스트")
    class RetryLogicTests {

        @Test
        @DisplayName("첫 번째 시도 실패 후 재시도 성공")
        void createZoomLink_RetrySuccess() {
            // given
            MeetingRound mockMeetingRound = createMockMeetingRound(100L, 1, null, LocalDateTime.now().plusMinutes(15));
            String expectedJoinUrl = "https://zoom.us/j/123456789";

            given(meetingRoundRepository.findMeetingRoundIdsForZoomLinkCreation(anyList(), any()))
                    .willReturn(List.of(100L));
            given(meetingRoundRepository.findByIdsWithMeeting(anyList()))
                    .willReturn(List.of(mockMeetingRound));
            given(zoomService.createMeeting(anyString(), any(), anyInt()))
                    .willThrow(new ZoomRetryableException("Rate Limit"))
                    .willReturn(expectedJoinUrl);
            given(meetingRoundRepository.findById(100L))
                    .willReturn(Optional.of(mockMeetingRound));

            // when
            schedulerService.createZoomLinksForUpcomingMeetings();

            // then
            verify(zoomService, times(2)).createMeeting(anyString(), any(), eq(30));
            verify(meetingRoundRepository, times(1)).save(any(MeetingRound.class));
        }

        @Test
        @DisplayName("3회 재시도 후에도 실패")
        void createZoomLink_FinalFailure() {
            // given
            MeetingRound mockMeetingRound = createMockMeetingRound(100L, 1, null, LocalDateTime.now().plusMinutes(15));

            given(meetingRoundRepository.findMeetingRoundIdsForZoomLinkCreation(anyList(), any()))
                    .willReturn(List.of(100L));
            given(meetingRoundRepository.findByIdsWithMeeting(anyList()))
                    .willReturn(List.of(mockMeetingRound));
            given(zoomService.createMeeting(anyString(), any(), anyInt()))
                    .willThrow(new ZoomRetryableException("Server Error"));

            // when
            schedulerService.createZoomLinksForUpcomingMeetings();

            // then
            verify(zoomService, times(3)).createMeeting(anyString(), any(), eq(30));
            verify(meetingRoundRepository, never()).save(any(MeetingRound.class));
        }
    }

    @Nested
    @DisplayName("인증 실패 테스트")
    class AuthenticationFailureTests {

        @Test
        @DisplayName("Zoom 인증 실패 시 스케줄러 중단")
        void createZoomLink_AuthFailure_Stops() {
            // given
            MeetingRound mockMeetingRound = createMockMeetingRound(100L, 1, null, LocalDateTime.now().plusMinutes(15));

            given(meetingRoundRepository.findMeetingRoundIdsForZoomLinkCreation(anyList(), any()))
                    .willReturn(List.of(100L));
            given(meetingRoundRepository.findByIdsWithMeeting(anyList()))
                    .willReturn(List.of(mockMeetingRound));
            given(zoomService.createMeeting(anyString(), any(), anyInt()))
                    .willThrow(new ZoomAuthenticationException("Invalid credentials"));

            // when
            schedulerService.createZoomLinksForUpcomingMeetings();

            // then
            verify(zoomService, times(1)).createMeeting(anyString(), any(), eq(30));
        }
    }

    @Nested
    @DisplayName("배치 처리 테스트")
    class BatchProcessingTests {

        @Test
        @DisplayName("여러 MeetingRound 배치 처리")
        void createZoomLink_BatchProcessing() {
            // given
            MeetingRound mockRound1 = createMockMeetingRound(101L, 1, null, LocalDateTime.now().plusMinutes(15));
            MeetingRound mockRound2 = createMockMeetingRound(102L, 2, null, LocalDateTime.now().plusMinutes(15));
            MeetingRound mockRound3 = createMockMeetingRound(103L, 3, null, LocalDateTime.now().plusMinutes(15));

            given(meetingRoundRepository.findMeetingRoundIdsForZoomLinkCreation(anyList(), any()))
                    .willReturn(Arrays.asList(101L, 102L, 103L));
            given(meetingRoundRepository.findByIdsWithMeeting(anyList()))
                    .willReturn(Arrays.asList(mockRound1, mockRound2, mockRound3));
            given(zoomService.createMeeting(anyString(), any(), anyInt()))
                    .willReturn("https://zoom.us/j/1")
                    .willReturn("https://zoom.us/j/2")
                    .willReturn("https://zoom.us/j/3");
            given(meetingRoundRepository.findById(anyLong()))
                    .willAnswer(invocation -> {
                        Long id = invocation.getArgument(0);
                        if (id == 101L) return Optional.of(mockRound1);
                        if (id == 102L) return Optional.of(mockRound2);
                        if (id == 103L) return Optional.of(mockRound3);
                        return Optional.empty();
                    });

            // when
            schedulerService.createZoomLinksForUpcomingMeetings();

            // then
            verify(zoomService, times(3)).createMeeting(anyString(), any(), eq(30));
            verify(meetingRoundRepository, times(3)).save(any(MeetingRound.class));
        }

        @Test
        @DisplayName("배치 중간 실패해도 나머지 계속 처리")
        void createZoomLink_PartialFailure_ContinuesProcessing() {
            // given
            MeetingRound mockRound1 = createMockMeetingRound(101L, 1, null, LocalDateTime.now().plusMinutes(15));
            MeetingRound mockRound2 = createMockMeetingRound(102L, 2, null, LocalDateTime.now().plusMinutes(15));
            MeetingRound mockRound3 = createMockMeetingRound(103L, 3, null, LocalDateTime.now().plusMinutes(15));

            given(meetingRoundRepository.findMeetingRoundIdsForZoomLinkCreation(anyList(), any()))
                    .willReturn(Arrays.asList(101L, 102L, 103L));
            given(meetingRoundRepository.findByIdsWithMeeting(anyList()))
                    .willReturn(Arrays.asList(mockRound1, mockRound2, mockRound3));
            given(zoomService.createMeeting(anyString(), any(), anyInt()))
                    .willReturn("https://zoom.us/j/1")
                    .willThrow(new ZoomRetryableException("Failed"))
                    .willThrow(new ZoomRetryableException("Failed"))
                    .willThrow(new ZoomRetryableException("Failed"))
                    .willReturn("https://zoom.us/j/3");
            given(meetingRoundRepository.findById(anyLong()))
                    .willAnswer(invocation -> {
                        Long id = invocation.getArgument(0);
                        if (id == 101L) return Optional.of(mockRound1);
                        if (id == 103L) return Optional.of(mockRound3);
                        return Optional.empty();
                    });

            // when
            schedulerService.createZoomLinksForUpcomingMeetings();

            // then
            // round1: 1회 성공, round2: 3회 실패, round3: 1회 성공 = 총 5회
            verify(zoomService, times(5)).createMeeting(anyString(), any(), eq(30));
            verify(meetingRoundRepository, times(2)).save(any(MeetingRound.class));
        }
    }

    @Nested
    @DisplayName("엣지 케이스 테스트")
    class EdgeCaseTests {

        @Test
        @DisplayName("startAt이 과거인 경우 스킵")
        void createZoomLink_PastStartAt_Skip() {
            // given
            MeetingRound mockMeetingRound = createMockMeetingRound(100L, 1, null, LocalDateTime.now().minusMinutes(5));

            given(meetingRoundRepository.findMeetingRoundIdsForZoomLinkCreation(anyList(), any()))
                    .willReturn(List.of(100L));
            given(meetingRoundRepository.findByIdsWithMeeting(anyList()))
                    .willReturn(List.of(mockMeetingRound));

            // when
            schedulerService.createZoomLinksForUpcomingMeetings();

            // then
            verify(zoomService, never()).createMeeting(anyString(), any(), anyInt());
        }
    }

    private MeetingRound createMockMeetingRound(Long id, int roundNo, String meetingLink, LocalDateTime startAt) {
        Meeting meeting = mock(Meeting.class);
        given(meeting.getId()).willReturn(1L);
        given(meeting.getTitle()).willReturn("테스트 모임");

        MeetingRound round = mock(MeetingRound.class);
        given(round.getId()).willReturn(id);
        given(round.getMeeting()).willReturn(meeting);
        given(round.getRoundNo()).willReturn(roundNo);
        given(round.getStartAt()).willReturn(startAt);
        given(round.getMeetingLink()).willReturn(meetingLink);

        return round;
    }
}

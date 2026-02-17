package com.example.doktoribackend.recommendation.service;

import com.example.doktoribackend.reading.domain.ReadingGenre;
import com.example.doktoribackend.reading.repository.ReadingGenreRepository;
import com.example.doktoribackend.recommendation.domain.UserMeetingRecommendation;
import com.example.doktoribackend.recommendation.dto.RecommendedMeetingDto;
import com.example.doktoribackend.recommendation.repository.UserMeetingRecommendationRepository;
import com.example.doktoribackend.common.s3.ImageUrlResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final UserMeetingRecommendationRepository recommendationRepository;
    private final ReadingGenreRepository readingGenreRepository;
    private final ImageUrlResolver imageUrlResolver;

    @Transactional(readOnly = true)
    public List<RecommendedMeetingDto> getRecommendedMeetings(Long userId) {
        // 1. 이번 주 월요일 계산
        LocalDate weekStartDate = getThisWeekMonday();

        // 2. 추천 조회 (rank 순서대로, 최대 4개)
        List<UserMeetingRecommendation> recommendations = recommendationRepository
                .findByUserIdAndWeekStartDateOrderByRank(userId, weekStartDate)
                .stream()
                .limit(4)
                .toList();

        // 3. ReadingGenre 조회 (N+1 방지)
        List<Long> genreIds = recommendations.stream()
                .map(r -> r.getMeeting().getReadingGenreId())
                .distinct()
                .toList();

        Map<Long, String> genreNameMap = readingGenreRepository.findAllById(genreIds)
                .stream()
                .collect(Collectors.toMap(ReadingGenre::getId, ReadingGenre::getName));

        // 4. DTO 변환
        return recommendations.stream()
                .map(r -> toDto(r, genreNameMap))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RecommendedMeetingDto> getRecommendedMeetingsForGuest() {
        // 1. 모집중인 모임 중 최신순, rank 우선순위로 조회 (최대 4개)
        List<UserMeetingRecommendation> recommendations = recommendationRepository
                .findRecruitingMeetingsOrderByLatestAndRank()
                .stream()
                .limit(4)
                .toList();

        // 2. ReadingGenre 조회 (N+1 방지)
        List<Long> genreIds = recommendations.stream()
                .map(r -> r.getMeeting().getReadingGenreId())
                .distinct()
                .toList();

        Map<Long, String> genreNameMap = readingGenreRepository.findAllById(genreIds)
                .stream()
                .collect(Collectors.toMap(ReadingGenre::getId, ReadingGenre::getName));

        // 3. DTO 변환
        return recommendations.stream()
                .map(r -> toDto(r, genreNameMap))
                .toList();
    }

    private LocalDate getThisWeekMonday() {
        LocalDate today = LocalDate.now();
        return today.with(DayOfWeek.MONDAY);
    }

    private RecommendedMeetingDto toDto(
            UserMeetingRecommendation recommendation,
            Map<Long, String> genreNameMap
    ) {
        var meeting = recommendation.getMeeting();
        var leader = meeting.getLeaderUser();

        return RecommendedMeetingDto.builder()
                .meetingId(meeting.getId())
                .meetingImagePath(imageUrlResolver.toUrl(meeting.getMeetingImagePath()))
                .title(meeting.getTitle())
                .readingGenreName(genreNameMap.get(meeting.getReadingGenreId()))
                .leaderNickname(leader.getNickname())
                .recruitmentDeadline(meeting.getRecruitmentDeadline())
                .build();
    }
}
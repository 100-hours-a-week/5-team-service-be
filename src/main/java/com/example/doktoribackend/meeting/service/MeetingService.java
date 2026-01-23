package com.example.doktoribackend.meeting.service;

import com.example.doktoribackend.book.domain.Book;
import com.example.doktoribackend.book.repository.BookRepository;
import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.exception.BusinessException;
import com.example.doktoribackend.meeting.domain.Meeting;
import com.example.doktoribackend.meeting.domain.MeetingDayOfWeek;
import com.example.doktoribackend.meeting.domain.MeetingMember;
import com.example.doktoribackend.meeting.domain.MeetingRound;
import com.example.doktoribackend.meeting.dto.MeetingCreateRequest;
import com.example.doktoribackend.meeting.dto.MeetingCreateResponse;
import com.example.doktoribackend.meeting.dto.MeetingListItem;
import com.example.doktoribackend.meeting.dto.MeetingListRequest;
import com.example.doktoribackend.meeting.dto.MeetingListResponse;
import com.example.doktoribackend.meeting.dto.PageInfo;
import com.example.doktoribackend.meeting.repository.MeetingMemberRepository;
import com.example.doktoribackend.meeting.repository.MeetingRepository;
import com.example.doktoribackend.meeting.repository.MeetingRoundRepository;
import com.example.doktoribackend.reading.repository.ReadingGenreRepository;
import com.example.doktoribackend.s3.ImageUrlResolver;
import com.example.doktoribackend.user.domain.User;
import com.example.doktoribackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final MeetingRoundRepository meetingRoundRepository;
    private final MeetingMemberRepository meetingMemberRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final ReadingGenreRepository readingGenreRepository;
    private final ImageUrlResolver imageUrlResolver;

    @Transactional
    public MeetingCreateResponse createMeeting(Long userId, MeetingCreateRequest request) {
        User leader = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!readingGenreRepository.existsByIdAndDeletedAtIsNull(request.getReadingGenreId())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        MeetingCreateRequest.TimeRequest time = request.getTime();
        LocalTime startTime = time.getStartTime();
        LocalTime endTime = time.getEndTime();
        int durationMinutes = resolveDurationMinutes(request.getDurationMinutes(), startTime, endTime);

        LocalDate firstRoundDate = request.getFirstRoundAt();
        LocalDateTime firstRoundAt = LocalDateTime.of(firstRoundDate, startTime);
        MeetingDayOfWeek dayOfWeek = MeetingDayOfWeek.from(firstRoundDate);

        Map<Integer, LocalDate> roundDates = toRoundDateMap(request.getRounds());
        if (!roundDates.containsKey(1)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (!roundDates.get(1).equals(firstRoundDate)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        Map<Integer, MeetingCreateRequest.BookRequest> booksByRound = toBookByRoundMap(request.getBooksByRound());
        if (booksByRound.size() != roundDates.size()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        if (request.getCapacity() < 3) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        String meetingImagePath = request.getMeetingImagePath();

        Meeting meeting = Meeting.create(
                leader,
                request.getReadingGenreId(),
                request.getLeaderIntro(),
                meetingImagePath,
                request.getTitle(),
                request.getDescription(),
                request.getCapacity(),
                request.getRoundCount(),
                dayOfWeek,
                startTime,
                durationMinutes,
                firstRoundAt,
                request.getRecruitmentDeadline(),
                1
        );
        meetingRepository.save(meeting);

        LocalDateTime approvedAt = LocalDateTime.now();
        MeetingMember leaderMember = MeetingMember.createLeader(meeting, leader, approvedAt);
        meetingMemberRepository.save(leaderMember);

        List<MeetingRound> rounds = request.getRounds().stream()
                .map(round -> {
                    int roundNo = round.getRoundNo();
                    MeetingCreateRequest.BookRequest bookRequest = booksByRound.get(roundNo);
                    if (bookRequest == null) {
                        throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
                    }
                    Book book = resolveBook(bookRequest);
                    LocalDateTime startAt = LocalDateTime.of(round.getDate(), startTime);
                    LocalDateTime endAt = startAt.plusMinutes(durationMinutes);
                    return MeetingRound.create(meeting, book, roundNo, startAt, endAt);
                })
                .toList();
        meetingRoundRepository.saveAll(rounds);

        if (Boolean.TRUE.equals(request.getLeaderIntroSavePolicy())) {
            leader.updateLeaderIntro(request.getLeaderIntro());
        }

        return new MeetingCreateResponse(meeting.getId());
    }

    @Transactional(readOnly = true)
    public MeetingListResponse getMeetings(MeetingListRequest request) {
        int size = request.getSizeOrDefault();
        List<MeetingListItem> results = meetingRepository.findMeetingList(request, size + 1);

        boolean hasNext = results.size() > size;
        List<MeetingListItem> items = hasNext ? results.subList(0, size) : results;
        List<MeetingListItem> mapped = items.stream()
                .map(item -> new MeetingListItem(
                        item.getMeetingId(),
                        imageUrlResolver.toUrl(item.getMeetingImagePath()),
                        item.getTitle(),
                        item.getReadingGenreId(),
                        item.getLeaderNickname(),
                        item.getCapacity(),
                        item.getCurrentMemberCount()
                ))
                .toList();

        Long nextCursorId = hasNext ? items.get(items.size() - 1).getMeetingId() : null;

        PageInfo pageInfo = new PageInfo(nextCursorId, hasNext, size);
        return new MeetingListResponse(mapped, pageInfo);
    }

    private int resolveDurationMinutes(Integer durationMinutes, LocalTime startTime, LocalTime endTime) {
        long diff = ChronoUnit.MINUTES.between(startTime, endTime);
        if (diff < 30 || diff % 30 != 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (durationMinutes == null) {
            return (int) diff;
        }
        if (durationMinutes < 30 || durationMinutes % 30 != 0 || durationMinutes != (int) diff) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return durationMinutes;
    }

    private Map<Integer, LocalDate> toRoundDateMap(List<MeetingCreateRequest.RoundRequest> rounds) {
        Map<Integer, LocalDate> map = new HashMap<>();
        for (MeetingCreateRequest.RoundRequest round : rounds) {
            map.put(round.getRoundNo(), round.getDate());
        }
        return map;
    }

    private Map<Integer, MeetingCreateRequest.BookRequest> toBookByRoundMap(
            List<MeetingCreateRequest.BookByRoundRequest> bookRequests
    ) {
        Map<Integer, MeetingCreateRequest.BookRequest> map = new HashMap<>();
        for (MeetingCreateRequest.BookByRoundRequest request : bookRequests) {
            if (map.putIfAbsent(request.getRoundNo(), request.getBook()) != null) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
            }
        }
        return map;
    }

    private Book resolveBook(MeetingCreateRequest.BookRequest request) {
        String isbn13 = normalizeIsbn(request.getIsbn13());
        if (isbn13 != null) {
            return bookRepository.findByIsbn13(isbn13)
                    .map(this::reviveIfDeleted)
                    .orElseGet(() -> bookRepository.save(toBook(request, isbn13)));
        }
        return bookRepository.save(toBook(request, null));
    }

    private Book reviveIfDeleted(Book existing) {
        if (existing.isDeleted()) {
            existing.revive();
        }
        return existing;
    }

    private Book toBook(MeetingCreateRequest.BookRequest request, String isbn13) {
        String authors = normalizeAuthors(request.getAuthors());
        return Book.create(
                isbn13,
                request.getTitle(),
                authors,
                request.getPublisher(),
                request.getThumbnailUrl(),
                request.getPublishedAt()
        );
    }

    private String normalizeIsbn(String isbn13) {
        if (isbn13 == null) {
            return null;
        }
        String normalized = isbn13.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private String normalizeAuthors(String authors) {
        if (authors == null) {
            return null;
        }
        String normalized = authors.trim();
        return normalized.isBlank() ? null : normalized;
    }
}

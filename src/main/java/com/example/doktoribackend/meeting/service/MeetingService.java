package com.example.doktoribackend.meeting.service;

import com.example.doktoribackend.book.domain.Book;
import com.example.doktoribackend.book.repository.BookRepository;
import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.exception.BusinessException;
import com.example.doktoribackend.meeting.domain.Meeting;
import com.example.doktoribackend.meeting.domain.MeetingDayOfWeek;
import com.example.doktoribackend.meeting.domain.MeetingMember;
import com.example.doktoribackend.meeting.domain.MeetingMemberStatus;
import com.example.doktoribackend.meeting.domain.MeetingRound;
import com.example.doktoribackend.meeting.domain.MeetingStatus;
import com.example.doktoribackend.meeting.dto.MeetingCreateRequest;
import com.example.doktoribackend.meeting.dto.MeetingCreateResponse;
import com.example.doktoribackend.meeting.dto.MeetingDetailResponse;
import com.example.doktoribackend.meeting.dto.JoinMeetingResponse;
import com.example.doktoribackend.meeting.dto.MeetingListRequest;
import com.example.doktoribackend.meeting.dto.MeetingListResponse;
import com.example.doktoribackend.meeting.dto.MeetingSearchRequest;
import com.example.doktoribackend.meeting.dto.PageInfo;
import com.example.doktoribackend.meeting.dto.MeetingListItem;
import com.example.doktoribackend.meeting.dto.MeetingListRow;
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
        List<MeetingListRow> results = meetingRepository.findMeetingList(request, size + 1);

        boolean hasNext = results.size() > size;
        List<MeetingListRow> sliced = hasNext ? results.subList(0, size) : results;
        List<MeetingListItem> mapped = sliced.stream()
                .map(this::toListItem)
                .toList();

        Long nextCursorId = hasNext ? mapped.get(mapped.size() - 1).getMeetingId() : null;

        PageInfo pageInfo = new PageInfo(nextCursorId, hasNext, size);
        return new MeetingListResponse(mapped, pageInfo);
    }

    @Transactional(readOnly = true)
    public MeetingDetailResponse getMeetingDetail(Long meetingId, Long currentUserId) {
        // 1. 모임 기본 정보 조회 (모임장 포함)
        Meeting meeting = meetingRepository.findByIdWithLeader(meetingId)
                .filter(m -> m.getDeletedAt() == null)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEETING_NOT_FOUND));

        // 2. 회차 정보 조회 (책 정보 포함)
        List<MeetingRound> rounds = meetingRoundRepository.findByMeetingIdWithBook(meetingId);

        // 3. 참여자 정보 조회 (APPROVED 상태, 가입순)
        List<MeetingMember> approvedMembers = meetingMemberRepository
                .findApprovedMembersByMeetingIdOrderByCreatedAt(meetingId);

        // 4. 현재 사용자 참여 상태 조회
        String myParticipationStatus = null;
        if (currentUserId != null) {
            myParticipationStatus = meetingMemberRepository
                    .findByMeetingIdAndUserId(meetingId, currentUserId)
                    .map(mm -> mm.getStatus().name())
                    .orElse(null);
        }

        // 5. DTO 변환 및 반환
        return MeetingDetailResponse.from(meeting, rounds, approvedMembers, myParticipationStatus);
    }

    @Transactional
    public JoinMeetingResponse joinMeeting(Long userId, Long meetingId) {
        // 1. 사용자 조회
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 2. 모임 조회 및 검증
        Meeting meeting = meetingRepository.findById(meetingId)
                .filter(m -> m.getDeletedAt() == null)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEETING_NOT_FOUND));

        // 3. 모집 상태 확인
        if (meeting.getStatus() != MeetingStatus.RECRUITING) {
            throw new BusinessException(ErrorCode.RECRUITMENT_CLOSED);
        }

        // 4. 중복 신청 방지
        meetingMemberRepository.findByMeetingIdAndUserId(meetingId, userId)
                .ifPresent(existingMember -> {
                    MeetingMemberStatus status = existingMember.getStatus();
                    // PENDING, APPROVED: 이미 신청/승인됨
                    if (status == MeetingMemberStatus.PENDING || status == MeetingMemberStatus.APPROVED) {
                        throw new BusinessException(ErrorCode.JOIN_REQUEST_ALREADY_EXISTS);
                    }
                    // KICKED: 강퇴된 사용자는 재신청 불가
                    if (status == MeetingMemberStatus.KICKED) {
                        throw new BusinessException(ErrorCode.JOIN_REQUEST_BLOCKED);
                    }
                    // REJECTED, LEFT: 재신청 가능 (if문 통과)
                });

        // 5. 정원 확인 (현재 승인된 인원 기준)
        if (meeting.getCurrentCount() >= meeting.getCapacity()) {
            throw new BusinessException(ErrorCode.CAPACITY_FULL);
        }

        // 6. 참여 요청 생성
        MeetingMember member = MeetingMember.createParticipant(meeting, user);
        meetingMemberRepository.save(member);

        // 7. 응답 반환
        return JoinMeetingResponse.from(member);
    }

    @Transactional(readOnly = true)
    public MeetingListResponse searchMeetings(MeetingSearchRequest request) {
        int size = request.getSizeOrDefault();
        List<MeetingListRow> results = meetingRepository.searchMeetings(request, size + 1);

        boolean hasNext = results.size() > size;
        List<MeetingListRow> sliced = hasNext ? results.subList(0, size) : results;
        List<MeetingListItem> mapped = sliced.stream()
                .map(this::toListItem)
                .toList();

        Long nextCursorId = hasNext ? mapped.get(mapped.size() - 1).getMeetingId() : null;

        PageInfo pageInfo = new PageInfo(nextCursorId, hasNext, size);
        return new MeetingListResponse(mapped, pageInfo);
    }

    private MeetingListItem toListItem(MeetingListRow row) {
        Long remainingDays = null;
        if (row.getRecruitmentDeadline() != null) {
            remainingDays = java.time.temporal.ChronoUnit.DAYS.between(
                    java.time.LocalDate.now(),
                    row.getRecruitmentDeadline()
            );
        }
        return new MeetingListItem(
                row.getMeetingId(),
                imageUrlResolver.toUrl(row.getMeetingImagePath()),
                row.getTitle(),
                row.getReadingGenreId(),
                row.getLeaderNickname(),
                row.getCapacity(),
                row.getCurrentMemberCount(),
                remainingDays
        );
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

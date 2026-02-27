package com.example.doktoribackend.meeting.service;

import com.example.doktoribackend.common.client.KakaoBookClient;
import com.example.doktoribackend.book.domain.Book;
import com.example.doktoribackend.common.client.KakaoBookResponse;
import com.example.doktoribackend.book.repository.BookRepository;
import com.example.doktoribackend.bookReport.domain.BookReport;
import com.example.doktoribackend.bookReport.domain.BookReportStatus;
import com.example.doktoribackend.bookReport.domain.BookReportStatusResolver;
import com.example.doktoribackend.bookReport.domain.UserBookReportStatus;
import com.example.doktoribackend.bookReport.repository.BookReportRepository;
import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.exception.BusinessException;
import com.example.doktoribackend.meeting.domain.Meeting;
import com.example.doktoribackend.meeting.domain.MeetingDayOfWeek;
import com.example.doktoribackend.meeting.domain.MeetingMember;
import com.example.doktoribackend.meeting.domain.MeetingMemberStatus;
import com.example.doktoribackend.meeting.domain.MeetingRound;
import com.example.doktoribackend.meeting.domain.MeetingRoundStatus;
import com.example.doktoribackend.meeting.domain.MeetingStatus;
import com.example.doktoribackend.meeting.dto.BookRequest;
import com.example.doktoribackend.meeting.dto.MeetingCreateRequest;
import com.example.doktoribackend.meeting.dto.MeetingCreateResponse;
import com.example.doktoribackend.meeting.dto.MeetingDetailResponse;
import com.example.doktoribackend.meeting.dto.JoinMeetingResponse;
import com.example.doktoribackend.meeting.dto.MeetingListRequest;
import com.example.doktoribackend.meeting.dto.MeetingListResponse;
import com.example.doktoribackend.meeting.dto.MeetingMembersResponse;
import com.example.doktoribackend.meeting.dto.MeetingSearchRequest;
import com.example.doktoribackend.meeting.dto.MeetingUpdateRequest;
import com.example.doktoribackend.meeting.dto.MyMeetingListRequest;
import com.example.doktoribackend.meeting.dto.ParticipationStatusUpdateRequest;
import com.example.doktoribackend.meeting.dto.ParticipationStatusUpdateResponse;
import com.example.doktoribackend.meeting.dto.RoundRequest;
import com.example.doktoribackend.meeting.dto.MyMeetingListResponse;
import com.example.doktoribackend.meeting.dto.MyMeetingItem;
import com.example.doktoribackend.meeting.dto.MyMeetingDetailResponse;
import com.example.doktoribackend.meeting.dto.PageInfo;
import com.example.doktoribackend.meeting.dto.MeetingListItem;
import com.example.doktoribackend.meeting.dto.MeetingListRow;
import com.example.doktoribackend.meeting.repository.MeetingMemberRepository;
import com.example.doktoribackend.meeting.repository.MeetingRepository;
import com.example.doktoribackend.meeting.repository.MeetingRoundRepository;
import com.example.doktoribackend.meeting.repository.NextRoundProjection;
import com.example.doktoribackend.reading.domain.ReadingGenre;
import com.example.doktoribackend.reading.repository.ReadingGenreRepository;
import com.example.doktoribackend.common.s3.ImageUrlResolver;
import com.example.doktoribackend.user.domain.User;
import com.example.doktoribackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final MeetingRoundRepository meetingRoundRepository;
    private final MeetingMemberRepository meetingMemberRepository;
    private final BookRepository bookRepository;
    private final KakaoBookClient kakaoBookClient;
    private final UserRepository userRepository;
    private final ReadingGenreRepository readingGenreRepository;
    private final BookReportRepository bookReportRepository;
    private final ImageUrlResolver imageUrlResolver;

    @Transactional
    public MeetingCreateResponse createMeeting(Long userId, MeetingCreateRequest request) {
        User leader = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!readingGenreRepository.existsByIdAndDeletedAtIsNull(request.readingGenreId())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        LocalTime startTime = request.startTime();
        int durationMinutes = request.durationMinutes();

        LocalDate firstRoundDate = request.firstRoundAt();
        if (firstRoundDate == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        LocalDateTime firstRoundAt = LocalDateTime.of(firstRoundDate, startTime);
        MeetingDayOfWeek dayOfWeek = MeetingDayOfWeek.from(firstRoundDate);

        Meeting meeting = Meeting.create(
                leader,
                request.readingGenreId(),
                request.leaderIntro(),
                request.meetingImagePath(),
                request.title(),
                request.description(),
                request.capacity(),
                request.roundCount(),
                dayOfWeek,
                startTime,
                durationMinutes,
                firstRoundAt,
                request.recruitmentDeadline(),
                1
        );
        meetingRepository.save(meeting);

        LocalDateTime approvedAt = LocalDateTime.now();
        MeetingMember leaderMember = MeetingMember.createLeader(meeting, leader, approvedAt);
        meetingMemberRepository.save(leaderMember);

        List<MeetingRound> rounds = request.rounds().stream()
                .map(round -> {
                    Book book = resolveBook(round.book());
                    LocalDateTime startAt = LocalDateTime.of(round.date(), startTime);
                    LocalDateTime endAt = startAt.plusMinutes(durationMinutes);
                    return MeetingRound.create(meeting, book, round.roundNo(), startAt, endAt);
                })
                .toList();
        meetingRoundRepository.saveAll(rounds);

        if (Boolean.TRUE.equals(request.leaderIntroSavePolicy())) {
            leader.updateLeaderIntro(request.leaderIntro());
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

        Long nextCursorId = hasNext ? mapped.getLast().getMeetingId() : null;

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
        return MeetingDetailResponse.from(meeting, rounds, approvedMembers, myParticipationStatus, imageUrlResolver);
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

        // 4. 모집 마감일 확인
        LocalDate today = LocalDate.now();
        if (today.isAfter(meeting.getRecruitmentDeadline())) {
            throw new BusinessException(ErrorCode.RECRUITMENT_CLOSED);
        }

        // 5. 정원 확인 (현재 승인된 인원 기준)
        if (meeting.getCurrentCount() >= meeting.getCapacity()) {
            throw new BusinessException(ErrorCode.CAPACITY_FULL);
        }

        // 6. 중복 신청 방지
        meetingMemberRepository.findByMeetingIdAndUserId(meetingId, userId)
                .ifPresent(existingMember -> {
                    MeetingMemberStatus status = existingMember.getStatus();
                    // APPROVED: 이미 승인됨
                    if (status == MeetingMemberStatus.APPROVED) {
                        throw new BusinessException(ErrorCode.JOIN_REQUEST_ALREADY_EXISTS);
                    }
                    // KICKED: 강퇴된 사용자는 재신청 불가
                    if (status == MeetingMemberStatus.KICKED) {
                        throw new BusinessException(ErrorCode.JOIN_REQUEST_BLOCKED);
                    }
                    // PENDING은 추후 사용 예정 (현재 정책에서는 발생하지 않음)
                    if (status == MeetingMemberStatus.PENDING) {
                        throw new BusinessException(ErrorCode.JOIN_REQUEST_ALREADY_EXISTS);
                    }
                    // REJECTED, LEFT: 재신청 가능 (if문 통과)
                });

        // 7. 참여 요청 생성 (PENDING 상태)
        MeetingMember member = MeetingMember.createParticipant(meeting, user);
        meetingMemberRepository.save(member);

        // 8. 응답 반환 (current_count 증가는 승인 시점에 수행)
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

        Long nextCursorId = hasNext ? mapped.getLast().getMeetingId() : null;

        PageInfo pageInfo = new PageInfo(nextCursorId, hasNext, size);
        return new MeetingListResponse(mapped, pageInfo);
    }

    @Transactional
    public MyMeetingListResponse getMyMeetings(Long userId, MyMeetingListRequest request) {
        int size = request.getSizeOrDefault();
        boolean activeOnly = request.isActiveFilter();

        List<MeetingListRow> results = meetingRepository.findMyMeetings(
                userId,
                request.getCursorId(),
                activeOnly,
                size + 1
        );

        boolean hasNext = results.size() > size;
        List<MeetingListRow> sliced = hasNext ? results.subList(0, size) : results;

        // 조회된 모임 중 종료된 모임 상태 갱신
        LocalDateTime now = LocalDateTime.now();
        updateCompletedMeetingsStatus(sliced, now);

        // N+1 해결: 다음 회차 일괄 조회
        List<Long> meetingIds = sliced.stream()
                .map(MeetingListRow::getMeetingId)
                .toList();
        
        Map<Long, LocalDateTime> nextRoundMap = meetingRoundRepository
                .findNextRoundDatesByMeetingIds(meetingIds, now)
                .stream()
                .collect(Collectors.toMap(
                        NextRoundProjection::getMeetingId,
                        NextRoundProjection::getNextRoundDate
                ));

        List<MyMeetingItem> mapped = sliced.stream()
                .map(row -> toMyMeetingItem(row, now, nextRoundMap))
                .toList();

        Long nextCursorId = hasNext ? mapped.getLast().getMeetingId() : null;
        PageInfo pageInfo = new PageInfo(nextCursorId, hasNext, size);

        return new MyMeetingListResponse(mapped, pageInfo);
    }

    @Transactional
    public MyMeetingListResponse getMyTodayMeetings(Long userId) {
        LocalDate today = LocalDate.now();
        List<MeetingListRow> results = meetingRepository.findMyTodayMeetings(userId, today);

        // 조회된 모임 중 종료된 모임 상태 갱신
        LocalDateTime now = LocalDateTime.now();
        updateCompletedMeetingsStatus(results, now);

        List<MyMeetingItem> mapped = results.stream()
                .map(row -> toMyMeetingItem(row, now))
                .toList();

        // 페이징 없음
        PageInfo pageInfo = new PageInfo(null, false, 10);

        return new MyMeetingListResponse(mapped, pageInfo);
    }

    @Transactional
    public MyMeetingDetailResponse getMyMeetingDetail(Long userId, Long meetingId) {
        // 1. 모임 기본 정보 조회
        Meeting meeting = meetingRepository.findByIdWithLeader(meetingId)
                .filter(m -> m.getDeletedAt() == null)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEETING_NOT_FOUND));

        // 2. 나의 참여 상태 확인
        MeetingMember myMember = meetingMemberRepository.findByMeetingIdAndUserId(meetingId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEETING_NOT_FOUND));

        // APPROVED나 PENDING이 아니면 접근 불가
        if (myMember.getStatus() != MeetingMemberStatus.APPROVED &&
                myMember.getStatus() != MeetingMemberStatus.PENDING) {
            throw new BusinessException(ErrorCode.MEETING_NOT_FOUND);
        }

        // 3. 종료된 모임 상태 갱신
        LocalDateTime now = LocalDateTime.now();
        updateCompletedMeetingStatus(meeting, now);

        // 4. ReadingGenre 조회
        ReadingGenre readingGenre = readingGenreRepository.findById(meeting.getReadingGenreId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE));

        // 5. 나의 역할 판단
        boolean isLeader = meeting.getLeaderUser().getId().equals(userId);
        String myRole = isLeader ? "LEADER" : "MEMBER";

        // 6. 회차 정보 조회
        List<MeetingRound> rounds = meetingRoundRepository.findByMeetingIdWithBook(meetingId);

        List<MyMeetingDetailResponse.RoundDetail> roundDetails = rounds.stream()
                .map(round -> toRoundDetail(round, userId, now, meeting, myMember, rounds))
                .collect(Collectors.toList());

        // 6. DTO 생성
        return MyMeetingDetailResponse.builder()
                .meetingId(meeting.getId())
                .meetingImagePath(imageUrlResolver.toUrl(meeting.getMeetingImagePath()))
                .meetingImageKey(meeting.getMeetingImagePath())
                .title(meeting.getTitle())
                .readingGenreName(readingGenre.getName())
                .leaderInfo(MyMeetingDetailResponse.LeaderInfo.builder()
                        .profileImagePath(imageUrlResolver.toUrl(meeting.getLeaderUser().getProfileImagePath()))
                        .nickname(meeting.getLeaderUser().getNickname())
                        .build())
                .myRole(myRole)
                .roundCount(meeting.getRoundCount())
                .capacity(meeting.getCapacity())
                .currentMemberCount(meeting.getCurrentCount())
                .rounds(roundDetails)
                .currentRoundNo(meeting.getCurrentRound())
                .build();
    }

    private MyMeetingDetailResponse.RoundDetail toRoundDetail(
            MeetingRound round,
            Long userId,
            LocalDateTime now,
            Meeting meeting,
            MeetingMember myMember,
            List<MeetingRound> allRounds
    ) {
        // 1. dDay 계산
        LocalDate roundDate = round.getStartAt().toLocalDate();
        LocalDate today = LocalDate.now();
        int dDay = (int) ChronoUnit.DAYS.between(today, roundDate);

        // 2. 독후감 조회
        Optional<BookReport> bookReportOpt = bookReportRepository
                .findByUserIdAndMeetingRoundIdAndDeletedAtIsNull(userId, round.getId());

        // 3. 이전 회차 조회 (상태 판단용)
        MeetingRound prevRound = allRounds.stream()
                .filter(r -> r.getRoundNo() == round.getRoundNo() - 1)
                .findFirst()
                .orElse(null);

        // 4. BookReportInfo 생성
        MyMeetingDetailResponse.RoundDetail.BookReportInfo bookReportInfo;
        if (bookReportOpt.isPresent()) {
            BookReport bookReport = bookReportOpt.get();
            UserBookReportStatus status = BookReportStatusResolver.fromBookReportStatus(bookReport.getStatus());
            bookReportInfo = MyMeetingDetailResponse.RoundDetail.BookReportInfo.builder()
                    .status(status.name())
                    .id(bookReport.getId())
                    .build();
        } else {
            UserBookReportStatus status = BookReportStatusResolver.resolveNotSubmitted(now, round, prevRound);
            bookReportInfo = MyMeetingDetailResponse.RoundDetail.BookReportInfo.builder()
                    .status(status.name())
                    .build();
        }

        // 6. meetingLink 공개 여부 (10분 전부터)
        LocalDateTime tenMinutesBefore = round.getStartAt().minusMinutes(10);
        boolean isLinkAvailable = !now.isBefore(tenMinutesBefore) && now.isBefore(round.getEndAt());

        // 7. canJoinMeeting 판단
        boolean canJoinMeeting = isLinkAvailable &&
                bookReportOpt.isPresent() &&
                bookReportOpt.get().getStatus() == BookReportStatus.APPROVED;

        String meetingLink = canJoinMeeting ? round.getMeetingLink() : null;

        // 8. Book 정보
        Book book = round.getBook();
        MyMeetingDetailResponse.RoundDetail.BookInfo bookInfo =
                MyMeetingDetailResponse.RoundDetail.BookInfo.builder()
                        .title(book.getTitle())
                        .authors(book.getAuthors())
                        .publisher(book.getPublisher())
                        .thumbnailUrl(book.getThumbnailUrl())
                        .publishedAt(book.getPublishedAt())
                        .build();

        return MyMeetingDetailResponse.RoundDetail.builder()
                .roundId(round.getId())
                .roundNo(round.getRoundNo())
                .meetingDate(round.getStartAt())
                .dDay(dDay)
                .meetingLink(meetingLink)
                .canJoinMeeting(canJoinMeeting)
                .book(bookInfo)
                .bookReport(bookReportInfo)
                .build();
    }

    private MyMeetingItem toMyMeetingItem(MeetingListRow row, LocalDateTime now) {
        // Meeting 조회 (currentRound 필요)
        Meeting meeting = meetingRepository.findById(row.getMeetingId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEETING_NOT_FOUND));

        // 다음 회차 날짜 조회
        List<LocalDateTime> nextRounds = meetingRoundRepository.findNextRoundDate(row.getMeetingId(), now);
        LocalDate meetingDate = nextRounds.isEmpty() ? null : nextRounds.getFirst().toLocalDate();

        return MyMeetingItem.builder()
                .meetingId(row.getMeetingId())
                .meetingImagePath(imageUrlResolver.toUrl(row.getMeetingImagePath()))
                .title(row.getTitle())
                .readingGenreId(row.getReadingGenreId())
                .leaderNickname(row.getLeaderNickname())
                .currentRound(meeting.getCurrentRound())
                .meetingDate(meetingDate)
                .build();
    }

    // N+1 해결 버전: Map을 사용한 toMyMeetingItem
    private MyMeetingItem toMyMeetingItem(
            MeetingListRow row, 
            LocalDateTime now, 
            Map<Long, LocalDateTime> nextRoundMap
    ) {
        // Meeting 조회 (currentRound 필요)
        Meeting meeting = meetingRepository.findById(row.getMeetingId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEETING_NOT_FOUND));

        // Map에서 다음 회차 날짜 조회 (O(1))
        LocalDateTime nextRound = nextRoundMap.get(row.getMeetingId());
        LocalDate meetingDate = nextRound != null ? nextRound.toLocalDate() : null;

        return MyMeetingItem.builder()
                .meetingId(row.getMeetingId())
                .meetingImagePath(imageUrlResolver.toUrl(row.getMeetingImagePath()))
                .title(row.getTitle())
                .readingGenreId(row.getReadingGenreId())
                .leaderNickname(row.getLeaderNickname())
                .currentRound(meeting.getCurrentRound())
                .meetingDate(meetingDate)
                .build();
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

    private Book resolveBook(BookRequest request) {
        String isbn = request.isbn();

        return bookRepository.findByIsbn(isbn)
                .map(this::reviveIfDeleted)
                .orElseGet(() -> kakaoBookClient.searchByIsbn(isbn)
                        .map(doc -> bookRepository.save(toBookFromKakao(doc, isbn)))
                        .orElseThrow(() -> new BusinessException(ErrorCode.BOOK_NOT_FOUND)));
    }

    private Book reviveIfDeleted(Book existing) {
        if (existing.isDeleted()) {
            existing.revive();
        }
        return existing;
    }

    private Book toBookFromKakao(KakaoBookResponse.KakaoBookDocument doc, String isbn) {
        String authors = doc.authors() != null ? String.join(", ", doc.authors()) : null;
        LocalDate publishedAt = parsePublishedAt(doc.datetime());

        return Book.create(
                isbn,
                doc.title(),
                authors,
                doc.publisher(),
                doc.thumbnail(),
                publishedAt
        );
    }

    private LocalDate parsePublishedAt(String datetime) {
        if (datetime == null || datetime.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(datetime.substring(0, 10));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 조회된 모임 목록 중 모든 회차가 종료된 FINISHED 모임을 CANCELED로 변경
     */
    private void updateCompletedMeetingsStatus(List<MeetingListRow> rows, LocalDateTime now) {
        if (rows.isEmpty()) {
            return;
        }

        List<Long> meetingIds = rows.stream()
                .map(MeetingListRow::getMeetingId)
                .toList();

        List<Meeting> completedMeetings = meetingRepository.findCompletedMeetingsInIds(meetingIds, now);

        for (Meeting meeting : completedMeetings) {
            meeting.updateStatusToCanceled();
        }
    }

    /**
     * 단일 모임이 모든 회차 종료된 FINISHED 상태면 CANCELED로 변경
     */
    private void updateCompletedMeetingStatus(Meeting meeting, LocalDateTime now) {
        if (meeting.getStatus() != MeetingStatus.FINISHED) {
            return;
        }

        List<Meeting> completedMeetings = meetingRepository.findCompletedMeetingsInIds(
                List.of(meeting.getId()), now);

        if (!completedMeetings.isEmpty()) {
            meeting.updateStatusToCanceled();
        }
    }

    @Transactional
    public MeetingCreateResponse updateMeeting(Long userId, Long meetingId, MeetingUpdateRequest request) {
        LocalDateTime now = LocalDateTime.now();

        // 1. 모임 조회
        Meeting meeting = meetingRepository.findByIdWithLeader(meetingId)
                .filter(m -> m.getDeletedAt() == null)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEETING_NOT_FOUND));

        // 2. 권한 체크 (리더만 수정 가능)
        if (!meeting.isLeader(userId)) {
            throw new BusinessException(ErrorCode.MEETING_UPDATE_FORBIDDEN);
        }

        // 3. 상태 체크 (CANCELED 모임은 수정 불가)
        if (meeting.isCanceled()) {
            throw new BusinessException(ErrorCode.MEETING_UPDATE_NOT_ALLOWED);
        }

        // 4. readingGenre 유효성 체크
        if (!readingGenreRepository.existsByIdAndDeletedAtIsNull(request.readingGenreId())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        // 5. 기존 rounds 조회
        List<MeetingRound> existingRounds = meetingRoundRepository.findByMeetingIdWithBook(meetingId);

        // 6. 잠금 회차 식별 (DONE 또는 진행중)
        Set<Integer> lockedRoundNos = identifyLockedRounds(existingRounds, now);

        // 7. 잠금 회차 검증
        validateLockedRounds(lockedRoundNos, existingRounds, request.rounds());

        // 8. 잠금되지 않은 기존 회차 삭제
        List<MeetingRound> roundsToDelete = existingRounds.stream()
                .filter(r -> !lockedRoundNos.contains(r.getRoundNo()))
                .toList();
        meetingRoundRepository.deleteAll(roundsToDelete);

        // 9. 새 회차 생성 (잠금 회차 제외)
        LocalTime startTime = request.startTime();
        int durationMinutes = request.durationMinutes();

        List<MeetingRound> newRounds = request.rounds().stream()
                .filter(r -> !lockedRoundNos.contains(r.roundNo()))
                .map(round -> {
                    Book book = resolveBook(round.book());
                    LocalDateTime startAt = LocalDateTime.of(round.date(), startTime);
                    LocalDateTime endAt = startAt.plusMinutes(durationMinutes);
                    return MeetingRound.create(meeting, book, round.roundNo(), startAt, endAt);
                })
                .toList();
        meetingRoundRepository.saveAll(newRounds);

        // 10. Meeting 엔티티 업데이트
        LocalDate firstRoundDate = request.firstRoundAt();
        LocalDateTime firstRoundAt = LocalDateTime.of(firstRoundDate, startTime);
        MeetingDayOfWeek dayOfWeek = MeetingDayOfWeek.from(firstRoundDate);

        meeting.update(
                request.meetingImageKey(),
                request.title(),
                request.description(),
                request.readingGenreId(),
                request.capacity(),
                request.roundCount(),
                startTime,
                durationMinutes,
                request.recruitmentDeadline(),
                request.leaderIntro(),
                dayOfWeek,
                firstRoundAt
        );

        // 11. leaderIntroSavePolicy 처리
        if (Boolean.TRUE.equals(request.leaderIntroSavePolicy())) {
            User leader = meeting.getLeaderUser();
            leader.updateLeaderIntro(request.leaderIntro());
        }

        return new MeetingCreateResponse(meeting.getId());
    }

    /**
     * 잠금 회차 식별: DONE 상태이거나 진행중인 회차
     * 진행중 회차: 이전 회차 endAt ≤ now < 현재 회차 endAt
     * 1회차의 경우: startAt ≤ now < endAt
     */
    private Set<Integer> identifyLockedRounds(List<MeetingRound> rounds, LocalDateTime now) {
        Map<Integer, MeetingRound> roundMap = rounds.stream()
                .collect(Collectors.toMap(MeetingRound::getRoundNo, r -> r));

        return rounds.stream()
                .filter(round -> isRoundLocked(round, roundMap.get(round.getRoundNo() - 1), now))
                .map(MeetingRound::getRoundNo)
                .collect(Collectors.toSet());
    }

    private boolean isRoundLocked(MeetingRound round, MeetingRound prevRound, LocalDateTime now) {
        // DONE 상태면 잠금
        if (round.getStatus() == MeetingRoundStatus.DONE) {
            return true;
        }

        // 진행중 회차 판단
        // 1회차: startAt ≤ now < endAt
        // N회차: (N-1)회차 endAt ≤ now < N회차 endAt
        LocalDateTime progressStart = (prevRound != null) ? prevRound.getEndAt() : round.getStartAt();
        return !now.isBefore(progressStart) && now.isBefore(round.getEndAt());
    }

    /**
     * 잠금 회차 검증:
     * 1. 잠금된 roundNo가 요청에 반드시 존재해야 함 (삭제 불가)
     * 2. 잠금된 roundNo의 date, book.isbn이 DB와 동일해야 함
     */
    private void validateLockedRounds(
            Set<Integer> lockedRoundNos,
            List<MeetingRound> existingRounds,
            List<RoundRequest> requestRounds
    ) {
        if (lockedRoundNos.isEmpty()) {
            return;
        }

        Map<Integer, MeetingRound> existingMap = existingRounds.stream()
                .collect(Collectors.toMap(MeetingRound::getRoundNo, r -> r));

        Map<Integer, RoundRequest> requestMap = requestRounds.stream()
                .collect(Collectors.toMap(RoundRequest::roundNo, r -> r));

        for (Integer lockedRoundNo : lockedRoundNos) {
            // 잠금 회차가 요청에 없으면 삭제 시도 → 409
            if (!requestMap.containsKey(lockedRoundNo)) {
                throw new BusinessException(ErrorCode.MEETING_ROUND_UPDATE_NOT_ALLOWED);
            }

            MeetingRound existing = existingMap.get(lockedRoundNo);
            RoundRequest requested = requestMap.get(lockedRoundNo);

            // date 비교
            LocalDate existingDate = existing.getStartAt().toLocalDate();
            if (!existingDate.equals(requested.date())) {
                throw new BusinessException(ErrorCode.MEETING_ROUND_UPDATE_NOT_ALLOWED);
            }

            // book isbn 비교
            String existingIsbn = existing.getBook().getIsbn();
            if (!existingIsbn.equals(requested.book().isbn())) {
                throw new BusinessException(ErrorCode.MEETING_ROUND_UPDATE_NOT_ALLOWED);
            }
        }
    }

    @Transactional
    public ParticipationStatusUpdateResponse updateParticipationStatus(
            Long userId,
            Long meetingId,
            Long joinRequestId,
            ParticipationStatusUpdateRequest request
    ) {
        LocalDateTime now = LocalDateTime.now();

        // 1. 모임 조회
        Meeting meeting = meetingRepository.findByIdWithLeader(meetingId)
                .filter(m -> m.getDeletedAt() == null)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEETING_NOT_FOUND));

        // 2. 권한 체크 (모임장만 처리 가능)
        if (!meeting.isLeader(userId)) {
            throw new BusinessException(ErrorCode.PARTICIPATION_UPDATE_FORBIDDEN);
        }

        // 3. 참여 요청 조회
        MeetingMember joinRequest = meetingMemberRepository.findById(joinRequestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.JOIN_REQUEST_NOT_FOUND));

        // 4. 참여 요청이 해당 모임에 속하는지 확인
        if (!joinRequest.belongsTo(meetingId)) {
            throw new BusinessException(ErrorCode.JOIN_REQUEST_NOT_FOUND);
        }

        // 5. PENDING 상태인지 확인
        if (!joinRequest.isPending()) {
            throw new BusinessException(ErrorCode.JOIN_REQUEST_ALREADY_PROCESSED);
        }

        // 6. 상태 변경 처리
        MeetingMemberStatus newStatus = MeetingMemberStatus.valueOf(request.status());

        if (newStatus == MeetingMemberStatus.APPROVED) {
            // 정원 체크
            if (meeting.getCurrentCount() >= meeting.getCapacity()) {
                throw new BusinessException(ErrorCode.CAPACITY_FULL);
            }

            // 승인 처리
            joinRequest.approve(now);
            meeting.incrementCurrentCount();

            // 모집 완료 상태 확인 및 업데이트
            if (meeting.isRecruitmentClosed()) {
                meeting.updateStatusToFinished();
            }
        } else if (newStatus == MeetingMemberStatus.REJECTED) {
            // 거절 처리
            joinRequest.reject(now);
        }

        return ParticipationStatusUpdateResponse.builder()
                .meetingId(meetingId)
                .joinRequestId(joinRequestId)
                .status(newStatus.name())
                .build();
    }

    @Transactional
    public void cancelParticipation(Long userId, Long meetingId) {
        LocalDateTime now = LocalDateTime.now();

        // 1. 모임 존재 여부 확인
        if (!meetingRepository.existsByIdAndDeletedAtIsNull(meetingId)) {
            throw new BusinessException(ErrorCode.MEETING_NOT_FOUND);
        }

        // 2. 본인 참여 요청 조회
        MeetingMember myRequest = meetingMemberRepository.findByMeetingIdAndUserId(meetingId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.JOIN_REQUEST_NOT_FOUND));

        // 3. PENDING 상태인지 확인
        if (!myRequest.isPending()) {
            throw new BusinessException(ErrorCode.PARTICIPATION_CANCEL_NOT_ALLOWED);
        }

        // 4. 취소 처리 (status = LEFT, left_at = now)
        myRequest.cancel(now);
    }

    @Transactional
    public void leaveMeeting(Long userId, Long meetingId) {
        LocalDateTime now = LocalDateTime.now();

        // 1. 모임 조회
        Meeting meeting = meetingRepository.findByIdWithLeader(meetingId)
                .filter(m -> m.getDeletedAt() == null)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEETING_NOT_FOUND));

        // 2. 나의 멤버 정보 조회
        MeetingMember myMember = meetingMemberRepository.findByMeetingIdAndUserId(meetingId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEETING_MEMBER_NOT_FOUND));

        // 3. 모임장은 탈퇴 불가 (위임 후 탈퇴해야 함)
        if (myMember.isLeader()) {
            throw new BusinessException(ErrorCode.LEADER_CANNOT_LEAVE);
        }

        // 4. APPROVED 상태인지 확인
        if (!myMember.isApproved()) {
            throw new BusinessException(ErrorCode.LEAVE_NOT_ALLOWED);
        }

        // 5. 탈퇴 처리 (status = LEFT, left_at = now)
        myMember.cancel(now);

        // 6. 모임 인원 감소
        meeting.decrementCurrentCount();
    }

    @Transactional(readOnly = true)
    public MeetingMembersResponse getMeetingMembers(Long userId, Long meetingId) {
        // 1. 모임 조회
        Meeting meeting = meetingRepository.findByIdWithLeader(meetingId)
                .filter(m -> m.getDeletedAt() == null)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEETING_NOT_FOUND));

        // 2. 권한 체크 (모임장만)
        if (!meeting.isLeader(userId)) {
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
        }

        // 3. APPROVED 멤버 목록 조회
        List<MeetingMember> approvedMembers = meetingMemberRepository
                .findApprovedMembersByMeetingIdOrderByCreatedAt(meetingId);

        // 4. approvedAt 기준 정렬 (joinedAt)
        List<MeetingMember> sortedMembers = approvedMembers.stream()
                .sorted((m1, m2) -> {
                    if (m1.getApprovedAt() == null && m2.getApprovedAt() == null) return 0;
                    if (m1.getApprovedAt() == null) return 1;
                    if (m2.getApprovedAt() == null) return -1;
                    return m1.getApprovedAt().compareTo(m2.getApprovedAt());
                })
                .toList();

        // 5. 응답 생성
        List<MeetingMembersResponse.MemberInfo> memberInfos = sortedMembers.stream()
                .map(member -> MeetingMembersResponse.MemberInfo.builder()
                        .meetingMemberId(member.getId())
                        .nickname(member.getUser().getNickname())
                        .profileImagePath(imageUrlResolver.toUrl(member.getUser().getProfileImagePath()))
                        .joinedAt(toKstOffset(member.getApprovedAt()))
                        .build())
                .toList();

        return MeetingMembersResponse.builder()
                .meetingId(meetingId)
                .memberCount(memberInfos.size())
                .members(memberInfos)
                .build();
    }

    @Transactional
    public void kickMember(Long userId, Long meetingId, Long memberId) {
        LocalDateTime now = LocalDateTime.now();

        // 1. 모임 조회
        Meeting meeting = meetingRepository.findByIdWithLeader(meetingId)
                .filter(m -> m.getDeletedAt() == null)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEETING_NOT_FOUND));

        // 2. 권한 체크 (모임장만)
        if (!meeting.isLeader(userId)) {
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
        }

        // 3. 강퇴 대상 멤버 조회
        MeetingMember targetMember = meetingMemberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEETING_MEMBER_NOT_FOUND));

        // 4. 해당 모임의 멤버인지 확인
        if (!targetMember.belongsTo(meetingId)) {
            throw new BusinessException(ErrorCode.MEETING_MEMBER_NOT_FOUND);
        }

        // 5. 본인 강퇴 방지
        if (targetMember.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.CANNOT_KICK_SELF);
        }

        // 6. APPROVED 상태인지 확인
        if (!targetMember.isApproved()) {
            throw new BusinessException(ErrorCode.KICK_NOT_ALLOWED);
        }

        // 7. 강퇴 처리
        targetMember.kick(now);

        // 8. 모임 인원 감소
        meeting.decrementCurrentCount();
    }

    private OffsetDateTime toKstOffset(LocalDateTime time) {
        if (time == null) {
            return null;
        }
        return time.atZone(java.time.ZoneId.of("Asia/Seoul")).toOffsetDateTime();
    }
}

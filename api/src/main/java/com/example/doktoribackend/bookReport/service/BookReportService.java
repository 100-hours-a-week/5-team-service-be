package com.example.doktoribackend.bookReport.service;

import com.example.doktoribackend.bookReport.domain.BookReport;
import com.example.doktoribackend.bookReport.domain.BookReportStatus;
import com.example.doktoribackend.bookReport.domain.BookReportStatusResolver;
import com.example.doktoribackend.bookReport.domain.UserBookReportStatus;
import com.example.doktoribackend.bookReport.dto.BookReportCreateRequest;
import com.example.doktoribackend.bookReport.dto.BookReportCreateResponse;
import com.example.doktoribackend.bookReport.dto.BookReportDetailResponse;
import com.example.doktoribackend.bookReport.dto.BookReportManagementResponse;
import com.example.doktoribackend.bookReport.dto.BookReportProjection;
import com.example.doktoribackend.bookReport.dto.MemberBookReportDetailResponse;
import com.example.doktoribackend.bookReport.repository.BookReportRepository;
import com.example.doktoribackend.book.domain.Book;
import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.exception.BusinessException;
import com.example.doktoribackend.exception.UserNotFoundException;
import com.example.doktoribackend.meeting.domain.Meeting;
import com.example.doktoribackend.meeting.domain.MeetingMember;
import com.example.doktoribackend.meeting.domain.MeetingMemberStatus;
import com.example.doktoribackend.meeting.domain.MeetingRound;
import com.example.doktoribackend.meeting.repository.MeetingMemberRepository;
import com.example.doktoribackend.meeting.repository.MeetingRoundRepository;
import com.example.doktoribackend.user.domain.User;
import com.example.doktoribackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookReportService {

    private static final int DAILY_SUBMISSION_LIMIT = 3;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final BookReportRepository bookReportRepository;
    private final MeetingRoundRepository meetingRoundRepository;
    private final MeetingMemberRepository meetingMemberRepository;
    private final UserRepository userRepository;
    private final AiValidationService aiValidationService;

    @Transactional
    public BookReportCreateResponse createBookReport(Long userId, Long roundId, BookReportCreateRequest request) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(UserNotFoundException::new);

        MeetingRound meetingRound = meetingRoundRepository.findById(roundId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROUND_NOT_FOUND));

        Long meetingId = meetingRound.getMeeting().getId();

        boolean isMember = meetingMemberRepository.existsByMeetingIdAndUserIdAndStatus(
                meetingId, userId, MeetingMemberStatus.APPROVED);
        if (!isMember) {
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
        }

        MeetingRound prevRound = findPrevRound(meetingRound);
        UserBookReportStatus writeStatus = BookReportStatusResolver.resolveNotSubmitted(
                LocalDateTime.now(), meetingRound, prevRound);
        if (writeStatus != UserBookReportStatus.NOT_SUBMITTED) {
            throw new BusinessException(ErrorCode.BOOK_REPORT_NOT_WRITABLE);
        }

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        int todayCount = bookReportRepository.countTodaySubmissions(userId, startOfDay);
        if (todayCount >= DAILY_SUBMISSION_LIMIT) {
            throw new BusinessException(ErrorCode.DAILY_SUBMISSION_LIMIT_EXCEEDED);
        }

        Optional<BookReport> existingReport = bookReportRepository
                .findByUserIdAndMeetingRoundIdAndDeletedAtIsNull(userId, roundId);

        if (existingReport.isPresent()) {
            BookReport existing = existingReport.get();
            if (!existing.isResubmittable()) {
                throw new BusinessException(ErrorCode.BOOK_REPORT_ALREADY_SUBMITTED);
            }
            existing.softDelete();
            bookReportRepository.saveAndFlush(existing);
        }
        BookReport bookReport = BookReport.create(user, meetingRound, request.content());
        bookReportRepository.save(bookReport);

        aiValidationService.validate(bookReport.getId(), meetingRound.getBook().getTitle(), request.content());

        return new BookReportCreateResponse(meetingId);
    }

    @Transactional(readOnly = true)
    public BookReportDetailResponse getMyBookReport(Long userId, Long roundId) {
        MeetingRound meetingRound = meetingRoundRepository.findByIdWithBookAndMeeting(roundId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROUND_NOT_FOUND));

        Book book = meetingRound.getBook();

        Optional<BookReportProjection> bookReportOpt = bookReportRepository
                .findProjectionByUserIdAndMeetingRoundId(userId, roundId);

        BookReportDetailResponse.BookInfo bookInfo = new BookReportDetailResponse.BookInfo(
                book.getTitle(),
                book.getAuthors(),
                book.getPublisher(),
                book.getThumbnailUrl(),
                book.getPublishedAt()
        );

        BookReportDetailResponse.BookReportInfo bookReportInfo;
        if (bookReportOpt.isPresent()) {
            BookReportProjection projection = bookReportOpt.get();
            UserBookReportStatus status = BookReportStatusResolver.fromBookReportStatus(projection.getStatus());
            bookReportInfo = new BookReportDetailResponse.BookReportInfo(
                    projection.getId(),
                    status.name(),
                    projection.getContent(),
                    projection.getRejectionReason()
            );
        } else {
            MeetingRound prevRound = findPrevRound(meetingRound);
            UserBookReportStatus status = BookReportStatusResolver.resolveNotSubmitted(
                    LocalDateTime.now(), meetingRound, prevRound);
            bookReportInfo = new BookReportDetailResponse.BookReportInfo(
                    null,
                    status.name(),
                    null,
                    null
            );
        }

        return new BookReportDetailResponse(bookInfo, bookReportInfo);
    }

    @Transactional(readOnly = true)
    public BookReportManagementResponse getBookReportManagement(Long userId, Long roundId) {
        // 1. 회차 조회 (모임 정보 포함)
        MeetingRound meetingRound = meetingRoundRepository.findByIdWithBookAndMeeting(roundId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROUND_NOT_FOUND));

        Meeting meeting = meetingRound.getMeeting();
        Long meetingId = meeting.getId();
        int roundNo = meetingRound.getRoundNo();

        // 2. 권한 체크 (모임장만 조회 가능)
        if (!meeting.isLeader(userId)) {
            throw new BusinessException(ErrorCode.BOOK_REPORT_MANAGEMENT_FORBIDDEN);
        }

        // 3. APPROVED 멤버 목록 조회 (모임장 포함)
        List<MeetingMember> approvedMembers = meetingMemberRepository
                .findApprovedMembersByMeetingIdOrderByCreatedAt(meetingId);

        // 4. 해당 회차의 독후감 목록 조회
        List<BookReport> bookReports = bookReportRepository.findByMeetingRoundId(roundId);
        Map<Long, BookReport> bookReportByUserId = bookReports.stream()
                .collect(Collectors.toMap(
                        br -> br.getUser().getId(),
                        br -> br
                ));

        // 5. 각 멤버의 APPROVED 독후감 수 일괄 조회 (N+1 방지)
        Map<Long, Long> approvedCountByUserId = bookReportRepository
                .countApprovedByMeetingIdGroupByUser(meetingId)
                .stream()
                .collect(Collectors.toMap(
                        BookReportRepository.MemberApprovedCountProjection::getUserId,
                        BookReportRepository.MemberApprovedCountProjection::getApprovedCount
                ));

        // 6. 멤버별 정보 조합
        List<BookReportManagementResponse.MemberBookReportInfo> memberInfos = approvedMembers.stream()
                .map(member -> {
                    Long memberUserId = member.getUser().getId();
                    BookReport bookReport = bookReportByUserId.get(memberUserId);

                    // bookReport 정보
                    BookReportManagementResponse.BookReportInfo bookReportInfo = null;
                    if (bookReport != null) {
                        bookReportInfo = BookReportManagementResponse.BookReportInfo.builder()
                                .id(bookReport.getId())
                                .status(bookReport.getStatus().name())
                                .submittedAt(toKstOffset(bookReport.getCreatedAt()))
                                .build();
                    }

                    // submissionRate 계산: (APPROVED 수 / roundNo) * 100
                    long approvedCount = approvedCountByUserId.getOrDefault(memberUserId, 0L);
                    int submissionRate = (int) Math.round((double) approvedCount / roundNo * 100);

                    return BookReportManagementResponse.MemberBookReportInfo.builder()
                            .meetingMemberId(member.getId())
                            .nickname(member.getUser().getNickname())
                            .bookReport(bookReportInfo)
                            .submissionRate(submissionRate)
                            .build();
                })
                .toList();

        // 7. 정렬: APPROVED 먼저, submittedAt ASC, 미제출은 nickname ASC
        List<BookReportManagementResponse.MemberBookReportInfo> sortedMembers = memberInfos.stream()
                .sorted(getBookReportManagementComparator())
                .toList();

        // 8. submittedCount 계산 (APPROVED 상태인 독후감 수)
        int submittedCount = (int) bookReports.stream()
                .filter(br -> br.getStatus() == BookReportStatus.APPROVED)
                .count();

        return BookReportManagementResponse.builder()
                .roundNo(roundNo)
                .submittedCount(submittedCount)
                .totalCount(approvedMembers.size())
                .members(sortedMembers)
                .build();
    }

    /**
     * 정렬 기준:
     * 1. APPROVED 상태 먼저
     * 2. submittedAt ASC (제출 시간 순)
     * 3. 미제출은 nickname ASC
     */
    private Comparator<BookReportManagementResponse.MemberBookReportInfo> getBookReportManagementComparator() {
        return (m1, m2) -> {
            boolean m1Approved = m1.bookReport() != null
                    && BookReportStatus.APPROVED.name().equals(m1.bookReport().status());
            boolean m2Approved = m2.bookReport() != null
                    && BookReportStatus.APPROVED.name().equals(m2.bookReport().status());

            // APPROVED 먼저
            if (m1Approved && !m2Approved) {
                return -1;
            }
            if (!m1Approved && m2Approved) {
                return 1;
            }

            // 둘 다 APPROVED면 submittedAt ASC
            if (m1Approved && m2Approved) {
                OffsetDateTime t1 = m1.bookReport().submittedAt();
                OffsetDateTime t2 = m2.bookReport().submittedAt();
                if (t1 != null && t2 != null) {
                    return t1.compareTo(t2);
                }
            }

            // 미제출이거나 같은 조건이면 nickname ASC
            return m1.nickname().compareTo(m2.nickname());
        };
    }

    private MeetingRound findPrevRound(MeetingRound meetingRound) {
        int roundNo = meetingRound.getRoundNo();
        if (roundNo <= 1) {
            return null;
        }
        return meetingRoundRepository.findByMeetingIdAndRoundNo(
                meetingRound.getMeeting().getId(), roundNo - 1
        ).orElse(null);
    }

    private OffsetDateTime toKstOffset(LocalDateTime time) {
        if (time == null) {
            return null;
        }
        return time.atZone(KST).toOffsetDateTime();
    }

    @Transactional(readOnly = true)
    public MemberBookReportDetailResponse getMemberBookReport(Long userId, Long roundId, Long bookReportId) {
        // 1. 회차 조회 (모임, 책 정보 포함)
        MeetingRound meetingRound = meetingRoundRepository.findByIdWithBookAndMeeting(roundId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROUND_NOT_FOUND));

        Meeting meeting = meetingRound.getMeeting();
        Long meetingId = meeting.getId();

        // 2. 권한 체크 (모임장만 조회 가능)
        if (!meeting.isLeader(userId)) {
            throw new BusinessException(ErrorCode.BOOK_REPORT_MANAGEMENT_FORBIDDEN);
        }

        // 3. 독후감 조회
        BookReport bookReport = bookReportRepository.findById(bookReportId)
                .filter(br -> br.getDeletedAt() == null)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOK_REPORT_NOT_FOUND));

        // 4. 독후감이 해당 회차에 속하는지 확인
        if (!bookReport.getMeetingRound().getId().equals(roundId)) {
            throw new BusinessException(ErrorCode.BOOK_REPORT_NOT_FOUND);
        }

        // 5. 작성자의 MeetingMember 정보 조회
        Long writerUserId = bookReport.getUser().getId();
        MeetingMember writerMember = meetingMemberRepository.findByMeetingIdAndUserId(meetingId, writerUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOK_REPORT_NOT_FOUND));

        // 6. 책 정보
        Book book = meetingRound.getBook();
        MemberBookReportDetailResponse.BookInfo bookInfo = MemberBookReportDetailResponse.BookInfo.builder()
                .title(book.getTitle())
                .authors(book.getAuthors())
                .publisher(book.getPublisher())
                .thumbnailUrl(book.getThumbnailUrl())
                .publishedAt(book.getPublishedAt())
                .build();

        // 7. 작성자 정보
        MemberBookReportDetailResponse.WriterInfo writerInfo = MemberBookReportDetailResponse.WriterInfo.builder()
                .meetingMemberId(writerMember.getId())
                .nickname(bookReport.getUser().getNickname())
                .build();

        // 8. 독후감 정보
        MemberBookReportDetailResponse.BookReportInfo bookReportInfo = MemberBookReportDetailResponse.BookReportInfo.builder()
                .id(bookReport.getId())
                .status(bookReport.getStatus().name())
                .content(bookReport.getContent())
                .rejectionReason(bookReport.getRejectionReason())
                .build();

        return MemberBookReportDetailResponse.builder()
                .book(bookInfo)
                .writer(writerInfo)
                .bookReport(bookReportInfo)
                .build();
    }
}

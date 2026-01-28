package com.example.doktoribackend.bookReport.service;

import com.example.doktoribackend.bookReport.domain.BookReport;
import com.example.doktoribackend.bookReport.dto.BookReportCreateRequest;
import com.example.doktoribackend.bookReport.dto.BookReportCreateResponse;
import com.example.doktoribackend.bookReport.dto.BookReportDetailResponse;
import com.example.doktoribackend.bookReport.repository.BookReportRepository;
import com.example.doktoribackend.book.domain.Book;
import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.exception.BusinessException;
import com.example.doktoribackend.exception.UserNotFoundException;
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
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BookReportService {

    private static final int DAILY_SUBMISSION_LIMIT = 50;

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

        if (!isWritablePeriod(meetingRound)) {
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

        String bookTitle = meetingRound.getBook().getTitle();
        aiValidationService.validate(bookReport.getId(), bookTitle, request.content());

        return new BookReportCreateResponse(meetingId);
    }

    @Transactional(readOnly = true)
    public BookReportDetailResponse getMyBookReport(Long userId, Long roundId) {
        MeetingRound meetingRound = meetingRoundRepository.findById(roundId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROUND_NOT_FOUND));

        Book book = meetingRound.getBook();

        Optional<BookReport> bookReportOpt = bookReportRepository
                .findByUserIdAndMeetingRoundIdAndDeletedAtIsNull(userId, roundId);

        BookReportDetailResponse.BookInfo bookInfo = new BookReportDetailResponse.BookInfo(
                book.getTitle(),
                book.getAuthors(),
                book.getPublisher(),
                book.getThumbnailUrl(),
                book.getPublishedAt()
        );

        BookReportDetailResponse.BookReportInfo bookReportInfo;
        if (bookReportOpt.isPresent()) {
            BookReport bookReport = bookReportOpt.get();
            bookReportInfo = new BookReportDetailResponse.BookReportInfo(
                    bookReport.getId(),
                    bookReport.getStatus().name(),
                    bookReport.getContent(),
                    bookReport.getRejectionReason()
            );
        } else {
            String status = isWritablePeriod(meetingRound) ? "NOT_SUBMITTED" : "DEADLINE_PASSED";
            bookReportInfo = new BookReportDetailResponse.BookReportInfo(
                    null,
                    status,
                    null,
                    null
            );
        }

        return new BookReportDetailResponse(bookInfo, bookReportInfo);
    }

    private boolean isWritablePeriod(MeetingRound meetingRound) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = meetingRound.getStartAt().minusHours(24);

        if (now.isAfter(deadline)) {
            return false;
        }

        int roundNo = meetingRound.getRoundNo();

        if (roundNo == 1) {
            return true;
        }

        Optional<MeetingRound> prevRoundOpt = meetingRoundRepository.findByMeetingIdAndRoundNo(
                meetingRound.getMeeting().getId(), roundNo - 1);

        if (prevRoundOpt.isEmpty()) {
            return false;
        }

        MeetingRound prevRound = prevRoundOpt.get();
        LocalDateTime startDate = prevRound.getEndAt().toLocalDate().plusDays(1).atStartOfDay();

        return !now.isBefore(startDate);
    }
}

package com.example.doktoribackend.meeting.service;

import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.common.s3.ImageUrlResolver;
import com.example.doktoribackend.exception.BusinessException;
import com.example.doktoribackend.meeting.domain.Meeting;
import com.example.doktoribackend.meeting.domain.MeetingBookmark;
import com.example.doktoribackend.meeting.domain.MeetingStatus;
import com.example.doktoribackend.meeting.dto.BookmarkedMeetingItem;
import com.example.doktoribackend.meeting.dto.BookmarkedMeetingListResponse;
import com.example.doktoribackend.meeting.dto.PageInfo;
import com.example.doktoribackend.meeting.repository.MeetingBookmarkRepository;
import com.example.doktoribackend.meeting.repository.MeetingRepository;
import com.example.doktoribackend.user.domain.User;
import com.example.doktoribackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MeetingBookmarkService {

    private final MeetingBookmarkRepository meetingBookmarkRepository;
    private final MeetingRepository meetingRepository;
    private final UserRepository userRepository;
    private final ImageUrlResolver imageUrlResolver;

    @Transactional
    public void addBookmark(Long userId, Long meetingId) {
        // 1. 사용자 조회
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 2. 모임 조회 및 검증 (조회 가능한 모집중 모임만)
        Meeting meeting = findBookmarkableMeeting(meetingId);

        // 3. 중복 북마크 확인
        if (meetingBookmarkRepository.existsByUserIdAndMeetingId(userId, meetingId)) {
            throw new BusinessException(ErrorCode.BOOKMARK_ALREADY_EXISTS);
        }

        // 4. 북마크 저장
        MeetingBookmark bookmark = MeetingBookmark.create(user, meeting);
        meetingBookmarkRepository.save(bookmark);
    }

    @Transactional
    public void removeBookmark(Long userId, Long meetingId) {
        // 1. 모임 조회 및 검증 (조회 가능한 모집중 모임만)
        findBookmarkableMeeting(meetingId);

        // 2. 북마크 삭제 (멱등: 없어도 정상 처리)
        meetingBookmarkRepository.deleteByUserIdAndMeetingId(userId, meetingId);
    }

    @Transactional(readOnly = true)
    public BookmarkedMeetingListResponse getBookmarkedMeetings(Long userId, Long cursorId, int size) {
        // 1. 페이지네이션 조회 (size + 1로 hasNext 판단)
        List<MeetingBookmark> bookmarks = meetingBookmarkRepository.findByUserIdWithCursor(
                userId, cursorId, PageRequest.of(0, size + 1));

        // 2. hasNext 판단
        boolean hasNext = bookmarks.size() > size;
        List<MeetingBookmark> content = hasNext ? bookmarks.subList(0, size) : bookmarks;

        // 3. DTO 변환
        List<BookmarkedMeetingItem> items = content.stream()
                .map(this::toBookmarkedMeetingItem)
                .toList();

        // 4. 다음 커서 설정
        Long nextCursorId = hasNext ? content.getLast().getId() : null;
        PageInfo pageInfo = new PageInfo(nextCursorId, hasNext, size);

        return new BookmarkedMeetingListResponse(items, pageInfo);
    }

    private BookmarkedMeetingItem toBookmarkedMeetingItem(MeetingBookmark bookmark) {
        Meeting meeting = bookmark.getMeeting();
        boolean isRecruiting = meeting.getStatus() == MeetingStatus.RECRUITING;

        return BookmarkedMeetingItem.builder()
                .meetingId(meeting.getId())
                .meetingImagePath(imageUrlResolver.toUrl(meeting.getMeetingImagePath()))
                .title(meeting.getTitle())
                .readingGenreName(meeting.getReadingGenre().getName())
                .leaderNickname(meeting.getLeaderUser().getNickname())
                .currentMemberCount(meeting.getCurrentCount())
                .capacity(meeting.getCapacity())
                .isRecruiting(isRecruiting)
                .build();
    }

    /**
     * 북마크 가능한 모임 조회
     * - soft delete 되지 않음
     * - RECRUITING 상태
     */
    private Meeting findBookmarkableMeeting(Long meetingId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .filter(m -> m.getDeletedAt() == null)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEETING_NOT_FOUND));

        if (meeting.getStatus() != MeetingStatus.RECRUITING) {
            throw new BusinessException(ErrorCode.MEETING_NOT_FOUND);
        }

        return meeting;
    }

    @Transactional(readOnly = true)
    public Boolean getBookmarkStatus(Long userId, Long meetingId) {
        // 1. 모임 존재 여부 확인 (soft delete 체크)
        boolean meetingExists = meetingRepository.findById(meetingId)
                .filter(m -> m.getDeletedAt() == null)
                .isPresent();

        if (!meetingExists) {
            throw new BusinessException(ErrorCode.MEETING_NOT_FOUND);
        }

        // 2. 비로그인이면 null 반환
        if (userId == null) {
            return null;
        }

        // 3. 북마크 여부 확인
        return meetingBookmarkRepository.existsByUserIdAndMeetingId(userId, meetingId);
    }
}
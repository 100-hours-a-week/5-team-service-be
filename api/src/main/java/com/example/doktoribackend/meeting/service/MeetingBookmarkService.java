package com.example.doktoribackend.meeting.service;

import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.exception.BusinessException;
import com.example.doktoribackend.meeting.domain.Meeting;
import com.example.doktoribackend.meeting.domain.MeetingBookmark;
import com.example.doktoribackend.meeting.domain.MeetingStatus;
import com.example.doktoribackend.meeting.repository.MeetingBookmarkRepository;
import com.example.doktoribackend.meeting.repository.MeetingRepository;
import com.example.doktoribackend.user.domain.User;
import com.example.doktoribackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MeetingBookmarkService {

    private final MeetingBookmarkRepository meetingBookmarkRepository;
    private final MeetingRepository meetingRepository;
    private final UserRepository userRepository;

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
}
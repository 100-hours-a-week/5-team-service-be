package com.example.doktoribackend.meeting.service;

import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.exception.BusinessException;
import com.example.doktoribackend.meeting.domain.Meeting;
import com.example.doktoribackend.meeting.domain.MeetingMember;
import com.example.doktoribackend.meeting.dto.LeaderDelegationRequest;
import com.example.doktoribackend.meeting.dto.LeaderDelegationResponse;
import com.example.doktoribackend.meeting.repository.MeetingMemberRepository;
import com.example.doktoribackend.meeting.repository.MeetingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LeaderDelegationService {

    private final MeetingRepository meetingRepository;
    private final MeetingMemberRepository meetingMemberRepository;

    @Transactional
    public LeaderDelegationResponse delegateLeader(
            Long userId,
            Long meetingId,
            LeaderDelegationRequest request
    ) {
        // 1. 모임 조회
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEETING_NOT_FOUND));

        // 2. 요청자의 MeetingMember 조회
        MeetingMember currentLeaderMember = meetingMemberRepository.findByMeetingIdAndUserId(meetingId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LEADER_DELEGATION_FORBIDDEN));

        // 3. 요청자가 LEADER인지 확인
        if (!currentLeaderMember.isLeader()) {
            throw new BusinessException(ErrorCode.LEADER_DELEGATION_FORBIDDEN);
        }

        // 4. 위임 대상 MeetingMember 조회 (해당 모임에 속하는지 함께 검증)
        MeetingMember newLeaderMember = meetingMemberRepository
                .findByIdAndMeetingIdWithUser(request.newLeaderMeetingMemberId(), meetingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEETING_MEMBER_NOT_FOUND));

        // 5. 자기 자신에게 위임 불가
        if (currentLeaderMember.getId().equals(newLeaderMember.getId())) {
            throw new BusinessException(ErrorCode.CANNOT_DELEGATE_TO_SELF);
        }

        // 6. 위임 대상이 APPROVED 상태인지 확인
        if (!newLeaderMember.isApproved()) {
            throw new BusinessException(ErrorCode.DELEGATION_TARGET_NOT_APPROVED);
        }

        // 7. 위임 대상이 이미 LEADER인지 확인
        if (newLeaderMember.isLeader()) {
            throw new BusinessException(ErrorCode.DELEGATION_TARGET_ALREADY_LEADER);
        }

        // 8. 원자적 변경
        currentLeaderMember.demoteToMember();
        newLeaderMember.promoteToLeader();
        meeting.changeLeader(newLeaderMember.getUser());

        // 9. 응답 반환
        return LeaderDelegationResponse.builder()
                .meetingId(meetingId)
                .leaderMeetingMemberId(newLeaderMember.getId())
                .build();
    }
}

package com.example.doktoribackend.meeting.service;

import com.example.doktoribackend.meeting.dto.LeaderDelegationRequest;
import com.example.doktoribackend.meeting.dto.LeaderDelegationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LeaderDelegationService {

    public LeaderDelegationResponse delegateLeader(
            Long userId,
            Long meetingId,
            LeaderDelegationRequest request
    ) {
        // TODO: 커밋 2에서 구현
        throw new UnsupportedOperationException("Not implemented yet");
    }
}

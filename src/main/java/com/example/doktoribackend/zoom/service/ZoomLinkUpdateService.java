package com.example.doktoribackend.zoom.service;

import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.exception.BusinessException;
import com.example.doktoribackend.meeting.domain.MeetingRound;
import com.example.doktoribackend.meeting.repository.MeetingRoundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ZoomLinkUpdateService {

    private final MeetingRoundRepository meetingRoundRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveMeetingLink(Long meetingRoundId, String meetingLink) {
        MeetingRound meetingRound = meetingRoundRepository.findById(meetingRoundId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEETING_NOT_FOUND));

        meetingRound.updateMeetingLink(meetingLink);
        meetingRoundRepository.save(meetingRound);
    }
}

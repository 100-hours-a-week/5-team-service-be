package com.example.doktoribackend.meeting.dto;

import com.example.doktoribackend.meeting.domain.MeetingMember;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ParticipantsPreview {
    private Integer previewCount;
    private List<String> profileImages;
    private String myParticipationStatus;

    public static ParticipantsPreview from(
            List<MeetingMember> approvedMembers,
            String myParticipationStatus
    ) {
        List<String> profileImages = approvedMembers.stream()
                .map(mm -> mm.getUser().getProfileImagePath())
                .toList();

        return ParticipantsPreview.builder()
                .previewCount(approvedMembers.size())
                .profileImages(profileImages)
                .myParticipationStatus(myParticipationStatus)
                .build();
    }
}
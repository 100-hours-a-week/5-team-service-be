package com.example.doktoribackend.meeting.dto;

import java.util.List;

public record OtherMembersResponse(
        List<MemberInfo> members
) {
    public record MemberInfo(
            Long userId,
            String nickname,
            String profileImageUrl
    ) {}
}

package com.example.doktoribackend.room.dto;

import com.example.doktoribackend.common.s3.ImageUrlResolver;
import com.example.doktoribackend.room.domain.ChattingRoomMember;

public record ChatStartMemberItem(Long userId, String nickname, String profileImageUrl) {

    public static ChatStartMemberItem from(ChattingRoomMember member, ImageUrlResolver resolver) {
        return new ChatStartMemberItem(
                member.getUserId(),
                member.getNickname(),
                resolver.toUrl(member.getProfileImageUrl())
        );
    }
}

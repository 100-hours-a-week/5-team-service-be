package com.example.doktoribackend.room.dto;

import com.example.doktoribackend.common.s3.ImageUrlResolver;
import com.example.doktoribackend.room.domain.ChattingRoomMember;
import com.example.doktoribackend.room.domain.MemberRole;
import com.example.doktoribackend.room.domain.Position;

public record WaitingRoomMemberItem(
        String nickname,
        String profileImageUrl,
        Position position,
        MemberRole role
) {

    public static WaitingRoomMemberItem from(ChattingRoomMember member, ImageUrlResolver imageUrlResolver) {
        return new WaitingRoomMemberItem(
                member.getNickname(),
                imageUrlResolver.toUrl(member.getProfileImageUrl()),
                member.getPosition(),
                member.getRole()
        );
    }
}

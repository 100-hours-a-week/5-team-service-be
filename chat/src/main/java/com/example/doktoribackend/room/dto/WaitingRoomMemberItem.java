package com.example.doktoribackend.room.dto;

import com.example.doktoribackend.room.domain.ChattingRoomMember;
import com.example.doktoribackend.room.domain.MemberRole;
import com.example.doktoribackend.room.domain.Position;

public record WaitingRoomMemberItem(
        String nickname,
        String profileImageUrl,
        Position position,
        MemberRole role
) {

    public static WaitingRoomMemberItem from(ChattingRoomMember member) {
        return new WaitingRoomMemberItem(
                member.getNickname(),
                member.getProfileImageUrl(),
                member.getPosition(),
                member.getRole()
        );
    }
}

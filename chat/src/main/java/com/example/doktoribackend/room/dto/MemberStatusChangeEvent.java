package com.example.doktoribackend.room.dto;

public record MemberStatusChangeEvent(
        String type,
        Long userId,
        String nickname,
        String status
) {}

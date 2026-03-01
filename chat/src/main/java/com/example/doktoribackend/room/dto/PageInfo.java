package com.example.doktoribackend.room.dto;

public record PageInfo(Long nextCursorId, boolean hasNext, int size) {}

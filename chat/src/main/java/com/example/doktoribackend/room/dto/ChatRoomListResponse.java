package com.example.doktoribackend.room.dto;

import java.util.List;

public record ChatRoomListResponse(List<ChatRoomListItem> items, PageInfo pageInfo) {}

package com.example.doktoribackend.message.dto;

import com.example.doktoribackend.room.dto.PageInfo;

import java.util.List;

public record MessageListResponse(List<MessageResponse> messages, PageInfo pageInfo) {}

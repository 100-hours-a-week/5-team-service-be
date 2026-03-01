package com.example.doktoribackend.message.dto;

import com.example.doktoribackend.message.domain.MessageType;

public record MessageSendRequest(
        String clientMessageId,
        MessageType messageType,
        String textMessage,
        String filePath
) {}

package com.example.doktoribackend.message.controller;

import com.example.doktoribackend.message.dto.MessageResponse;
import com.example.doktoribackend.message.dto.MessageSendRequest;
import com.example.doktoribackend.message.service.ChatRoomRedisPublisher;
import com.example.doktoribackend.message.service.MessageService;
import com.example.doktoribackend.security.CustomUserDetails;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Slf4j
@Controller
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final ChatRoomRedisPublisher chatRoomRedisPublisher;

    @Timed(value = "chat.message.send", description = "Time to process and broadcast a chat message")
    @MessageMapping("/chat-rooms/{roomId}/messages")
    public void sendMessage(@DestinationVariable Long roomId,
                            @Payload MessageSendRequest request,
                            Principal principal) {
        UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) principal;
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();

        MessageResponse response = messageService.sendMessage(
                roomId, userDetails.getId(), userDetails.getNickname(), request);

        log.info("[Chat] 메시지 저장 완료, Redis 발행: roomId={}, messageId={}", roomId, response.messageId());
        chatRoomRedisPublisher.publish(roomId, response);
    }
}

package com.example.doktoribackend.message.controller;

import com.example.doktoribackend.message.dto.MessageResponse;
import com.example.doktoribackend.message.dto.MessageSendRequest;
import com.example.doktoribackend.message.service.MessageService;
import com.example.doktoribackend.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat-rooms/{roomId}/messages")
    public void sendMessage(@DestinationVariable Long roomId,
                            @Payload MessageSendRequest request,
                            Principal principal) {
        UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) principal;
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();

        MessageResponse response = messageService.sendMessage(
                roomId, userDetails.getId(), userDetails.getNickname(), request);

        if (response != null) {
            messagingTemplate.convertAndSend("/topic/chat-rooms/" + roomId, response);
        }
    }
}

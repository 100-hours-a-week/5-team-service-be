package com.example.doktoribackend.room.controller;

import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.common.response.ApiResult;
import com.example.doktoribackend.exception.BusinessException;
import com.example.doktoribackend.room.dto.ChatRoomCreateRequest;
import com.example.doktoribackend.room.dto.ChatRoomCreateResponse;
import com.example.doktoribackend.room.dto.ChatRoomJoinRequest;
import com.example.doktoribackend.room.dto.ChatRoomListResponse;
import com.example.doktoribackend.room.dto.ChatRoomStartResponse;
import com.example.doktoribackend.room.dto.WaitingRoomResponse;
import com.example.doktoribackend.room.service.ChatRoomService;
import com.example.doktoribackend.room.service.WaitingRoomSseService;
import com.example.doktoribackend.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chat-rooms")
public class ChatRoomController implements ChatRoomApi {

    private static final int MAX_SIZE = 20;

    private final ChatRoomService chatRoomService;
    private final WaitingRoomSseService waitingRoomSseService;

    @GetMapping
    @Override
    public ResponseEntity<ApiResult<ChatRoomListResponse>> getChatRooms(
            @RequestParam(required = false) Long cursorId,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        validateSize(size);
        validateCursorId(cursorId);

        ChatRoomListResponse response = chatRoomService.getChatRooms(cursorId, size);
        return ResponseEntity.ok(ApiResult.ok(response));
    }

    @PostMapping
    @Override
    public ResponseEntity<ApiResult<ChatRoomCreateResponse>> createChatRoom(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ChatRoomCreateRequest request
    ) {
        ChatRoomCreateResponse response = chatRoomService.createChatRoom(
                userDetails.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResult.ok(response));
    }

    @PostMapping("/{roomId}/members")
    @Override
    public ResponseEntity<ApiResult<WaitingRoomResponse>> joinChatRoom(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long roomId,
            @Valid @RequestBody ChatRoomJoinRequest request
    ) {
        WaitingRoomResponse response = chatRoomService.joinChatRoom(
                roomId, userDetails.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResult.ok(response));
    }

    @GetMapping("/{roomId}/waiting-room")
    @Override
    public ResponseEntity<ApiResult<WaitingRoomResponse>> getWaitingRoom(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long roomId
    ) {
        WaitingRoomResponse response = chatRoomService.getWaitingRoom(
                roomId, userDetails.getId());
        return ResponseEntity.ok(ApiResult.ok(response));
    }

    @GetMapping(value = "/{roomId}/waiting-room/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Override
    public SseEmitter subscribeWaitingRoom(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long roomId
    ) {
        chatRoomService.getWaitingRoom(roomId, userDetails.getId());
        return waitingRoomSseService.subscribe(roomId);
    }

    @PatchMapping("/{roomId}")
    @Override
    public ResponseEntity<ApiResult<ChatRoomStartResponse>> startChatRoom(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long roomId
    ) {
        ChatRoomStartResponse response = chatRoomService.startChatRoom(roomId, userDetails.getId());
        return ResponseEntity.ok(ApiResult.ok(response));
    }

    @DeleteMapping("/{roomId}/members/me")
    @Override
    public ResponseEntity<Void> leaveChatRoom(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long roomId
    ) {
        chatRoomService.leaveChatRoom(roomId, userDetails.getId());
        return ResponseEntity.noContent().build();
    }

    private void validateSize(int size) {
        if (size < 1 || size > MAX_SIZE) {
            throw new BusinessException(ErrorCode.PAGINATION_SIZE_OUT_OF_RANGE);
        }
    }

    private void validateCursorId(Long cursorId) {
        if (cursorId != null && cursorId < 1) {
            throw new BusinessException(ErrorCode.PAGINATION_INVALID_CURSOR);
        }
    }
}

package com.example.doktoribackend.room.service;

import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.common.s3.ImageUrlResolver;
import com.example.doktoribackend.exception.BusinessException;
import com.example.doktoribackend.room.domain.ChattingRoom;
import com.example.doktoribackend.room.domain.ChattingRoomMember;
import com.example.doktoribackend.room.domain.MemberStatus;
import com.example.doktoribackend.room.domain.Position;
import com.example.doktoribackend.room.domain.RoomRound;
import com.example.doktoribackend.room.domain.RoomStatus;
import com.example.doktoribackend.room.dto.ChatRoomListItem;
import com.example.doktoribackend.room.dto.ChatRoomListResponse;
import com.example.doktoribackend.room.dto.ChatRoomStartResponse;
import com.example.doktoribackend.room.dto.ChatStartMemberItem;
import com.example.doktoribackend.room.dto.PageInfo;
import com.example.doktoribackend.room.dto.WaitingRoomMemberItem;
import com.example.doktoribackend.room.dto.WaitingRoomResponse;
import com.example.doktoribackend.room.repository.ChattingRoomMemberRepository;
import com.example.doktoribackend.room.repository.ChattingRoomRepository;
import com.example.doktoribackend.room.repository.RoomRoundRepository;
import com.example.doktoribackend.summary.dto.ChatRoomSummaryResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomQueryService {

    private static final List<MemberStatus> ACTIVE_STATUSES =
            List.of(MemberStatus.WAITING, MemberStatus.JOINED, MemberStatus.DISCONNECTED);

    private final ChattingRoomRepository chattingRoomRepository;
    private final ChattingRoomMemberRepository chattingRoomMemberRepository;
    private final RoomRoundRepository roomRoundRepository;
    private final ImageUrlResolver imageUrlResolver;
    private final ObjectMapper objectMapper;

    public ChatRoomListResponse getChatRooms(Long cursorId, int size) {
        List<ChattingRoom> rooms = chattingRoomRepository.findByStatusWithCursor(
                RoomStatus.WAITING, cursorId, PageRequest.of(0, size + 1));
        boolean hasNext = rooms.size() > size;
        List<ChattingRoom> content = hasNext ? rooms.subList(0, size) : rooms;

        List<ChatRoomListItem> items = content.stream()
                .map(ChatRoomListItem::from)
                .toList();

        Long nextCursorId = hasNext ? content.getLast().getId() : null;
        PageInfo pageInfo = new PageInfo(nextCursorId, hasNext, size);

        return new ChatRoomListResponse(items, pageInfo);
    }

    public WaitingRoomResponse getWaitingRoom(Long roomId, Long userId) {
        ChattingRoom room = findRoom(roomId);

        chattingRoomMemberRepository.findByChattingRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_MEMBER_NOT_FOUND));

        return buildWaitingRoomResponse(room);
    }

    public ChatRoomStartResponse getChatRoomDetail(Long roomId, Long userId) {
        ChattingRoomAndMember context = findChattingRoomAndMember(roomId, userId);
        ChattingRoom room = context.room();

        List<ChattingRoomMember> activeMembers = findActiveMembers(roomId);

        List<ChatStartMemberItem> agreeMembers = activeMembers.stream()
                .filter(m -> m.getPosition() == Position.AGREE)
                .map(m -> ChatStartMemberItem.from(m, imageUrlResolver))
                .toList();
        List<ChatStartMemberItem> disagreeMembers = activeMembers.stream()
                .filter(m -> m.getPosition() == Position.DISAGREE)
                .map(m -> ChatStartMemberItem.from(m, imageUrlResolver))
                .toList();

        RoomRound activeRound = roomRoundRepository.findByChattingRoomIdAndEndedAtIsNull(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_ROUND_NOT_FOUND));

        return new ChatRoomStartResponse(
                room.getTopic(), agreeMembers, disagreeMembers, activeRound.getRoundNumber(), activeRound.getStartedAt());
    }

    WaitingRoomResponse buildWaitingRoomResponse(ChattingRoom room) {
        List<ChattingRoomMember> activeMembers = chattingRoomMemberRepository
                .findByChattingRoomIdAndStatusIn(room.getId(), ACTIVE_STATUSES);

        int agreeCount = (int) activeMembers.stream()
                .filter(m -> m.getPosition() == Position.AGREE).count();
        int disagreeCount = (int) activeMembers.stream()
                .filter(m -> m.getPosition() == Position.DISAGREE).count();
        int maxPerPosition = room.getCapacity() / 2;

        List<WaitingRoomMemberItem> members = activeMembers.stream()
                .map(m -> WaitingRoomMemberItem.from(m, imageUrlResolver))
                .toList();

        return new WaitingRoomResponse(room.getId(), agreeCount, disagreeCount, maxPerPosition, members);
    }

    ChatStartMemberItem toStartMemberItem(ChattingRoomMember member) {
        return ChatStartMemberItem.from(member, imageUrlResolver);
    }

    public ChatRoomSummaryResponse getRoomSummary(Long roomId) {
        ChattingRoom room = findRoom(roomId);

        if (room.getStatus() != RoomStatus.ENDED) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_NOT_ENDED);
        }

        List<RoomRound> rounds = roomRoundRepository.findByChattingRoomIdOrderByRoundNumberAsc(roomId);

        List<ChatRoomSummaryResponse.RoundSummaryItem> roundItems = rounds.stream()
                .map(round -> new ChatRoomSummaryResponse.RoundSummaryItem(
                        round.getRoundNumber(),
                        parseSummary(round.getSummary())
                ))
                .toList();

        return new ChatRoomSummaryResponse(roomId, room.getTopic(), roundItems);
    }

    private ChatRoomSummaryResponse.RoundSummaryContent parseSummary(String summaryJson) {
        if (summaryJson == null) {
            return null;
        }
        try {
            return objectMapper.readValue(summaryJson, ChatRoomSummaryResponse.RoundSummaryContent.class);
        } catch (Exception e) {
            log.error("Failed to parse round summary JSON: {}", e.getMessage());
            return null;
        }
    }

    private ChattingRoom findRoom(Long roomId) {
        return chattingRoomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));
    }

    private List<ChattingRoomMember> findActiveMembers(Long roomId) {
        return chattingRoomMemberRepository.findByChattingRoomIdAndStatusIn(roomId, ACTIVE_STATUSES);
    }

    record ChattingRoomAndMember(ChattingRoom room, ChattingRoomMember member) {}

    ChattingRoomAndMember findChattingRoomAndMember(Long roomId, Long userId) {
        ChattingRoom room = findRoom(roomId);

        if (room.getStatus() != RoomStatus.CHATTING) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_NOT_CHATTING);
        }

        ChattingRoomMember member = chattingRoomMemberRepository.findByChattingRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_MEMBER_NOT_FOUND));

        return new ChattingRoomAndMember(room, member);
    }
}

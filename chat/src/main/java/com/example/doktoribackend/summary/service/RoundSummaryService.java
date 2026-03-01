package com.example.doktoribackend.summary.service;

import com.example.doktoribackend.message.domain.Message;
import com.example.doktoribackend.message.repository.MessageRepository;
import com.example.doktoribackend.room.domain.ChattingRoomMember;
import com.example.doktoribackend.room.repository.ChattingRoomMemberRepository;
import com.example.doktoribackend.room.repository.RoomRoundRepository;
import com.example.doktoribackend.summary.client.AiSummaryClient;
import com.example.doktoribackend.summary.client.AiSummaryRequest;
import com.example.doktoribackend.summary.client.AiSummaryResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoundSummaryService {

    private static final int MAX_CHAR_LIMIT = 2400;
    private static final int MAX_RETRY = 3;

    private final MessageRepository messageRepository;
    private final ChattingRoomMemberRepository chattingRoomMemberRepository;
    private final RoomRoundRepository roomRoundRepository;
    private final AiSummaryClient aiSummaryClient;
    private final ObjectMapper objectMapper;
    private final PlatformTransactionManager transactionManager;

    @Async("aiSummaryExecutor")
    public void generateSummaryAsync(Long roomId, Long roundId, String topic, int roundNumber) {
        try {
            List<AiSummaryRequest.MessageItem> messageItems = loadMessages(roomId, roundId);

            if (messageItems.isEmpty()) {
                log.info("No text messages found for round summary: roomId={}, roundId={}", roomId, roundId);
                return;
            }

            AiSummaryRequest request = new AiSummaryRequest(topic, String.valueOf(roundNumber), messageItems);
            AiSummaryResponse response = requestWithRetry(roomId, request);

            String summaryJson = objectMapper.writeValueAsString(response.summary());

            TransactionTemplate writeTx = new TransactionTemplate(transactionManager);
            writeTx.executeWithoutResult(status ->
                roomRoundRepository.findById(roundId).ifPresent(round -> round.updateSummary(summaryJson))
            );

            log.info("AI summary generated successfully: roomId={}, roundNumber={}", roomId, roundNumber);

        } catch (Exception e) {
            log.error("Failed to generate AI summary: roomId={}, roundId={}, error={}", roomId, roundId, e.getMessage());
        }
    }

    private List<AiSummaryRequest.MessageItem> loadMessages(Long roomId, Long roundId) {
        TransactionTemplate readTx = new TransactionTemplate(transactionManager);
        readTx.setReadOnly(true);

        return readTx.execute(status -> {
            List<Message> messages = messageRepository.findTextMessagesByRoundIdDesc(roundId);
            if (messages.isEmpty()) {
                return Collections.emptyList();
            }

            Set<Long> senderIds = messages.stream()
                    .map(Message::getSenderId)
                    .collect(Collectors.toSet());

            Map<Long, String> nicknameMap = chattingRoomMemberRepository
                    .findByChattingRoomIdAndUserIdIn(roomId, senderIds)
                    .stream()
                    .collect(Collectors.toMap(ChattingRoomMember::getUserId, ChattingRoomMember::getNickname));

            List<AiSummaryRequest.MessageItem> selected = new ArrayList<>();
            int totalLength = 0;

            for (Message message : messages) {
                String nickname = nicknameMap.getOrDefault(message.getSenderId(), "unknown");
                String text = message.getTextMessage();
                int length = nickname.length() + text.length();

                if (totalLength + length > MAX_CHAR_LIMIT) {
                    break;
                }

                selected.add(new AiSummaryRequest.MessageItem(nickname, text));
                totalLength += length;
            }

            Collections.reverse(selected);
            return selected;
        });
    }

    private AiSummaryResponse requestWithRetry(Long roomId, AiSummaryRequest request) {
        return requestWithRetry(roomId, request, 1);
    }

    private AiSummaryResponse requestWithRetry(Long roomId, AiSummaryRequest request, int attempt) {
        try {
            return aiSummaryClient.requestSummary(roomId, request);
        } catch (Exception e) {
            if (attempt >= MAX_RETRY) {
                throw e;
            }
            long waitMs = attempt * 2000L;
            log.warn("AI summary request failed (attempt {}), retrying in {}ms: {}", attempt, waitMs, e.getMessage());
            sleep(waitMs);
            return requestWithRetry(roomId, request, attempt + 1);
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}

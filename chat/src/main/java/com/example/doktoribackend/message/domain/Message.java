package com.example.doktoribackend.message.domain;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "messages")
@CompoundIndexes({
    @CompoundIndex(name = "uk_room_sender_client", def = "{'roomId':1, 'senderId':1, 'clientMessageId':1}", unique = true),
    @CompoundIndex(name = "idx_room_id", def = "{'roomId':1}"),
    @CompoundIndex(name = "idx_round_id", def = "{'roundId':1}")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Message {

    @Id
    private String id;

    private Long roomId;
    private Long roundId;
    private Long senderId;
    private String clientMessageId;
    private MessageType messageType;
    private String textMessage;
    private String filePath;

    @CreatedDate
    private LocalDateTime createdAt;

    @Builder
    public Message(Long roomId, Long roundId, Long senderId,
                   String clientMessageId, MessageType messageType,
                   String textMessage, String filePath) {
        this.roomId = roomId;
        this.roundId = roundId;
        this.senderId = senderId;
        this.clientMessageId = clientMessageId;
        this.messageType = messageType;
        this.textMessage = textMessage;
        this.filePath = filePath;
    }

    public static Message createTextMessage(Long roomId, Long roundId,
                                             Long senderId, String clientMessageId,
                                             String textMessage) {
        return Message.builder()
                .roomId(roomId)
                .roundId(roundId)
                .senderId(senderId)
                .clientMessageId(clientMessageId)
                .messageType(MessageType.TEXT)
                .textMessage(textMessage)
                .build();
    }

    public static Message createFileMessage(Long roomId, Long roundId,
                                             Long senderId, String clientMessageId,
                                             String filePath) {
        return Message.builder()
                .roomId(roomId)
                .roundId(roundId)
                .senderId(senderId)
                .clientMessageId(clientMessageId)
                .messageType(MessageType.FILE)
                .filePath(filePath)
                .build();
    }
}

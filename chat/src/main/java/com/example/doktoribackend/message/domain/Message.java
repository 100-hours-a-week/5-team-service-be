package com.example.doktoribackend.message.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "messages",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_messages_room_sender_client",
        columnNames = {"room_id", "sender_id", "client_message_id"}
    ),
    indexes = {
        @Index(name = "idx_messages_room_id", columnList = "room_id, id"),
        @Index(name = "idx_messages_round_id", columnList = "round_id, id")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "round_id", nullable = false)
    private Long roundId;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(name = "client_message_id", nullable = false, length = 50)
    private String clientMessageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 20)
    private MessageType messageType;

    @Column(name = "text_message", length = 300)
    private String textMessage;

    @Column(name = "file_path", length = 512)
    private String filePath;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
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

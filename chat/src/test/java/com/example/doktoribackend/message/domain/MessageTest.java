package com.example.doktoribackend.message.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MessageTest {

    @Test
    @DisplayName("createTextMessage로 텍스트 메시지를 생성한다")
    void createTextMessage() {
        Message message = Message.createTextMessage(
                1L, 10L, 100L, "client-msg-1", "안녕하세요"
        );

        assertThat(message.getRoomId()).isEqualTo(1L);
        assertThat(message.getRoundId()).isEqualTo(10L);
        assertThat(message.getSenderId()).isEqualTo(100L);
        assertThat(message.getClientMessageId()).isEqualTo("client-msg-1");
        assertThat(message.getMessageType()).isEqualTo(MessageType.TEXT);
        assertThat(message.getTextMessage()).isEqualTo("안녕하세요");
        assertThat(message.getFilePath()).isNull();
    }

    @Test
    @DisplayName("createFileMessage로 파일 메시지를 생성한다")
    void createFileMessage() {
        Message message = Message.createFileMessage(
                1L, 10L, 100L, "client-msg-2", "uploads/image.png"
        );

        assertThat(message.getRoomId()).isEqualTo(1L);
        assertThat(message.getRoundId()).isEqualTo(10L);
        assertThat(message.getSenderId()).isEqualTo(100L);
        assertThat(message.getClientMessageId()).isEqualTo("client-msg-2");
        assertThat(message.getMessageType()).isEqualTo(MessageType.FILE);
        assertThat(message.getFilePath()).isEqualTo("uploads/image.png");
        assertThat(message.getTextMessage()).isNull();
    }

    @Test
    @DisplayName("Builder로 직접 메시지를 생성한다")
    void createWithBuilder() {
        Message message = Message.builder()
                .roomId(1L)
                .roundId(10L)
                .senderId(100L)
                .clientMessageId("client-msg-3")
                .messageType(MessageType.TEXT)
                .textMessage("테스트")
                .build();

        assertThat(message.getMessageType()).isEqualTo(MessageType.TEXT);
        assertThat(message.getTextMessage()).isEqualTo("테스트");
    }

    @Test
    @DisplayName("createTextMessage는 filePath를 설정하지 않는다")
    void textMessageHasNoFilePath() {
        Message message = Message.createTextMessage(
                1L, 10L, 100L, "client-msg-4", "텍스트"
        );

        assertThat(message.getMessageType()).isEqualTo(MessageType.TEXT);
        assertThat(message.getTextMessage()).isEqualTo("텍스트");
        assertThat(message.getFilePath()).isNull();
    }

    @Test
    @DisplayName("createFileMessage는 textMessage를 설정하지 않는다")
    void fileMessageHasNoTextMessage() {
        Message message = Message.createFileMessage(
                1L, 10L, 100L, "client-msg-5", "uploads/file.pdf"
        );

        assertThat(message.getMessageType()).isEqualTo(MessageType.FILE);
        assertThat(message.getFilePath()).isEqualTo("uploads/file.pdf");
        assertThat(message.getTextMessage()).isNull();
    }
}

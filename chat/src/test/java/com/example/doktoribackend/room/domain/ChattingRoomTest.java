package com.example.doktoribackend.room.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChattingRoomTest {

    @Test
    @DisplayName("Builder로 채팅방을 생성한다")
    void create() {
        ChattingRoom room = ChattingRoom.builder()
                .topic("AI는 인간을 대체할까")
                .description("AI 대체 가능성 토론")
                .capacity(4)
                .duration(30)
                .build();

        assertThat(room.getTopic()).isEqualTo("AI는 인간을 대체할까");
        assertThat(room.getDescription()).isEqualTo("AI 대체 가능성 토론");
        assertThat(room.getCapacity()).isEqualTo(4);
        assertThat(room.getDuration()).isEqualTo(30);
        assertThat(room.getStatus()).isEqualTo(RoomStatus.WAITING);
    }

    @Test
    @DisplayName("duration이 null이면 기본값 30이 설정된다")
    void createWithDefaultDuration() {
        ChattingRoom room = ChattingRoom.builder()
                .topic("주제")
                .description("설명")
                .capacity(2)
                .duration(null)
                .build();

        assertThat(room.getDuration()).isEqualTo(30);
    }

    @Test
    @DisplayName("채팅을 시작하면 상태가 CHATTING으로 변경된다")
    void startChatting() {
        ChattingRoom room = ChattingRoom.builder()
                .topic("주제")
                .description("설명")
                .capacity(4)
                .build();

        room.startChatting();

        assertThat(room.getStatus()).isEqualTo(RoomStatus.CHATTING);
    }

    @Test
    @DisplayName("채팅을 종료하면 상태가 ENDED로 변경된다")
    void endChatting() {
        ChattingRoom room = ChattingRoom.builder()
                .topic("주제")
                .description("설명")
                .capacity(4)
                .build();

        room.startChatting();
        room.endChatting();

        assertThat(room.getStatus()).isEqualTo(RoomStatus.ENDED);
    }

    @Test
    @DisplayName("채팅방을 취소하면 상태가 CANCELLED로 변경되고 인원이 0이 된다")
    void cancel() {
        ChattingRoom room = ChattingRoom.builder()
                .topic("주제")
                .description("설명")
                .capacity(4)
                .build();
        room.increaseMemberCount();
        room.increaseMemberCount();

        room.cancel();

        assertThat(room.getStatus()).isEqualTo(RoomStatus.CANCELLED);
        assertThat(room.getCurrentMemberCount()).isZero();
    }

    @Test
    @DisplayName("생성 시 rounds와 members 리스트가 빈 리스트로 초기화된다")
    void listsInitialized() {
        ChattingRoom room = ChattingRoom.builder()
                .topic("주제")
                .description("설명")
                .capacity(4)
                .build();

        assertThat(room.getRounds()).isNotNull().isEmpty();
        assertThat(room.getMembers()).isNotNull().isEmpty();
    }

}

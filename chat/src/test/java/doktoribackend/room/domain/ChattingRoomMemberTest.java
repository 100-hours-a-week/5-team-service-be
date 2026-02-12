package doktoribackend.room.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChattingRoomMemberTest {

    private ChattingRoom room;

    @BeforeEach
    void setUp() {
        room = ChattingRoom.builder()
                .topic("주제")
                .description("설명")
                .capacity(4)
                .build();
    }

    @Test
    @DisplayName("Builder로 멤버를 생성한다")
    void create() {
        ChattingRoomMember member = ChattingRoomMember.builder()
                .chattingRoom(room)
                .userId(1L)
                .role(MemberRole.HOST)
                .position(Position.AGREE)
                .build();

        assertThat(member.getChattingRoom()).isEqualTo(room);
        assertThat(member.getUserId()).isEqualTo(1L);
        assertThat(member.getRole()).isEqualTo(MemberRole.HOST);
        assertThat(member.getPosition()).isEqualTo(Position.AGREE);
        assertThat(member.getStatus()).isEqualTo(MemberStatus.WAITING);
    }

    @Test
    @DisplayName("join 호출 시 상태가 JOINED로 변경된다")
    void join() {
        ChattingRoomMember member = ChattingRoomMember.builder()
                .chattingRoom(room)
                .userId(1L)
                .role(MemberRole.PARTICIPANT)
                .position(Position.DISAGREE)
                .build();

        member.join();

        assertThat(member.getStatus()).isEqualTo(MemberStatus.JOINED);
    }

    @Test
    @DisplayName("disconnect 호출 시 상태가 DISCONNECTED로 변경된다")
    void disconnect() {
        ChattingRoomMember member = ChattingRoomMember.builder()
                .chattingRoom(room)
                .userId(1L)
                .role(MemberRole.PARTICIPANT)
                .position(Position.AGREE)
                .build();

        member.join();
        member.disconnect();

        assertThat(member.getStatus()).isEqualTo(MemberStatus.DISCONNECTED);
    }

    @Test
    @DisplayName("leave 호출 시 상태가 LEFT로 변경된다")
    void leave() {
        ChattingRoomMember member = ChattingRoomMember.builder()
                .chattingRoom(room)
                .userId(1L)
                .role(MemberRole.PARTICIPANT)
                .position(Position.DISAGREE)
                .build();

        member.leave();

        assertThat(member.getStatus()).isEqualTo(MemberStatus.LEFT);
    }

    @Test
    @DisplayName("HOST 역할이면 isHost가 true를 반환한다")
    void isHostTrue() {
        ChattingRoomMember host = ChattingRoomMember.builder()
                .chattingRoom(room)
                .userId(1L)
                .role(MemberRole.HOST)
                .position(Position.AGREE)
                .build();

        assertThat(host.isHost()).isTrue();
    }

    @Test
    @DisplayName("PARTICIPANT 역할이면 isHost가 false를 반환한다")
    void isHostFalse() {
        ChattingRoomMember participant = ChattingRoomMember.builder()
                .chattingRoom(room)
                .userId(2L)
                .role(MemberRole.PARTICIPANT)
                .position(Position.DISAGREE)
                .build();

        assertThat(participant.isHost()).isFalse();
    }
}

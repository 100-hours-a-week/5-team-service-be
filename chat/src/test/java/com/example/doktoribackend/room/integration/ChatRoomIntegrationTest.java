package com.example.doktoribackend.room.integration;

import com.example.doktoribackend.room.domain.*;
import com.example.doktoribackend.room.repository.ChattingRoomMemberRepository;
import com.example.doktoribackend.room.repository.ChattingRoomRepository;
import com.example.doktoribackend.room.repository.RoomRoundRepository;
import com.example.doktoribackend.message.repository.MessageRepository;
import com.example.doktoribackend.security.jwt.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ChatRoomIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ChattingRoomRepository chattingRoomRepository;

    @Autowired
    private ChattingRoomMemberRepository chattingRoomMemberRepository;

    @Autowired
    private RoomRoundRepository roomRoundRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestRestTemplate restTemplate;

    private Long roomId;
    private String hostToken;

    private static final Long HOST_ID = 1L;
    private static final String HOST_NICKNAME = "방장";
    private static final Long PARTICIPANT_ID = 2L;
    private static final String PARTICIPANT_NICKNAME = "참여자";

    @BeforeEach
    void setUp() {
        hostToken = jwtTokenProvider.createAccessToken(HOST_ID, HOST_NICKNAME);

        ChattingRoom room = ChattingRoom.builder()
                .topic("테스트 토론")
                .description("통합 테스트용")
                .capacity(4)
                .build();
        room.startChatting();
        chattingRoomRepository.save(room);
        roomId = room.getId();

        ChattingRoomMember host = ChattingRoomMember.builder()
                .chattingRoom(room)
                .userId(HOST_ID)
                .nickname(HOST_NICKNAME)
                .role(MemberRole.HOST)
                .position(Position.AGREE)
                .build();
        host.join();
        chattingRoomMemberRepository.save(host);

        ChattingRoomMember participant = ChattingRoomMember.builder()
                .chattingRoom(room)
                .userId(PARTICIPANT_ID)
                .nickname(PARTICIPANT_NICKNAME)
                .role(MemberRole.PARTICIPANT)
                .position(Position.DISAGREE)
                .build();
        participant.join();
        chattingRoomMemberRepository.save(participant);
    }

    @AfterEach
    void tearDown() {
        messageRepository.deleteAllInBatch();
        roomRoundRepository.deleteAllInBatch();
        chattingRoomMemberRepository.deleteAllInBatch();
        chattingRoomRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("다음 라운드 전환 시 STOMP 구독자에게 NextRoundResponse가 브로드캐스트된다")
    void nextRound_broadcastToSubscribers() throws Exception {
        // given
        RoomRound round = RoomRound.builder()
                .chattingRoom(chattingRoomRepository.findById(roomId).orElseThrow())
                .roundNumber(1)
                .build();
        roomRoundRepository.save(round);

        WebSocketStompClient stompClient = createStompClient();
        BlockingQueue<Map> queue = new LinkedBlockingQueue<>();

        String participantToken = jwtTokenProvider.createAccessToken(PARTICIPANT_ID, PARTICIPANT_NICKNAME);
        StompSession session = connectWithToken(stompClient, participantToken);

        session.subscribe("/topic/chat-rooms/" + roomId, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                queue.offer((Map) payload);
            }
        });

        Thread.sleep(500);

        // when
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(hostToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<Void> response = restTemplate.exchange(
                "/chat-rooms/" + roomId + "/next-round",
                HttpMethod.PATCH, request, Void.class);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        Map received = queue.poll(5, TimeUnit.SECONDS);
        assertThat(received).isNotNull();
        assertThat(received.get("currentRound")).isEqualTo(2);
        assertThat(received.get("startedAt")).isNotNull();

        stompClient.stop();
    }

    @Test
    @DisplayName("채팅방 종료 시 STOMP 구독자에게 ROOM_ENDED가 브로드캐스트된다")
    void endChatRoom_broadcastToSubscribers() throws Exception {
        // given
        RoomRound round = RoomRound.builder()
                .chattingRoom(chattingRoomRepository.findById(roomId).orElseThrow())
                .roundNumber(3)
                .build();
        roomRoundRepository.save(round);

        WebSocketStompClient stompClient = createStompClient();
        BlockingQueue<Map> queue = new LinkedBlockingQueue<>();

        String participantToken = jwtTokenProvider.createAccessToken(PARTICIPANT_ID, PARTICIPANT_NICKNAME);
        StompSession session = connectWithToken(stompClient, participantToken);

        session.subscribe("/topic/chat-rooms/" + roomId, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                queue.offer((Map) payload);
            }
        });

        Thread.sleep(500);

        // when
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(hostToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<Void> response = restTemplate.exchange(
                "/chat-rooms/" + roomId,
                HttpMethod.DELETE, request, Void.class);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        Map received = queue.poll(5, TimeUnit.SECONDS);
        assertThat(received).isNotNull();
        assertThat(received.get("type")).isEqualTo("ROOM_ENDED");

        stompClient.stop();
    }

    private WebSocketStompClient createStompClient() {
        SockJsClient sockJsClient = new SockJsClient(
                List.of(new WebSocketTransport(new StandardWebSocketClient()))
        );
        WebSocketStompClient stompClient = new WebSocketStompClient(sockJsClient);
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(objectMapper);
        stompClient.setMessageConverter(converter);
        return stompClient;
    }

    private StompSession connectWithToken(WebSocketStompClient stompClient, String token)
            throws Exception {
        String url = "ws://localhost:" + port + "/api/ws";
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + token);

        return stompClient.connectAsync(
                url,
                new WebSocketHttpHeaders(),
                connectHeaders,
                new StompSessionHandlerAdapter() {}
        ).get(5, TimeUnit.SECONDS);
    }
}

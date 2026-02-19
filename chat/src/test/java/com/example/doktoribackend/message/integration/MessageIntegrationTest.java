package com.example.doktoribackend.message.integration;

import com.example.doktoribackend.message.domain.MessageType;
import com.example.doktoribackend.message.dto.MessageResponse;
import com.example.doktoribackend.message.repository.MessageRepository;
import com.example.doktoribackend.room.domain.*;
import com.example.doktoribackend.room.repository.ChattingRoomMemberRepository;
import com.example.doktoribackend.room.repository.ChattingRoomRepository;
import com.example.doktoribackend.room.repository.RoomRoundRepository;
import com.example.doktoribackend.security.jwt.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
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
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class MessageIntegrationTest {

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

    private Long roomId;

    private static final Long USER_A_ID = 1L;
    private static final String USER_A_NICKNAME = "사용자A";
    private static final Long USER_B_ID = 2L;
    private static final String USER_B_NICKNAME = "사용자B";

    @BeforeEach
    void setUp() {
        ChattingRoom room = ChattingRoom.builder()
                .topic("테스트 토론")
                .description("통합 테스트용")
                .capacity(4)
                .build();
        room.startChatting();
        chattingRoomRepository.save(room);
        roomId = room.getId();

        ChattingRoomMember memberA = ChattingRoomMember.builder()
                .chattingRoom(room)
                .userId(USER_A_ID)
                .nickname(USER_A_NICKNAME)
                .role(MemberRole.HOST)
                .position(Position.AGREE)
                .build();
        memberA.join();
        chattingRoomMemberRepository.save(memberA);

        ChattingRoomMember memberB = ChattingRoomMember.builder()
                .chattingRoom(room)
                .userId(USER_B_ID)
                .nickname(USER_B_NICKNAME)
                .role(MemberRole.PARTICIPANT)
                .position(Position.DISAGREE)
                .build();
        memberB.join();
        chattingRoomMemberRepository.save(memberB);

        RoomRound round = RoomRound.builder()
                .chattingRoom(room)
                .roundNumber(1)
                .build();
        roomRoundRepository.save(round);
    }

    @AfterEach
    void tearDown() {
        messageRepository.deleteAllInBatch();
        roomRoundRepository.deleteAllInBatch();
        chattingRoomMemberRepository.deleteAllInBatch();
        chattingRoomRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("메시지를 전송하면 구독자에게 브로드캐스트된다")
    void sendMessage_broadcastToSubscribers() throws Exception {
        // given
        String token = jwtTokenProvider.createAccessToken(USER_A_ID, USER_A_NICKNAME);
        WebSocketStompClient stompClient = createStompClient();
        BlockingQueue<MessageResponse> queue = new LinkedBlockingQueue<>();

        StompSession session = connectWithToken(stompClient, token);

        session.subscribe("/topic/chat-rooms/" + roomId, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return MessageResponse.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                queue.offer((MessageResponse) payload);
            }
        });

        Thread.sleep(500);

        // when
        String clientMessageId = UUID.randomUUID().toString();
        Map<String, Object> message = Map.of(
                "clientMessageId", clientMessageId,
                "messageType", "TEXT",
                "textMessage", "안녕하세요"
        );
        session.send("/app/chat-rooms/" + roomId + "/messages", message);

        // then
        MessageResponse response = queue.poll(5, TimeUnit.SECONDS);
        assertThat(response).isNotNull();
        assertThat(response.senderId()).isEqualTo(USER_A_ID);
        assertThat(response.senderNickname()).isEqualTo(USER_A_NICKNAME);
        assertThat(response.messageType()).isEqualTo(MessageType.TEXT);
        assertThat(response.textMessage()).isEqualTo("안녕하세요");

        stompClient.stop();
    }

    @Test
    @DisplayName("두 명의 사용자가 각각 메시지를 전송하면 모두 수신한다")
    void twoUsers_sendAndReceiveMessages() throws Exception {
        // given
        String tokenA = jwtTokenProvider.createAccessToken(USER_A_ID, USER_A_NICKNAME);
        String tokenB = jwtTokenProvider.createAccessToken(USER_B_ID, USER_B_NICKNAME);

        WebSocketStompClient stompClientA = createStompClient();
        WebSocketStompClient stompClientB = createStompClient();

        BlockingQueue<MessageResponse> queueA = new LinkedBlockingQueue<>();
        BlockingQueue<MessageResponse> queueB = new LinkedBlockingQueue<>();

        StompSession sessionA = connectWithToken(stompClientA, tokenA);
        StompSession sessionB = connectWithToken(stompClientB, tokenB);

        StompFrameHandler handlerA = new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return MessageResponse.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                queueA.offer((MessageResponse) payload);
            }
        };

        StompFrameHandler handlerB = new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return MessageResponse.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                queueB.offer((MessageResponse) payload);
            }
        };

        sessionA.subscribe("/topic/chat-rooms/" + roomId, handlerA);
        sessionB.subscribe("/topic/chat-rooms/" + roomId, handlerB);

        Thread.sleep(500);

        // when: A sends a message
        String clientMsgIdA = UUID.randomUUID().toString();
        Map<String, Object> messageA = Map.of(
                "clientMessageId", clientMsgIdA,
                "messageType", "TEXT",
                "textMessage", "A의 메시지"
        );
        sessionA.send("/app/chat-rooms/" + roomId + "/messages", messageA);

        // then: both A and B receive A's message
        MessageResponse responseA1 = queueA.poll(5, TimeUnit.SECONDS);
        MessageResponse responseB1 = queueB.poll(5, TimeUnit.SECONDS);

        assertThat(responseA1).isNotNull();
        assertThat(responseA1.senderId()).isEqualTo(USER_A_ID);
        assertThat(responseA1.textMessage()).isEqualTo("A의 메시지");

        assertThat(responseB1).isNotNull();
        assertThat(responseB1.senderId()).isEqualTo(USER_A_ID);
        assertThat(responseB1.textMessage()).isEqualTo("A의 메시지");

        // when: B sends a message
        String clientMsgIdB = UUID.randomUUID().toString();
        Map<String, Object> messageB = Map.of(
                "clientMessageId", clientMsgIdB,
                "messageType", "TEXT",
                "textMessage", "B의 메시지"
        );
        sessionB.send("/app/chat-rooms/" + roomId + "/messages", messageB);

        // then: both A and B receive B's message
        MessageResponse responseA2 = queueA.poll(5, TimeUnit.SECONDS);
        MessageResponse responseB2 = queueB.poll(5, TimeUnit.SECONDS);

        assertThat(responseA2).isNotNull();
        assertThat(responseA2.senderId()).isEqualTo(USER_B_ID);
        assertThat(responseA2.textMessage()).isEqualTo("B의 메시지");

        assertThat(responseB2).isNotNull();
        assertThat(responseB2.senderId()).isEqualTo(USER_B_ID);
        assertThat(responseB2.textMessage()).isEqualTo("B의 메시지");

        stompClientA.stop();
        stompClientB.stop();
    }

    @Test
    @DisplayName("파일 메시지를 전송하면 filePath가 포함되어 브로드캐스트된다")
    void sendFileMessage_broadcastWithFilePath() throws Exception {
        // given
        String token = jwtTokenProvider.createAccessToken(USER_A_ID, USER_A_NICKNAME);
        WebSocketStompClient stompClient = createStompClient();
        BlockingQueue<MessageResponse> queue = new LinkedBlockingQueue<>();

        StompSession session = connectWithToken(stompClient, token);

        session.subscribe("/topic/chat-rooms/" + roomId, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return MessageResponse.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                queue.offer((MessageResponse) payload);
            }
        });

        Thread.sleep(500);

        // when
        String clientMessageId = UUID.randomUUID().toString();
        Map<String, Object> message = Map.of(
                "clientMessageId", clientMessageId,
                "messageType", "FILE",
                "filePath", "images/chats/550e8400-e29b-41d4-a716-446655440000.png"
        );
        session.send("/app/chat-rooms/" + roomId + "/messages", message);

        // then
        MessageResponse response = queue.poll(5, TimeUnit.SECONDS);
        assertThat(response).isNotNull();
        assertThat(response.senderId()).isEqualTo(USER_A_ID);
        assertThat(response.senderNickname()).isEqualTo(USER_A_NICKNAME);
        assertThat(response.messageType()).isEqualTo(MessageType.FILE);
        assertThat(response.filePath()).isEqualTo("https://test-bucket.s3.amazonaws.com/images/chats/550e8400-e29b-41d4-a716-446655440000.png");
        assertThat(response.textMessage()).isNull();

        stompClient.stop();
    }

    @Test
    @DisplayName("중복 clientMessageId 메시지는 브로드캐스트되지 않는다")
    void duplicateClientMessageId_notBroadcast() throws Exception {
        // given
        String token = jwtTokenProvider.createAccessToken(USER_A_ID, USER_A_NICKNAME);
        WebSocketStompClient stompClient = createStompClient();
        BlockingQueue<MessageResponse> queue = new LinkedBlockingQueue<>();

        StompSession session = connectWithToken(stompClient, token);

        session.subscribe("/topic/chat-rooms/" + roomId, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return MessageResponse.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                queue.offer((MessageResponse) payload);
            }
        });

        Thread.sleep(500);

        String duplicateClientMessageId = UUID.randomUUID().toString();
        Map<String, Object> message = Map.of(
                "clientMessageId", duplicateClientMessageId,
                "messageType", "TEXT",
                "textMessage", "첫 번째 메시지"
        );

        // when: send same clientMessageId twice
        session.send("/app/chat-rooms/" + roomId + "/messages", message);

        MessageResponse firstResponse = queue.poll(5, TimeUnit.SECONDS);
        assertThat(firstResponse).isNotNull();
        assertThat(firstResponse.textMessage()).isEqualTo("첫 번째 메시지");

        session.send("/app/chat-rooms/" + roomId + "/messages", message);

        // then: second message should not be broadcast
        MessageResponse secondResponse = queue.poll(2, TimeUnit.SECONDS);
        assertThat(secondResponse).isNull();

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

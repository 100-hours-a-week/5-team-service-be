package com.example.doktoribackend.room.controller;

import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.common.s3.ImageUrlResolver;
import com.example.doktoribackend.exception.BusinessException;
import com.example.doktoribackend.room.domain.MemberRole;
import com.example.doktoribackend.room.domain.Position;
import com.example.doktoribackend.room.dto.ChatRoomCreateRequest;
import com.example.doktoribackend.room.dto.ChatRoomCreateRequest.QuizChoiceRequest;
import com.example.doktoribackend.room.dto.ChatRoomCreateRequest.QuizRequest;
import com.example.doktoribackend.room.dto.ChatRoomCreateResponse;
import com.example.doktoribackend.room.dto.ChatRoomJoinRequest;
import com.example.doktoribackend.room.dto.ChatRoomListItem;
import com.example.doktoribackend.room.dto.ChatRoomListResponse;
import com.example.doktoribackend.room.dto.ChatRoomStartResponse;
import com.example.doktoribackend.room.dto.ChatStartMemberItem;
import com.example.doktoribackend.room.dto.PageInfo;
import com.example.doktoribackend.message.domain.MessageType;
import com.example.doktoribackend.message.dto.MessageListResponse;
import com.example.doktoribackend.message.dto.MessageResponse;
import com.example.doktoribackend.message.service.MessageService;
import com.example.doktoribackend.room.dto.WaitingRoomMemberItem;
import com.example.doktoribackend.room.dto.WaitingRoomResponse;
import com.example.doktoribackend.quiz.service.QuizService;
import com.example.doktoribackend.room.service.ChatRoomService;
import com.example.doktoribackend.room.service.WaitingRoomSseService;
import com.example.doktoribackend.security.CustomUserDetails;
import com.example.doktoribackend.security.jwt.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatRoomController.class)
class ChatRoomControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    ChatRoomService chatRoomService;

    @MockitoBean
    MessageService messageService;

    @MockitoBean
    WaitingRoomSseService waitingRoomSseService;

    @MockitoBean
    QuizService quizService;

    @MockitoBean
    JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    ImageUrlResolver imageUrlResolver;

    private static final Long USER_ID = 1L;

    private CustomUserDetails createUserDetails() {
        return CustomUserDetails.of(USER_ID, "testUser");
    }

    private List<QuizChoiceRequest> validChoices() {
        return List.of(
                new QuizChoiceRequest(1, "선택지1"),
                new QuizChoiceRequest(2, "선택지2"),
                new QuizChoiceRequest(3, "선택지3"),
                new QuizChoiceRequest(4, "선택지4")
        );
    }

    private QuizRequest validQuiz() {
        return new QuizRequest("퀴즈 질문입니다", validChoices(), 1);
    }

    private ChatRoomCreateRequest validRequest() {
        return new ChatRoomCreateRequest(
                "토론 주제", "주제에 대한 설명", "9781234567890", 4, Position.AGREE, validQuiz()
        );
    }

    private void performPostAndExpectUnprocessable(Object request) throws Exception {
        mockMvc.perform(post("/chat-rooms")
                        .with(SecurityMockMvcRequestPostProcessors.user(createUserDetails()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Nested
    @DisplayName("성공 케이스")
    class Success {

        @Test
        @DisplayName("유효한 요청이면 201 Created와 roomId를 반환한다")
        void createChatRoom_success() throws Exception {
            given(chatRoomService.createChatRoom(eq(USER_ID), any()))
                    .willReturn(new ChatRoomCreateResponse(100L));

            mockMvc.perform(post("/chat-rooms")
                            .with(SecurityMockMvcRequestPostProcessors.user(createUserDetails()))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.message").value("OK"))
                    .andExpect(jsonPath("$.data.roomId").value(100));
        }
    }

    @Nested
    @DisplayName("topic 검증")
    class TopicValidation {

        @ParameterizedTest(name = "topic이 [{0}]이면 검증 실패")
        @NullSource
        @ValueSource(strings = {"가", "주제!@#$"})
        void invalidTopic_fails(String topic) throws Exception {
            ChatRoomCreateRequest request = new ChatRoomCreateRequest(
                    topic, "주제에 대한 설명", "9781234567890", 4, Position.AGREE, validQuiz());
            performPostAndExpectUnprocessable(request);
        }
    }

    @Nested
    @DisplayName("필수값 누락 검증")
    class RequiredFieldValidation {

        @Test
        @DisplayName("description이 null이면 검증 실패")
        void description_null() throws Exception {
            ChatRoomCreateRequest request = new ChatRoomCreateRequest(
                    "토론 주제", null, "9781234567890", 4, Position.AGREE, validQuiz());
            performPostAndExpectUnprocessable(request);
        }

        @Test
        @DisplayName("capacity가 null이면 검증 실패")
        void capacity_null() throws Exception {
            ChatRoomCreateRequest request = new ChatRoomCreateRequest(
                    "토론 주제", "주제에 대한 설명", "9781234567890", null, Position.AGREE, validQuiz());
            performPostAndExpectUnprocessable(request);
        }

        @Test
        @DisplayName("position이 null이면 검증 실패")
        void position_null() throws Exception {
            ChatRoomCreateRequest request = new ChatRoomCreateRequest(
                    "토론 주제", "주제에 대한 설명", "9781234567890", 4, null, validQuiz());
            performPostAndExpectUnprocessable(request);
        }

        @Test
        @DisplayName("quiz가 null이면 검증 실패")
        void quiz_null() throws Exception {
            ChatRoomCreateRequest request = new ChatRoomCreateRequest(
                    "토론 주제", "주제에 대한 설명", "9781234567890", 4, Position.AGREE, null);
            performPostAndExpectUnprocessable(request);
        }
    }

    @Nested
    @DisplayName("quiz 검증")
    class QuizValidation {

        @Test
        @DisplayName("quiz.question이 null이면 검증 실패")
        void quiz_question_null() throws Exception {
            QuizRequest quiz = new QuizRequest(null, validChoices(), 1);
            ChatRoomCreateRequest request = new ChatRoomCreateRequest(
                    "토론 주제", "주제에 대한 설명", "9781234567890", 4, Position.AGREE, quiz);
            performPostAndExpectUnprocessable(request);
        }

        @Test
        @DisplayName("quiz.choices가 3개이면 검증 실패")
        void quiz_choices_lessThanFour() throws Exception {
            List<QuizChoiceRequest> threeChoices = List.of(
                    new QuizChoiceRequest(1, "선택지1"),
                    new QuizChoiceRequest(2, "선택지2"),
                    new QuizChoiceRequest(3, "선택지3")
            );
            QuizRequest quiz = new QuizRequest("퀴즈 질문입니다", threeChoices, 1);
            ChatRoomCreateRequest request = new ChatRoomCreateRequest(
                    "토론 주제", "주제에 대한 설명", "9781234567890", 4, Position.AGREE, quiz);
            performPostAndExpectUnprocessable(request);
        }

    }

    @Nested
    @DisplayName("quiz/quizChoice 범위 검증")
    class QuizRangeValidation {

        static Stream<Arguments> outOfRangeArguments() {
            return Stream.of(
                    Arguments.of(0, 1),
                    Arguments.of(5, 1),
                    Arguments.of(1, 0)
            );
        }

        @ParameterizedTest(name = "correctChoiceNumber={0}, choiceNumber={1}이면 검증 실패")
        @MethodSource("outOfRangeArguments")
        void outOfRange_fails(int correctChoiceNumber, int firstChoiceNumber) throws Exception {
            List<QuizChoiceRequest> choices = List.of(
                    new QuizChoiceRequest(firstChoiceNumber, "선택지1"),
                    new QuizChoiceRequest(2, "선택지2"),
                    new QuizChoiceRequest(3, "선택지3"),
                    new QuizChoiceRequest(4, "선택지4")
            );
            QuizRequest quiz = new QuizRequest("퀴즈 질문입니다", choices, correctChoiceNumber);
            ChatRoomCreateRequest request = new ChatRoomCreateRequest(
                    "토론 주제", "주제에 대한 설명", "9781234567890", 4, Position.AGREE, quiz);
            performPostAndExpectUnprocessable(request);
        }
    }

    @Nested
    @DisplayName("quizChoice 검증")
    class QuizChoiceValidation {

        @Test
        @DisplayName("choice.text가 빈 문자열이면 검증 실패")
        void choiceText_blank() throws Exception {
            List<QuizChoiceRequest> choices = List.of(
                    new QuizChoiceRequest(1, ""),
                    new QuizChoiceRequest(2, "선택지2"),
                    new QuizChoiceRequest(3, "선택지3"),
                    new QuizChoiceRequest(4, "선택지4")
            );
            QuizRequest quiz = new QuizRequest("퀴즈 질문입니다", choices, 1);
            ChatRoomCreateRequest request = new ChatRoomCreateRequest(
                    "토론 주제", "주제에 대한 설명", "9781234567890", 4, Position.AGREE, quiz);
            performPostAndExpectUnprocessable(request);
        }
    }

    @Nested
    @DisplayName("채팅방 목록 조회 - 성공")
    class GetChatRoomsSuccess {

        @Test
        @DisplayName("파라미터 없이 요청하면 200 OK와 목록을 반환한다")
        void getChatRooms_noParams_success() throws Exception {
            ChatRoomListResponse response = new ChatRoomListResponse(
                    List.of(new ChatRoomListItem(3L, "주제3", "설명3", 4, 2, "책제목3", "저자3", "http://thumb3.url"),
                            new ChatRoomListItem(2L, "주제2", "설명2", 6, 1, "책제목2", "저자2", "http://thumb2.url")),
                    new PageInfo(null, false, 10)
            );
            given(chatRoomService.getChatRooms(null, 10)).willReturn(response);

            mockMvc.perform(get("/chat-rooms")
                            .with(SecurityMockMvcRequestPostProcessors.user(createUserDetails()))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.items").isArray())
                    .andExpect(jsonPath("$.data.items.length()").value(2))
                    .andExpect(jsonPath("$.data.items[0].roomId").value(3))
                    .andExpect(jsonPath("$.data.items[0].topic").value("주제3"))
                    .andExpect(jsonPath("$.data.items[0].capacity").value(4))
                    .andExpect(jsonPath("$.data.items[0].currentMemberCount").value(2))
                    .andExpect(jsonPath("$.data.pageInfo.hasNext").value(false))
                    .andExpect(jsonPath("$.data.pageInfo.nextCursorId").isEmpty());
        }

        @Test
        @DisplayName("cursorId와 size를 지정하면 해당 파라미터로 조회한다")
        void getChatRooms_withParams_success() throws Exception {
            ChatRoomListResponse response = new ChatRoomListResponse(
                    List.of(new ChatRoomListItem(4L, "주제4", "설명4", 4, 1, "책제목4", "저자4", "http://thumb4.url")),
                    new PageInfo(4L, true, 1)
            );
            given(chatRoomService.getChatRooms(10L, 1)).willReturn(response);

            mockMvc.perform(get("/chat-rooms")
                            .param("cursorId", "10")
                            .param("size", "1")
                            .with(SecurityMockMvcRequestPostProcessors.user(createUserDetails()))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.items.length()").value(1))
                    .andExpect(jsonPath("$.data.pageInfo.hasNext").value(true))
                    .andExpect(jsonPath("$.data.pageInfo.nextCursorId").value(4));
        }
    }

    @Nested
    @DisplayName("채팅방 목록 조회 - size 검증")
    class GetChatRoomsSizeValidation {

        @ParameterizedTest(name = "size={0}이면 400 Bad Request")
        @ValueSource(ints = {0, 21})
        void invalidSize_returns400(int size) throws Exception {
            mockMvc.perform(get("/chat-rooms")
                            .param("size", String.valueOf(size))
                            .with(SecurityMockMvcRequestPostProcessors.user(createUserDetails()))
                            .with(csrf()))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("채팅방 목록 조회 - cursorId 검증")
    class GetChatRoomsCursorValidation {

        @ParameterizedTest(name = "cursorId={0}이면 400 Bad Request")
        @ValueSource(ints = {0, -1})
        void invalidCursorId_returns400(int cursorId) throws Exception {
            mockMvc.perform(get("/chat-rooms")
                            .param("cursorId", String.valueOf(cursorId))
                            .with(SecurityMockMvcRequestPostProcessors.user(createUserDetails()))
                            .with(csrf()))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("채팅방 나가기")
    class LeaveChatRoom {

        @Test
        @DisplayName("정상적으로 채팅방을 나가면 204 No Content를 반환한다")
        void leaveChatRoom_success() throws Exception {
            willDoNothing().given(chatRoomService).leaveChatRoom(10L, USER_ID);

            mockMvc.perform(delete("/chat-rooms/10/members/me")
                            .with(SecurityMockMvcRequestPostProcessors.user(createUserDetails()))
                            .with(csrf()))
                    .andExpect(status().isNoContent());

            then(chatRoomService).should().leaveChatRoom(10L, USER_ID);
        }

        @Test
        @DisplayName("존재하지 않는 채팅방이면 404를 반환한다")
        void leaveChatRoom_roomNotFound() throws Exception {
            willThrow(new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND))
                    .given(chatRoomService).leaveChatRoom(999L, USER_ID);

            mockMvc.perform(delete("/chat-rooms/999/members/me")
                            .with(SecurityMockMvcRequestPostProcessors.user(createUserDetails()))
                            .with(csrf()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("이미 종료된 채팅방이면 409를 반환한다")
        void leaveChatRoom_alreadyEnded() throws Exception {
            willThrow(new BusinessException(ErrorCode.CHAT_ROOM_ALREADY_ENDED))
                    .given(chatRoomService).leaveChatRoom(10L, USER_ID);

            mockMvc.perform(delete("/chat-rooms/10/members/me")
                            .with(SecurityMockMvcRequestPostProcessors.user(createUserDetails()))
                            .with(csrf()))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("이미 나간 채팅방이면 409를 반환한다")
        void leaveChatRoom_alreadyLeft() throws Exception {
            willThrow(new BusinessException(ErrorCode.CHAT_ROOM_ALREADY_LEFT))
                    .given(chatRoomService).leaveChatRoom(10L, USER_ID);

            mockMvc.perform(delete("/chat-rooms/10/members/me")
                            .with(SecurityMockMvcRequestPostProcessors.user(createUserDetails()))
                            .with(csrf()))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("채팅방 참여")
    class JoinChatRoom {

        @Test
        @DisplayName("유효한 요청이면 201 Created와 WaitingRoomResponse를 반환한다")
        void joinChatRoom_success() throws Exception {
            ChatRoomJoinRequest request = new ChatRoomJoinRequest(Position.AGREE, 1);
            WaitingRoomResponse response = new WaitingRoomResponse(
                    10L, 1, 1, 2,
                    List.of(
                            new WaitingRoomMemberItem("방장", "http://host.url", Position.AGREE, MemberRole.HOST),
                            new WaitingRoomMemberItem("테스터", "http://profile.url", Position.DISAGREE, MemberRole.PARTICIPANT)
                    )
            );
            given(chatRoomService.joinChatRoom(eq(10L), eq(USER_ID), any())).willReturn(response);

            mockMvc.perform(post("/chat-rooms/10/members")
                            .with(SecurityMockMvcRequestPostProcessors.user(createUserDetails()))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.roomId").value(10))
                    .andExpect(jsonPath("$.data.agreeCount").value(1))
                    .andExpect(jsonPath("$.data.disagreeCount").value(1))
                    .andExpect(jsonPath("$.data.maxPerPosition").value(2))
                    .andExpect(jsonPath("$.data.members").isArray())
                    .andExpect(jsonPath("$.data.members.length()").value(2));
        }

        @Test
        @DisplayName("position이 null이면 422를 반환한다")
        void joinChatRoom_positionNull() throws Exception {
            ChatRoomJoinRequest request = new ChatRoomJoinRequest(null, 1);

            mockMvc.perform(post("/chat-rooms/10/members")
                            .with(SecurityMockMvcRequestPostProcessors.user(createUserDetails()))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnprocessableEntity());
        }

        @Test
        @DisplayName("quizAnswer가 null이면 422를 반환한다")
        void joinChatRoom_quizAnswerNull() throws Exception {
            ChatRoomJoinRequest request = new ChatRoomJoinRequest(Position.AGREE, null);

            mockMvc.perform(post("/chat-rooms/10/members")
                            .with(SecurityMockMvcRequestPostProcessors.user(createUserDetails()))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnprocessableEntity());
        }

        @Test
        @DisplayName("퀴즈 오답이면 403을 반환한다")
        void joinChatRoom_quizWrong() throws Exception {
            ChatRoomJoinRequest request = new ChatRoomJoinRequest(Position.AGREE, 1);
            willThrow(new BusinessException(ErrorCode.CHAT_ROOM_QUIZ_WRONG_ANSWER))
                    .given(chatRoomService).joinChatRoom(eq(10L), eq(USER_ID), any());

            mockMvc.perform(post("/chat-rooms/10/members")
                            .with(SecurityMockMvcRequestPostProcessors.user(createUserDetails()))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("정원 초과이면 409를 반환한다")
        void joinChatRoom_roomFull() throws Exception {
            ChatRoomJoinRequest request = new ChatRoomJoinRequest(Position.AGREE, 1);
            willThrow(new BusinessException(ErrorCode.CHAT_ROOM_FULL))
                    .given(chatRoomService).joinChatRoom(eq(10L), eq(USER_ID), any());

            mockMvc.perform(post("/chat-rooms/10/members")
                            .with(SecurityMockMvcRequestPostProcessors.user(createUserDetails()))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("포지션 정원 초과이면 409를 반환한다")
        void joinChatRoom_positionFull() throws Exception {
            ChatRoomJoinRequest request = new ChatRoomJoinRequest(Position.AGREE, 1);
            willThrow(new BusinessException(ErrorCode.CHAT_ROOM_POSITION_FULL))
                    .given(chatRoomService).joinChatRoom(eq(10L), eq(USER_ID), any());

            mockMvc.perform(post("/chat-rooms/10/members")
                            .with(SecurityMockMvcRequestPostProcessors.user(createUserDetails()))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("존재하지 않는 채팅방이면 404를 반환한다")
        void joinChatRoom_roomNotFound() throws Exception {
            ChatRoomJoinRequest request = new ChatRoomJoinRequest(Position.AGREE, 1);
            willThrow(new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND))
                    .given(chatRoomService).joinChatRoom(eq(10L), eq(USER_ID), any());

            mockMvc.perform(post("/chat-rooms/10/members")
                            .with(SecurityMockMvcRequestPostProcessors.user(createUserDetails()))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("대기실 조회")
    class GetWaitingRoom {

        @Test
        @DisplayName("성공하면 200 OK와 WaitingRoomResponse를 반환한다")
        void getWaitingRoom_success() throws Exception {
            WaitingRoomResponse response = new WaitingRoomResponse(
                    10L, 1, 0, 2,
                    List.of(new WaitingRoomMemberItem("방장", "http://host.url", Position.AGREE, MemberRole.HOST))
            );
            given(chatRoomService.getWaitingRoom(10L, USER_ID)).willReturn(response);

            mockMvc.perform(get("/chat-rooms/10/waiting-room")
                            .with(SecurityMockMvcRequestPostProcessors.user(createUserDetails()))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.roomId").value(10))
                    .andExpect(jsonPath("$.data.agreeCount").value(1))
                    .andExpect(jsonPath("$.data.disagreeCount").value(0))
                    .andExpect(jsonPath("$.data.maxPerPosition").value(2))
                    .andExpect(jsonPath("$.data.members.length()").value(1));
        }

        @Test
        @DisplayName("채팅방이 없으면 404를 반환한다")
        void getWaitingRoom_roomNotFound() throws Exception {
            willThrow(new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND))
                    .given(chatRoomService).getWaitingRoom(10L, USER_ID);

            mockMvc.perform(get("/chat-rooms/10/waiting-room")
                            .with(SecurityMockMvcRequestPostProcessors.user(createUserDetails()))
                            .with(csrf()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("멤버가 아니면 404를 반환한다")
        void getWaitingRoom_memberNotFound() throws Exception {
            willThrow(new BusinessException(ErrorCode.CHAT_ROOM_MEMBER_NOT_FOUND))
                    .given(chatRoomService).getWaitingRoom(10L, USER_ID);

            mockMvc.perform(get("/chat-rooms/10/waiting-room")
                            .with(SecurityMockMvcRequestPostProcessors.user(createUserDetails()))
                            .with(csrf()))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("채팅 시작")
    class StartChatRoom {

        @Test
        @DisplayName("방장이 시작하면 200 OK와 멤버/라운드 정보를 반환한다")
        void startChatRoom_success() throws Exception {
            ChatRoomStartResponse response = new ChatRoomStartResponse(
                    "토론 주제",
                    List.of(new ChatStartMemberItem("독서왕", "https://example.com/profile.jpg")),
                    List.of(new ChatStartMemberItem("책벌레", null)),
                    1,
                    LocalDateTime.of(2026, 2, 17, 14, 30, 0)
            );
            given(chatRoomService.startChatRoom(10L, USER_ID)).willReturn(response);

            mockMvc.perform(patch("/chat-rooms/10")
                            .with(SecurityMockMvcRequestPostProcessors.user(createUserDetails()))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("OK"))
                    .andExpect(jsonPath("$.data.agreeMembers[0].nickname").value("독서왕"))
                    .andExpect(jsonPath("$.data.agreeMembers[0].profileImageUrl").value("https://example.com/profile.jpg"))
                    .andExpect(jsonPath("$.data.disagreeMembers[0].nickname").value("책벌레"))
                    .andExpect(jsonPath("$.data.currentRound").value(1))
                    .andExpect(jsonPath("$.data.startedAt").value("2026-02-17T14:30:00"));

            then(chatRoomService).should().startChatRoom(10L, USER_ID);
        }

        @Test
        @DisplayName("방장이 아니면 403을 반환한다")
        void startChatRoom_notHost() throws Exception {
            willThrow(new BusinessException(ErrorCode.CHAT_ROOM_NOT_HOST))
                    .given(chatRoomService).startChatRoom(10L, USER_ID);

            mockMvc.perform(patch("/chat-rooms/10")
                            .with(SecurityMockMvcRequestPostProcessors.user(createUserDetails()))
                            .with(csrf()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("채팅방이 없으면 404를 반환한다")
        void startChatRoom_roomNotFound() throws Exception {
            willThrow(new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND))
                    .given(chatRoomService).startChatRoom(999L, USER_ID);

            mockMvc.perform(patch("/chat-rooms/999")
                            .with(SecurityMockMvcRequestPostProcessors.user(createUserDetails()))
                            .with(csrf()))
                    .andExpect(status().isNotFound());
        }

        // TODO: 테스트 후 주석 해제
        // @Test
        // @DisplayName("상대 포지션 부족이면 409를 반환한다")
        // void startChatRoom_insufficientMembers() throws Exception {
        //     willThrow(new BusinessException(ErrorCode.CHAT_ROOM_INSUFFICIENT_MEMBERS))
        //             .given(chatRoomService).startChatRoom(10L, USER_ID);
        //
        //     mockMvc.perform(patch("/chat-rooms/10")
        //                     .with(SecurityMockMvcRequestPostProcessors.user(createUserDetails()))
        //                     .with(csrf()))
        //             .andExpect(status().isConflict());
        // }
    }

    @Nested
    @DisplayName("대기실 SSE 구독")
    class SubscribeWaitingRoom {

        @Test
        @DisplayName("성공하면 200 OK를 반환한다")
        void subscribeWaitingRoom_success() throws Exception {
            WaitingRoomResponse response = new WaitingRoomResponse(10L, 1, 0, 2, List.of());
            given(chatRoomService.getWaitingRoom(10L, USER_ID)).willReturn(response);
            given(waitingRoomSseService.subscribe(10L)).willReturn(new SseEmitter());

            mockMvc.perform(get("/chat-rooms/10/waiting-room/subscribe")
                            .with(SecurityMockMvcRequestPostProcessors.user(createUserDetails()))
                            .with(csrf())
                            .accept(MediaType.TEXT_EVENT_STREAM_VALUE))
                    .andExpect(status().isOk());
        }

    }

    @Nested
    @DisplayName("채팅방 상세 조회")
    class GetChatRoomDetail {

        @Test
        @DisplayName("성공하면 200 OK와 멤버/라운드 정보를 반환한다")
        void getChatRoomDetail_success() throws Exception {
            ChatRoomStartResponse response = new ChatRoomStartResponse(
                    "토론 주제",
                    List.of(new ChatStartMemberItem("독서왕", "https://example.com/profile.jpg")),
                    List.of(new ChatStartMemberItem("책벌레", null)),
                    1,
                    LocalDateTime.of(2026, 2, 17, 14, 30, 0)
            );
            given(chatRoomService.getChatRoomDetail(10L, USER_ID)).willReturn(response);

            mockMvc.perform(get("/chat-rooms/10")
                            .with(SecurityMockMvcRequestPostProcessors.user(createUserDetails()))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("OK"))
                    .andExpect(jsonPath("$.data.agreeMembers[0].nickname").value("독서왕"))
                    .andExpect(jsonPath("$.data.disagreeMembers[0].nickname").value("책벌레"))
                    .andExpect(jsonPath("$.data.currentRound").value(1))
                    .andExpect(jsonPath("$.data.startedAt").value("2026-02-17T14:30:00"));
        }

        @Test
        @DisplayName("채팅방이 없으면 404를 반환한다")
        void getChatRoomDetail_roomNotFound() throws Exception {
            willThrow(new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND))
                    .given(chatRoomService).getChatRoomDetail(999L, USER_ID);

            mockMvc.perform(get("/chat-rooms/999")
                            .with(SecurityMockMvcRequestPostProcessors.user(createUserDetails()))
                            .with(csrf()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("채팅 중이 아니면 409를 반환한다")
        void getChatRoomDetail_notChatting() throws Exception {
            willThrow(new BusinessException(ErrorCode.CHAT_ROOM_NOT_CHATTING))
                    .given(chatRoomService).getChatRoomDetail(10L, USER_ID);

            mockMvc.perform(get("/chat-rooms/10")
                            .with(SecurityMockMvcRequestPostProcessors.user(createUserDetails()))
                            .with(csrf()))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("멤버가 아니면 404를 반환한다")
        void getChatRoomDetail_memberNotFound() throws Exception {
            willThrow(new BusinessException(ErrorCode.CHAT_ROOM_MEMBER_NOT_FOUND))
                    .given(chatRoomService).getChatRoomDetail(10L, USER_ID);

            mockMvc.perform(get("/chat-rooms/10")
                            .with(SecurityMockMvcRequestPostProcessors.user(createUserDetails()))
                            .with(csrf()))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("메시지 목록 조회")
    class GetMessages {

        @Test
        @DisplayName("파라미터 없이 요청하면 200 OK와 메시지 목록을 반환한다")
        void getMessages_noParams_success() throws Exception {
            MessageListResponse response = new MessageListResponse(
                    List.of(new MessageResponse(100L, 1L, "독서왕", MessageType.TEXT, "안녕하세요!", null,
                            LocalDateTime.of(2026, 2, 17, 14, 35, 0))),
                    new PageInfo(null, false, 20)
            );
            given(messageService.getMessages(10L, USER_ID, null, 20)).willReturn(response);

            mockMvc.perform(get("/chat-rooms/10/messages")
                            .with(SecurityMockMvcRequestPostProcessors.user(createUserDetails()))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.messages").isArray())
                    .andExpect(jsonPath("$.data.messages.length()").value(1))
                    .andExpect(jsonPath("$.data.messages[0].messageId").value(100))
                    .andExpect(jsonPath("$.data.messages[0].senderNickname").value("독서왕"))
                    .andExpect(jsonPath("$.data.messages[0].messageType").value("TEXT"))
                    .andExpect(jsonPath("$.data.messages[0].textMessage").value("안녕하세요!"))
                    .andExpect(jsonPath("$.data.pageInfo.hasNext").value(false));
        }

        @Test
        @DisplayName("cursorId와 size를 지정하면 해당 파라미터로 조회한다")
        void getMessages_withParams_success() throws Exception {
            MessageListResponse response = new MessageListResponse(
                    List.of(new MessageResponse(99L, 1L, "독서왕", MessageType.TEXT, "이전 메시지", null,
                            LocalDateTime.of(2026, 2, 17, 14, 30, 0))),
                    new PageInfo(98L, true, 5)
            );
            given(messageService.getMessages(10L, USER_ID, 100L, 5)).willReturn(response);

            mockMvc.perform(get("/chat-rooms/10/messages")
                            .param("cursorId", "100")
                            .param("size", "5")
                            .with(SecurityMockMvcRequestPostProcessors.user(createUserDetails()))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.messages[0].messageId").value(99))
                    .andExpect(jsonPath("$.data.pageInfo.hasNext").value(true))
                    .andExpect(jsonPath("$.data.pageInfo.nextCursorId").value(98));
        }

        @Test
        @DisplayName("채팅방이 없으면 404를 반환한다")
        void getMessages_roomNotFound() throws Exception {
            willThrow(new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND))
                    .given(messageService).getMessages(999L, USER_ID, null, 20);

            mockMvc.perform(get("/chat-rooms/999/messages")
                            .with(SecurityMockMvcRequestPostProcessors.user(createUserDetails()))
                            .with(csrf()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("멤버가 아니면 404를 반환한다")
        void getMessages_memberNotFound() throws Exception {
            willThrow(new BusinessException(ErrorCode.CHAT_ROOM_MEMBER_NOT_FOUND))
                    .given(messageService).getMessages(10L, USER_ID, null, 20);

            mockMvc.perform(get("/chat-rooms/10/messages")
                            .with(SecurityMockMvcRequestPostProcessors.user(createUserDetails()))
                            .with(csrf()))
                    .andExpect(status().isNotFound());
        }

        @ParameterizedTest(name = "size={0}이면 400 Bad Request")
        @ValueSource(ints = {0, 21})
        void invalidSize_returns400(int size) throws Exception {
            mockMvc.perform(get("/chat-rooms/10/messages")
                            .param("size", String.valueOf(size))
                            .with(SecurityMockMvcRequestPostProcessors.user(createUserDetails()))
                            .with(csrf()))
                    .andExpect(status().isBadRequest());
        }

        @ParameterizedTest(name = "cursorId={0}이면 400 Bad Request")
        @ValueSource(ints = {0, -1})
        void invalidCursorId_returns400(int cursorId) throws Exception {
            mockMvc.perform(get("/chat-rooms/10/messages")
                            .param("cursorId", String.valueOf(cursorId))
                            .with(SecurityMockMvcRequestPostProcessors.user(createUserDetails()))
                            .with(csrf()))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("채팅방 종료")
    class EndChatRoom {

        @Test
        @DisplayName("방장이 종료하면 204 No Content를 반환한다")
        void endChatRoom_success() throws Exception {
            willDoNothing().given(chatRoomService).endChatRoom(10L, USER_ID);

            mockMvc.perform(delete("/chat-rooms/10")
                            .with(SecurityMockMvcRequestPostProcessors.user(createUserDetails()))
                            .with(csrf()))
                    .andExpect(status().isNoContent());

            then(chatRoomService).should().endChatRoom(10L, USER_ID);
        }

        @Test
        @DisplayName("방장이 아니면 403을 반환한다")
        void endChatRoom_notHost() throws Exception {
            willThrow(new BusinessException(ErrorCode.CHAT_ROOM_NOT_HOST))
                    .given(chatRoomService).endChatRoom(10L, USER_ID);

            mockMvc.perform(delete("/chat-rooms/10")
                            .with(SecurityMockMvcRequestPostProcessors.user(createUserDetails()))
                            .with(csrf()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("채팅방이 없으면 404를 반환한다")
        void endChatRoom_roomNotFound() throws Exception {
            willThrow(new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND))
                    .given(chatRoomService).endChatRoom(999L, USER_ID);

            mockMvc.perform(delete("/chat-rooms/999")
                            .with(SecurityMockMvcRequestPostProcessors.user(createUserDetails()))
                            .with(csrf()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("채팅 중이 아니면 409를 반환한다")
        void endChatRoom_notChatting() throws Exception {
            willThrow(new BusinessException(ErrorCode.CHAT_ROOM_NOT_CHATTING))
                    .given(chatRoomService).endChatRoom(10L, USER_ID);

            mockMvc.perform(delete("/chat-rooms/10")
                            .with(SecurityMockMvcRequestPostProcessors.user(createUserDetails()))
                            .with(csrf()))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("마지막 라운드가 아니면 409를 반환한다")
        void endChatRoom_notLastRound() throws Exception {
            willThrow(new BusinessException(ErrorCode.CHAT_ROOM_NOT_LAST_ROUND))
                    .given(chatRoomService).endChatRoom(10L, USER_ID);

            mockMvc.perform(delete("/chat-rooms/10")
                            .with(SecurityMockMvcRequestPostProcessors.user(createUserDetails()))
                            .with(csrf()))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("다음 라운드 전환")
    class NextRound {

        @Test
        @DisplayName("방장이 전환하면 204 No Content를 반환한다")
        void nextRound_success() throws Exception {
            willDoNothing().given(chatRoomService).nextRound(10L, USER_ID);

            mockMvc.perform(patch("/chat-rooms/10/next-round")
                            .with(SecurityMockMvcRequestPostProcessors.user(createUserDetails()))
                            .with(csrf()))
                    .andExpect(status().isNoContent());

            then(chatRoomService).should().nextRound(10L, USER_ID);
        }

        @Test
        @DisplayName("방장이 아니면 403을 반환한다")
        void nextRound_notHost() throws Exception {
            willThrow(new BusinessException(ErrorCode.CHAT_ROOM_NOT_HOST))
                    .given(chatRoomService).nextRound(10L, USER_ID);

            mockMvc.perform(patch("/chat-rooms/10/next-round")
                            .with(SecurityMockMvcRequestPostProcessors.user(createUserDetails()))
                            .with(csrf()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("채팅방이 없으면 404를 반환한다")
        void nextRound_roomNotFound() throws Exception {
            willThrow(new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND))
                    .given(chatRoomService).nextRound(999L, USER_ID);

            mockMvc.perform(patch("/chat-rooms/999/next-round")
                            .with(SecurityMockMvcRequestPostProcessors.user(createUserDetails()))
                            .with(csrf()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("채팅 중이 아니면 409를 반환한다")
        void nextRound_notChatting() throws Exception {
            willThrow(new BusinessException(ErrorCode.CHAT_ROOM_NOT_CHATTING))
                    .given(chatRoomService).nextRound(10L, USER_ID);

            mockMvc.perform(patch("/chat-rooms/10/next-round")
                            .with(SecurityMockMvcRequestPostProcessors.user(createUserDetails()))
                            .with(csrf()))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("최대 라운드에 도달하면 409를 반환한다")
        void nextRound_maxRoundReached() throws Exception {
            willThrow(new BusinessException(ErrorCode.CHAT_ROOM_MAX_ROUND_REACHED))
                    .given(chatRoomService).nextRound(10L, USER_ID);

            mockMvc.perform(patch("/chat-rooms/10/next-round")
                            .with(SecurityMockMvcRequestPostProcessors.user(createUserDetails()))
                            .with(csrf()))
                    .andExpect(status().isConflict());
        }
    }
}

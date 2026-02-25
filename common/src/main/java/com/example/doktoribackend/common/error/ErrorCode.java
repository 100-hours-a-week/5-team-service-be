package com.example.doktoribackend.common.error;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    // Common
    INVALID_INPUT_VALUE(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_FAILED", "요청 값이 유효하지 않습니다."),
    INVALID_REQUEST_BODY(HttpStatus.BAD_REQUEST, "INVALID_REQUEST_BODY", "요청 본문을 읽을 수 없습니다."),
    INVALID_PARAMETER_TYPE(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER_TYPE", "요청 파라미터 타입이 올바르지 않습니다."),
    MISSING_PARAMETER(HttpStatus.BAD_REQUEST, "MISSING_PARAMETER", "필수 요청 파라미터가 누락되었습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "지원하지 않는 HTTP 메서드입니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "리소스를 찾을 수 없습니다."),
    CONFLICT(HttpStatus.CONFLICT, "CONFLICT", "요청이 현재 리소스 상태와 충돌합니다."),
    OPTIMISTIC_LOCK_FAILURE(HttpStatus.CONFLICT, "OPTIMISTIC_LOCK_FAILURE", "리소스가 동시에 수정되었습니다. 다시 시도해주세요."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다."),

    // OAuth
    KAKAO_TOKEN_FETCH_FAILED(HttpStatus.UNAUTHORIZED, "KAKAO_TOKEN_FETCH_FAILED", "카카오 토큰 발급에 실패했습니다."),
    KAKAO_USER_INFO_FETCH_FAILED(HttpStatus.UNAUTHORIZED, "KAKAO_USER_INFO_FETCH_FAILED", "카카오 사용자 정보 조회에 실패했습니다."),
    UNSUPPORTED_PROVIDER(HttpStatus.BAD_REQUEST, "UNSUPPORTED_PROVIDER", "지원하지 않는 OAuth 제공자입니다."),
    INVALID_OAUTH_STATE(HttpStatus.UNAUTHORIZED, "INVALID_OAUTH_STATE", "OAuth 상태 값이 일치하지 않습니다."),

    // Auth
    AUTH_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_UNAUTHORIZED", "인증이 필요합니다."),
    AUTH_FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH_FORBIDDEN", "접근 권한이 없습니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED", "토큰이 만료되었습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "유효하지 않은 리프레시 토큰입니다."),
    INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_ACCESS_TOKEN", "유효하지 않은 액세스 토큰입니다."),
    NOT_EXIST_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "NOT_EXIST_REFRESH_TOKEN", "리프레시 토큰이 존재하지 않습니다."),
    INVALID_TOKEN_REUSE_DETECTED(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN_REUSE_DETECTED", "리프레시 토큰 재사용이 감지되었습니다. 모든 토큰이 무효화됩니다."),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_EXPIRED"," 리프레시 토큰이 만료되었습니다."),
    INVALID_USER_ID(HttpStatus.BAD_REQUEST, "INVALID_USER_ID", "유효하지 않은 사용자 ID입니다."),

    // External API
    UPSTREAM_KAKAO_FAILED(HttpStatus.BAD_GATEWAY, "UPSTREAM_KAKAO_FAILED", "카카오 API 호출에 실패했습니다."),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "존재하지 않는 사용자입니다."),
    PROFILE_ALREADY_COMPLETED(HttpStatus.CONFLICT, "PROFILE_ALREADY_COMPLETED", "이미 프로필 필수 정보를 완료했습니다."),
    ONBOARDING_ALREADY_COMPLETED(HttpStatus.CONFLICT, "ONBOARDING_ALREADY_COMPLETED", "이미 온보딩을 완료했습니다."),
    WITHDRAWAL_BLOCKED_ACTIVE_LEADER(HttpStatus.CONFLICT, "WITHDRAWAL_BLOCKED_ACTIVE_LEADER", "모임장으로 진행중인 모임이 있습니다. 모임장 위임 후 다시 진행해주세요."),

    // Policy
    POLICY_NOT_FOUND(HttpStatus.NOT_FOUND, "POLICY_NOT_FOUND", "정책을 찾을 수 없습니다."),

    //S3
    S3_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "S3_UPLOAD_FAILED", "파일 업로드에 실패했습니다."),
    S3_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "S3_DELETE_FAILED", "파일 삭제에 실패했습니다."),
    INVALID_IMG_URL(HttpStatus.INTERNAL_SERVER_ERROR, "INVALID_IMG_URL", "이미지 url이 올바르지 않습니다."),
    FILE_NAME_IS_NOT_BLANK(HttpStatus.BAD_REQUEST, "FILE_NAME_IS_NOT_BLANK", "파일 이름은 공백일 수 없습니다."),
    CONTENT_TYPE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "CONTENT_TYPE_NOT_ALLOWED", "허용되지 않는 컨텐츠 타입입니다."),
    INVALID_S3_KEY(HttpStatus.BAD_REQUEST, "INVALID_S3_KEY", "유효하지 않은 S3 키입니다."),
    FILE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "FILE_SIZE_EXCEEDED", "허용된 파일 크기를 초과했습니다."),
    INVALID_FILE_SIZE(HttpStatus.BAD_REQUEST, "INVALID_FILE_SIZE", "파일 크기가 유효하지 않습니다."),
    INVALID_FILE_EXTENSION(HttpStatus.BAD_REQUEST, "INVALID_FILE_EXTENSION", "허용되지 않은 파일 확장자입니다."),
    CONTENT_TYPE_MISMATCH(HttpStatus.BAD_REQUEST, "CONTENT_TYPE_MISMATCH", "파일 확장자와 콘텐츠 타입이 일치하지 않습니다."),

    // Book
    BOOK_NOT_FOUND(HttpStatus.NOT_FOUND, "BOOK_NOT_FOUND", "유효하지 않은 도서 정보입니다."),

    //Meeting
    MEETING_NOT_FOUND(HttpStatus.NOT_FOUND, "MEETING_NOT_FOUND", "존재하지 않는 모임입니다."),
    JOIN_REQUEST_ALREADY_EXISTS(HttpStatus.CONFLICT, "JOIN_REQUEST_ALREADY_EXISTS", "이미 참여 요청이 접수된 모임입니다."),
    JOIN_REQUEST_BLOCKED(HttpStatus.FORBIDDEN, "JOIN_REQUEST_BLOCKED", "해당 모임에 참여할 수 없습니다."),
    RECRUITMENT_CLOSED(HttpStatus.CONFLICT, "RECRUITMENT_CLOSED", "모집이 마감된 모임입니다."),
    CAPACITY_FULL(HttpStatus.CONFLICT, "CAPACITY_FULL", "모집 정원이 가득 찼습니다."),
    MEETING_UPDATE_FORBIDDEN(HttpStatus.FORBIDDEN, "MEETING_UPDATE_FORBIDDEN", "모임을 수정할 권한이 없습니다."),
    MEETING_UPDATE_NOT_ALLOWED(HttpStatus.CONFLICT, "MEETING_UPDATE_NOT_ALLOWED", "취소된 모임은 수정할 수 없습니다."),
    MEETING_ROUND_UPDATE_NOT_ALLOWED(HttpStatus.CONFLICT, "MEETING_ROUND_UPDATE_NOT_ALLOWED", "진행된 회차 또는 진행 중인 회차는 수정할 수 없습니다."),
    JOIN_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "JOIN_REQUEST_NOT_FOUND", "존재하지 않는 참여 요청입니다."),
    JOIN_REQUEST_ALREADY_PROCESSED(HttpStatus.CONFLICT, "JOIN_REQUEST_ALREADY_PROCESSED", "이미 처리된 참여 요청입니다."),
    PARTICIPATION_UPDATE_FORBIDDEN(HttpStatus.FORBIDDEN, "PARTICIPATION_UPDATE_FORBIDDEN", "참여 요청을 처리할 권한이 없습니다."),

    // LeaderDelegation
    LEADER_DELEGATION_FORBIDDEN(HttpStatus.FORBIDDEN, "LEADER_DELEGATION_FORBIDDEN", "모임장 위임 권한이 없습니다."),
    MEETING_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEETING_MEMBER_NOT_FOUND", "존재하지 않는 모임 멤버입니다."),
    DELEGATION_TARGET_NOT_APPROVED(HttpStatus.CONFLICT, "DELEGATION_TARGET_NOT_APPROVED", "승인된 모임원에게만 위임할 수 있습니다."),
    DELEGATION_TARGET_ALREADY_LEADER(HttpStatus.CONFLICT, "DELEGATION_TARGET_ALREADY_LEADER", "이미 모임장인 멤버에게 위임할 수 없습니다."),
    CANNOT_DELEGATE_TO_SELF(HttpStatus.CONFLICT, "CANNOT_DELEGATE_TO_SELF", "자기 자신에게 위임할 수 없습니다."),

    // Pagination
    PAGINATION_INVALID_CURSOR(HttpStatus.BAD_REQUEST, "PAGINATION_INVALID_CURSOR", "cursorId는 1 이상의 정수여야 합니다."),
    PAGINATION_SIZE_OUT_OF_RANGE(HttpStatus.BAD_REQUEST, "PAGINATION_SIZE_OUT_OF_RANGE", "size는 1~10 사이여야 합니다."),
    INVALID_STATUS_PARAMETER(HttpStatus.BAD_REQUEST, "INVALID_STATUS_PARAMETER", "status는 ACTIVE 또는 INACTIVE여야 합니다."),

    // Notification
    NOTIFICATION_TYPE_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTIFICATION_TYPE_NOT_FOUND", "알림 유형을 찾을 수 없습니다."),
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTIFICATION_NOT_FOUND", "알림을 찾을 수 없습니다."),

    // MeetingRound
    ROUND_NOT_FOUND(HttpStatus.NOT_FOUND, "ROUND_NOT_FOUND", "모임 회차를 찾을 수 없습니다."),

    // TopicRecommendation
    TOPIC_RECOMMENDATION_FORBIDDEN(HttpStatus.FORBIDDEN, "TOPIC_RECOMMENDATION_FORBIDDEN", "토론 주제 추천 권한이 없습니다."),
    NO_BOOK_REPORTS_FOR_TOPIC(HttpStatus.CONFLICT, "NO_BOOK_REPORTS_FOR_TOPIC", "제출된 독후감이 없어 AI 추천을 할 수 없습니다."),
    TOPIC_RECOMMENDATION_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "TOPIC_RECOMMENDATION_LIMIT_EXCEEDED", "일일 AI 추천 횟수(15회)를 초과했습니다."),
    TOPIC_REQUIRED_FOR_LEADER_MODE(HttpStatus.BAD_REQUEST, "TOPIC_REQUIRED_FOR_LEADER_MODE", "LEADER 모드에서는 주제 입력이 필수입니다."),
    AI_TOPIC_RECOMMENDATION_FAILED(HttpStatus.BAD_GATEWAY, "AI_TOPIC_RECOMMENDATION_FAILED", "AI 주제 추천에 실패했습니다."),

    // BookReport
    BOOK_REPORT_ALREADY_SUBMITTED(HttpStatus.CONFLICT, "BOOK_REPORT_ALREADY_SUBMITTED", "이미 독후감을 제출했습니다."),
    BOOK_REPORT_NOT_WRITABLE(HttpStatus.CONFLICT, "BOOK_REPORT_NOT_WRITABLE", "독후감 작성 가능 시간이 아닙니다."),
    DAILY_SUBMISSION_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "DAILY_SUBMISSION_LIMIT_EXCEEDED", "일일 독후감 제출 횟수(3회)를 초과했습니다."),
    BOOK_REPORT_MANAGEMENT_FORBIDDEN(HttpStatus.FORBIDDEN, "BOOK_REPORT_MANAGEMENT_FORBIDDEN", "독후감 관리 권한이 없습니다."),
    BOOK_REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "BOOK_REPORT_NOT_FOUND", "독후감을 찾을 수 없습니다."),

    // ChatRoom
    CHAT_ROOM_INVALID_CAPACITY(HttpStatus.BAD_REQUEST, "CHAT_ROOM_INVALID_CAPACITY", "채팅방 정원은 2, 4, 6명만 가능합니다."),
    CHAT_ROOM_ALREADY_JOINED(HttpStatus.CONFLICT, "CHAT_ROOM_ALREADY_JOINED", "이미 참여 중인 채팅방이 있어 새 채팅방을 생성할 수 없습니다."),
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT_ROOM_NOT_FOUND", "존재하지 않는 채팅방입니다."),
    CHAT_ROOM_ALREADY_ENDED(HttpStatus.CONFLICT, "CHAT_ROOM_ALREADY_ENDED", "이미 종료되거나 취소된 채팅방입니다."),
    CHAT_ROOM_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT_ROOM_MEMBER_NOT_FOUND", "채팅방 멤버가 아닙니다."),
    CHAT_ROOM_ALREADY_LEFT(HttpStatus.CONFLICT, "CHAT_ROOM_ALREADY_LEFT", "이미 나간 채팅방입니다."),
    CHAT_ROOM_FULL(HttpStatus.CONFLICT, "CHAT_ROOM_FULL", "채팅방 정원이 가득 찼습니다."),
    CHAT_ROOM_POSITION_FULL(HttpStatus.CONFLICT, "CHAT_ROOM_POSITION_FULL", "해당 포지션의 정원이 가득 찼습니다."),
    CHAT_ROOM_QUIZ_WRONG_ANSWER(HttpStatus.FORBIDDEN, "CHAT_ROOM_QUIZ_WRONG_ANSWER", "퀴즈 정답이 아닙니다."),
    CHAT_ROOM_QUIZ_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT_ROOM_QUIZ_NOT_FOUND", "채팅방에 퀴즈가 존재하지 않습니다."),
    CHAT_ROOM_NOT_WAITING(HttpStatus.CONFLICT, "CHAT_ROOM_NOT_WAITING", "대기 중인 채팅방만 참여할 수 있습니다."),
    CHAT_ROOM_NOT_HOST(HttpStatus.FORBIDDEN, "CHAT_ROOM_NOT_HOST", "방장만 채팅을 시작할 수 있습니다."),
    CHAT_ROOM_INSUFFICIENT_MEMBERS(HttpStatus.CONFLICT, "CHAT_ROOM_INSUFFICIENT_MEMBERS", "상대 포지션에 최소 1명 이상의 멤버가 필요합니다."),
    CHAT_ROOM_NOT_CHATTING(HttpStatus.CONFLICT, "CHAT_ROOM_NOT_CHATTING", "채팅 중인 채팅방이 아닙니다."),
    CHAT_ROOM_ROUND_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT_ROOM_ROUND_NOT_FOUND", "진행 중인 라운드가 없습니다."),
    CHAT_ROOM_MAX_ROUND_REACHED(HttpStatus.CONFLICT, "CHAT_ROOM_MAX_ROUND_REACHED", "최대 라운드에 도달했습니다."),
    CHAT_ROOM_NOT_LAST_ROUND(HttpStatus.CONFLICT, "CHAT_ROOM_NOT_LAST_ROUND", "마지막 라운드에서만 채팅을 종료할 수 있습니다."),

    // Vote
    VOTE_NOT_FOUND(HttpStatus.NOT_FOUND, "VOTE_NOT_FOUND", "투표를 찾을 수 없습니다."),
    VOTE_ALREADY_CLOSED(HttpStatus.CONFLICT, "VOTE_ALREADY_CLOSED", "이미 종료된 투표입니다."),
    VOTE_ALREADY_CAST(HttpStatus.CONFLICT, "VOTE_ALREADY_CAST", "이미 투표하였습니다.");


    private final HttpStatus status;
    private final String code;
    private final String message;
}

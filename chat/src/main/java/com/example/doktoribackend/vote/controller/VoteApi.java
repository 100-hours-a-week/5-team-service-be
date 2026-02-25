package com.example.doktoribackend.vote.controller;

import com.example.doktoribackend.common.response.ApiResult;
import com.example.doktoribackend.common.swagger.AuthErrorResponses;
import com.example.doktoribackend.common.swagger.CommonErrorResponses;
import com.example.doktoribackend.security.CustomUserDetails;
import com.example.doktoribackend.vote.dto.VoteCastRequest;
import com.example.doktoribackend.vote.dto.VoteResultResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Vote", description = "투표 API")
public interface VoteApi {

    @CommonErrorResponses
    @AuthErrorResponses
    @Operation(summary = "투표 참여", description = "채팅 종료 후 투표에 참여합니다. 투표 시작 후 1분 이내에만 가능합니다.")
    @ApiResponse(responseCode = "204", description = "No Content")
    @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(mediaType = "application/json",
                    examples = {
                            @ExampleObject(name = "투표 없음",
                                    value = """
                                            {
                                              "code": "VOTE_NOT_FOUND",
                                              "message": "투표를 찾을 수 없습니다."
                                            }
                                            """),
                            @ExampleObject(name = "멤버 아님",
                                    value = """
                                            {
                                              "code": "CHAT_ROOM_MEMBER_NOT_FOUND",
                                              "message": "채팅방 멤버가 아닙니다."
                                            }
                                            """)
                    }))
    @ApiResponse(responseCode = "409", description = "Conflict",
            content = @Content(mediaType = "application/json",
                    examples = {
                            @ExampleObject(name = "투표 종료",
                                    value = """
                                            {
                                              "code": "VOTE_ALREADY_CLOSED",
                                              "message": "이미 종료된 투표입니다."
                                            }
                                            """),
                            @ExampleObject(name = "중복 투표",
                                    value = """
                                            {
                                              "code": "VOTE_ALREADY_CAST",
                                              "message": "이미 투표하였습니다."
                                            }
                                            """)
                    }))
    ResponseEntity<ApiResult<Void>> castVote(
            @Parameter(hidden = true) CustomUserDetails userDetails,
            @Parameter(description = "채팅방 ID", example = "1") Long roomId,
            VoteCastRequest request);

    @CommonErrorResponses
    @AuthErrorResponses
    @Operation(summary = "투표 결과 조회", description = "채팅 종료 후 투표 결과를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {
                              "message": "OK",
                              "data": {
                                "agreeCount": 2,
                                "disagreeCount": 1,
                                "totalMemberCount": 4,
                                "isClosed": false,
                                "myChoice": "AGREE"
                              }
                            }
                            """)))
    @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(mediaType = "application/json",
                    examples = {
                            @ExampleObject(name = "투표 없음",
                                    value = """
                                            {
                                              "code": "VOTE_NOT_FOUND",
                                              "message": "투표를 찾을 수 없습니다."
                                            }
                                            """),
                            @ExampleObject(name = "멤버 아님",
                                    value = """
                                            {
                                              "code": "CHAT_ROOM_MEMBER_NOT_FOUND",
                                              "message": "채팅방 멤버가 아닙니다."
                                            }
                                            """)
                    }))
    ResponseEntity<ApiResult<VoteResultResponse>> getVoteResult(
            @Parameter(hidden = true) CustomUserDetails userDetails,
            @Parameter(description = "채팅방 ID", example = "1") Long roomId);
}

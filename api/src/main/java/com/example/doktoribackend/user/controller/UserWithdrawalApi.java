package com.example.doktoribackend.user.controller;

import com.example.doktoribackend.common.swagger.AuthErrorResponses;
import com.example.doktoribackend.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;

@Tag(name = "User", description = "사용자 API")
public interface UserWithdrawalApi {

    @AuthErrorResponses
    @Operation(
            summary = "회원 탈퇴",
            description = "본인 계정을 탈퇴 처리합니다. 성공 시 204 No Content를 반환하고 쿠키를 삭제합니다."
    )
    @ApiResponse(
            responseCode = "204",
            description = "No Content - 탈퇴 성공"
    )
    @ApiResponse(
            responseCode = "409",
            description = "Conflict",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {
                              "code": "WITHDRAWAL_BLOCKED_ACTIVE_LEADER",
                              "message": "모임장으로 진행중인 모임이 있습니다. 모임장 위임 후 다시 진행해주세요."
                            }
                            """)
            )
    )
    ResponseEntity<Void> withdraw(
            @Parameter(hidden = true) CustomUserDetails userDetails,
            HttpServletResponse response
    );
}

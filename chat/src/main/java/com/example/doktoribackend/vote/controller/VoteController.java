package com.example.doktoribackend.vote.controller;

import com.example.doktoribackend.common.response.ApiResult;
import com.example.doktoribackend.security.CustomUserDetails;
import com.example.doktoribackend.vote.dto.VoteCastRequest;
import com.example.doktoribackend.vote.dto.VoteResultResponse;
import com.example.doktoribackend.vote.service.VoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chat-rooms/{roomId}/vote")
public class VoteController implements VoteApi {

    private final VoteService voteService;

    @PostMapping
    @Override
    public ResponseEntity<ApiResult<Void>> castVote(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long roomId,
            @Valid @RequestBody VoteCastRequest request
    ) {
        voteService.castVote(roomId, userDetails.getId(), request.choice());
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Override
    public ResponseEntity<ApiResult<VoteResultResponse>> getVoteResult(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long roomId
    ) {
        VoteResultResponse response = voteService.getVoteResult(roomId, userDetails.getId());
        return ResponseEntity.ok(ApiResult.ok(response));
    }
}

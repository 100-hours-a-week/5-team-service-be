package com.example.doktoribackend.user.policy;

import com.example.doktoribackend.common.response.ApiResult;
import com.example.doktoribackend.user.policy.dto.ReadingPolicyResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Policy", description = "정책 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/policies")
public class PolicyController {

    private final PolicyService policyService;

    @Operation(summary = "독서량 정책 조회", description = "삭제되지 않은 독서량 정책을 조회합니다.")
    @GetMapping("/reading-volumes")
    public ResponseEntity<ApiResult<List<ReadingPolicyResponse>>> getReadingVolumes() {
        return ResponseEntity.ok(ApiResult.ok(policyService.getReadingVolumePolicies()));
    }

    @Operation(summary = "독서 목적 정책 조회", description = "삭제되지 않은 독서 목적 정책을 조회합니다.")
    @GetMapping("/reading-purposes")
    public ResponseEntity<ApiResult<List<ReadingPolicyResponse>>> getReadingPurposes() {
        return ResponseEntity.ok(ApiResult.ok(policyService.getReadingPurposePolicies()));
    }

    @Operation(summary = "독서 장르 정책 조회", description = "삭제되지 않은 독서 장르 정책을 조회합니다.")
    @GetMapping("/reading-genres")
    public ResponseEntity<ApiResult<List<ReadingPolicyResponse>>> getReadingGenres() {
        return ResponseEntity.ok(ApiResult.ok(policyService.getReadingGenrePolicies()));
    }
}

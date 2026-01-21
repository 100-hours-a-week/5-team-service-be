package com.example.doktoribackend.common.controller;

import com.example.doktoribackend.common.response.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Health", description = "헬스 체크 API")
@RestController
public class HealthController {

    @Operation(summary = "헬스 체크")
    @GetMapping("/health")
    public ResponseEntity<ApiResult<Void>> health() {
        return ResponseEntity.ok(ApiResult.ok());
    }
}

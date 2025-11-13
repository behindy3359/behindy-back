package com.example.backend.controller.multiplayer;

import com.example.backend.dto.multiplayer.UserStatsResponse;
import com.example.backend.service.multiplayer.UserStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "멀티플레이어 통계 API", description = "사용자 멀티플레이어 통계 조회 API")
@Slf4j
@RestController
@RequestMapping("/api/multiplayer/stats")
@RequiredArgsConstructor
public class MultiplayerStatsController {

    private final UserStatsService statsService;

    @Operation(summary = "내 통계 조회", description = "현재 로그인한 사용자의 멀티플레이어 통계를 조회합니다")
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserStatsResponse> getMyStats() {
        UserStatsResponse stats = statsService.getMyStats();
        return ResponseEntity.ok(stats);
    }

    @Operation(summary = "사용자 통계 조회", description = "특정 사용자의 멀티플레이어 통계를 조회합니다")
    @GetMapping("/users/{userId}")
    public ResponseEntity<UserStatsResponse> getUserStats(@PathVariable Long userId) {
        UserStatsResponse stats = statsService.getUserStats(userId);
        return ResponseEntity.ok(stats);
    }
}

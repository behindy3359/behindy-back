package com.example.backend.controller;

import com.example.backend.dto.auth.ApiResponse;
import com.example.backend.service.AIStoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/ai-stories")
@RequiredArgsConstructor
public class AIStoryController {

    private final AIStoryService aiStoryService;

    @GetMapping("/health")
    public ResponseEntity<ApiResponse> checkLLMServerHealth() {
        boolean isHealthy = aiStoryService.isLLMServerHealthySync();

        return ResponseEntity.ok(ApiResponse.builder()
                .success(isHealthy)
                .message(isHealthy ? "healthy" : "unhealthy")
                .build());
    }
}
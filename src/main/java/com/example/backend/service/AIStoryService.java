package com.example.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIStoryService {

    @Qualifier("llmWebClient")
    private final WebClient llmWebClient;

    public Mono<Boolean> isLLMServerHealthy() {
        return llmWebClient.get()
                .uri("/health")
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> true)
                .timeout(Duration.ofSeconds(30))
                .onErrorReturn(false);
    }

    public boolean isLLMServerHealthySync() {
        try {
            return isLLMServerHealthy().block(Duration.ofSeconds(30));
        } catch (Exception e) {
            log.warn("LLM server health check failed: {}", e.getMessage());
            return false;
        }
    }
}

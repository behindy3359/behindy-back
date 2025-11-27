package com.example.backend.dto.multiplayer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LlmStoryResponse {
    private StoryContent story;
    private List<CharacterEffect> effects;
    private Integer phase;
    private Boolean isEnding;
    private String storyOutline;
    private String phaseSummary;      // 이번 Phase 요약 (다음 요청에 포함)
    private String endingSummary;     // 엔딩 시 전체 스토리 요약

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StoryContent {
        private String currentSituation;  // 현재 상황 묘사
        private String specialEvent;      // 이번 특별한 이벤트
        private String hint;               // 플레이어 행동 유도 힌트
    }
}

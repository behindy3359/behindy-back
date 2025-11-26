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

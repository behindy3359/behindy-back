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
    private String phaseSummary;
    private String endingSummary;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StoryContent {
        private String currentSituation;
        private String specialEvent;
        private String hint;
    }
}

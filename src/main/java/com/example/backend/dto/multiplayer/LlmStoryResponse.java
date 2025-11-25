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
    private String storyText;
    private List<CharacterEffect> effects;
    private Integer phase;
    private Boolean isEnding;
    private String storyOutline;
}

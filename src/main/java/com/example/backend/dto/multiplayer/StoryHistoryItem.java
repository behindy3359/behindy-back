package com.example.backend.dto.multiplayer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoryHistoryItem {
    private Integer phase;
    private String summary;
}

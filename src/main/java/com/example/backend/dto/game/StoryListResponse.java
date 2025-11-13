package com.example.backend.dto.game;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoryListResponse {
    private List<StoryResponse> stories;
    private String stationName;
    private Integer stationLine;
    private boolean hasActiveGame;
}

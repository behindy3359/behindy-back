package com.example.backend.dto.game;

import com.example.backend.dto.character.CharacterResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameStateResponse {
    private boolean hasActiveGame;
    private Long storyId;
    private String storyTitle;
    private PageResponse currentPage;
    private CharacterResponse character;
    private LocalDateTime gameStartTime;
    private String message;
}

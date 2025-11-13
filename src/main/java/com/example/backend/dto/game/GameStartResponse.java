package com.example.backend.dto.game;

import com.example.backend.dto.character.CharacterResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameStartResponse {
    private Long storyId;
    private String storyTitle;
    private PageResponse currentPage;
    private CharacterResponse character;
    private String message;
}
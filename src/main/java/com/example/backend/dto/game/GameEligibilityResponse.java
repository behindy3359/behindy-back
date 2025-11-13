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
public class GameEligibilityResponse {
    private boolean canStartGame;
    private String reason;
    private boolean requiresLogin;
    private boolean requiresCharacterCreation;
    private boolean hasActiveGame;
    private CharacterResponse character;
}

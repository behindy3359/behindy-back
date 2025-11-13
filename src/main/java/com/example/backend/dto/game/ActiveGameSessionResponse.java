package com.example.backend.dto.game;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActiveGameSessionResponse {
    private Long characterId;
    private String characterName;
    private String userName;
    private Long storyId;
    private String storyTitle;
    private Long currentPageNumber;
    private LocalDateTime gameStartTime;
}

package com.example.backend.dto.game;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameStartRequest {
    @NotNull(message = "스토리 ID는 필수입니다.")
    private Long storyId;
}

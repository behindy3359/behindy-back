package com.example.backend.dto.character;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "캐릭터 정보 응답")
public class CharacterResponse {
    @Schema(description = "캐릭터 ID", example = "1")
    private Long charId;

    @Schema(description = "캐릭터 이름", example = "모험가")
    private String charName;

    @Schema(description = "체력 (0-100)", example = "80")
    private Integer charHealth;

    @Schema(description = "정신력 (0-100)", example = "60")
    private Integer charSanity;

    @Schema(description = "생존 여부", example = "true")
    private boolean isAlive;

    @Schema(description = "위험 상태 여부 (체력/정신력 < 30)", example = "false")
    private boolean isDying;

    @Schema(description = "상태 메시지", example = "건강함")
    private String statusMessage;

    @Schema(description = "게임 진행 중 여부", example = "true")
    private boolean hasGameProgress;

    @Schema(description = "현재 플레이 중인 스토리 ID", example = "10", nullable = true)
    private Long currentStoryId;

    @Schema(description = "캐릭터 생성 시간", example = "2025-10-26T10:00:00")
    private LocalDateTime createdAt;
}
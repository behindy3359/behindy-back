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
@Schema(description = "캐릭터 게임 상태 응답")
public class CharacterGameStatusResponse {

    @Schema(description = "캐릭터 ID", example = "1")
    private Long charId;

    @Schema(description = "캐릭터 이름", example = "모험가")
    private String charName;

    @Schema(description = "체력 (0-100)", example = "80")
    private Integer charHealth;

    @Schema(description = "정신력 (0-100)", example = "60")
    private Integer charSanity;

    @Schema(description = "생존 여부", example = "true")
    private boolean alive;

    @Schema(description = "상태 메시지", example = "건강함")
    private String statusMessage;

    @Schema(description = "현재 진행 중인 게임 존재 여부", example = "true")
    private boolean hasActiveGame;

    @Schema(description = "현재 플레이 중인 스토리 ID", example = "1", nullable = true)
    private Long currentStoryId;

    @Schema(description = "현재 플레이 중인 스토리 제목", example = "강남역의 미스터리", nullable = true)
    private String currentStoryTitle;

    @Schema(description = "현재 페이지 번호", example = "5", nullable = true)
    private Long currentPageNumber;

    @Schema(description = "게임 시작 시간", example = "2025-10-26T10:00:00", nullable = true)
    private LocalDateTime gameStartTime;

    @Schema(description = "스토리 클리어 횟수", example = "3")
    private Long totalClears;

    @Schema(description = "총 플레이 횟수", example = "5")
    private Long totalPlays;

    @Schema(description = "클리어율 (%)", example = "60.0")
    private Double clearRate;

    @Schema(description = "새 게임 진입 가능 여부", example = "false")
    private boolean canEnterNewGame;

    @Schema(description = "새 게임 진입 불가 사유", example = "이미 진행 중인 게임이 있습니다.", nullable = true)
    private String cannotEnterReason;
}
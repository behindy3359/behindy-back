package com.example.backend.dto.game;

import com.example.backend.dto.character.CharacterResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "선택 결과 응답")
public class ChoiceResultResponse {
    @Schema(description = "성공 여부", example = "true")
    private boolean success;

    @Schema(description = "선택 결과 설명", example = "용기를 내어 소리가 나는 쪽으로 다가갔다...")
    private String result;

    @Schema(description = "업데이트된 캐릭터 정보", nullable = true)
    private CharacterResponse updatedCharacter;

    @Schema(description = "다음 페이지 정보", nullable = true)
    private PageResponse nextPage;

    @Schema(description = "게임 종료 여부", example = "false")
    private boolean isGameOver;

    @Schema(description = "게임 종료 이유 (스토리 완료, 캐릭터 사망 등)", example = "스토리 완료", nullable = true)
    private String gameOverReason;

    @Schema(description = "응답 메시지", example = "다음 페이지로 이동합니다")
    private String message;
}

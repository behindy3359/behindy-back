package com.example.backend.dto.game;

import com.example.backend.dto.character.CharacterResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "게임 진입 응답")
public class GameEnterResponse {
    @Schema(description = "성공 여부", example = "true")
    private boolean success;

    @Schema(description = "게임 진입 액션 타입", example = "START_NEW",
            allowableValues = {"START_NEW", "RESUME_EXISTING", "NO_STORIES", "CHARACTER_REQUIRED"})
    private String action;

    @Schema(description = "응답 메시지", example = "새로운 스토리를 시작합니다")
    private String message;

    @Schema(description = "선택된 스토리 ID (START_NEW 액션 시)", example = "1", nullable = true)
    private Long selectedStoryId;

    @Schema(description = "선택된 스토리 제목 (START_NEW 액션 시)", example = "강남역의 미스터리", nullable = true)
    private String selectedStoryTitle;

    @Schema(description = "첫 번째 페이지 (START_NEW 액션 시)", nullable = true)
    private PageResponse firstPage;

    @Schema(description = "재개할 스토리 ID (RESUME_EXISTING 액션 시)", example = "1", nullable = true)
    private Long resumeStoryId;

    @Schema(description = "재개할 스토리 제목 (RESUME_EXISTING 액션 시)", example = "강남역의 미스터리", nullable = true)
    private String resumeStoryTitle;

    @Schema(description = "현재 페이지 (RESUME_EXISTING 액션 시)", nullable = true)
    private PageResponse currentPage;

    @Schema(description = "캐릭터 정보")
    private CharacterResponse character;

    @Schema(description = "역 이름", example = "강남역")
    private String stationName;

    @Schema(description = "호선 번호", example = "2")
    private Integer stationLine;

    @Schema(description = "선택 가능한 스토리 목록 (필요 시)", nullable = true)
    private List<StoryResponse> availableStories;
}
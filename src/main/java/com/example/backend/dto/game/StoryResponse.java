package com.example.backend.dto.game;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "스토리 정보 응답")
public class StoryResponse {
    @Schema(description = "스토리 ID", example = "1")
    private Long storyId;

    @Schema(description = "스토리 제목", example = "강남역의 미스터리")
    private String storyTitle;

    @Schema(description = "예상 플레이 시간 (분)", example = "10")
    private Integer estimatedLength;

    @Schema(description = "난이도 (쉬움, 보통, 어려움)", example = "보통")
    private String difficulty;

    @Schema(description = "테마 (공포, 로맨스, 미스터리 등)", example = "공포")
    private String theme;

    @Schema(description = "스토리 설명", example = "강남역에서 펼쳐지는 공포 장르의 텍스트 어드벤처입니다...")
    private String description;

    @Schema(description = "역 이름", example = "강남역")
    private String stationName;

    @Schema(description = "호선 번호", example = "2")
    private Integer stationLine;

    @Schema(description = "플레이 가능 여부", example = "true")
    private boolean canPlay;

    @Schema(description = "플레이 상태 (플레이 가능, 플레이 불가, 로그인 필요 등)", example = "플레이 가능")
    private String playStatus;
}
package com.example.backend.dto.character;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "방문한 역 정보 응답")
public class VisitedStationResponse {
    @Schema(description = "역 이름", example = "강남역")
    private String stationName;

    @Schema(description = "호선 번호", example = "2")
    private Integer stationLine;

    @Schema(description = "방문 횟수 (클리어 횟수)", example = "3")
    private Long visitCount;

    @Schema(description = "총 플레이 횟수 (클리어 + 실패)", example = "5")
    private Long totalPlayCount;

    @Schema(description = "클리어율 (%)", example = "60.0")
    private Double clearRate;

    @Schema(description = "최근 방문 시간", example = "2025-10-26T10:00:00")
    private String lastVisitedAt;

    @Schema(description = "방문 등급 (BRONZE, SILVER, GOLD, PLATINUM)", example = "SILVER")
    private String visitBadge;
}

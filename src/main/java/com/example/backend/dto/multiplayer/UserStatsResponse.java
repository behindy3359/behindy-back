package com.example.backend.dto.multiplayer;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "사용자 통계 응답")
public class UserStatsResponse {

    @Schema(description = "사용자 ID", example = "10")
    private Long userId;

    @Schema(description = "총 참여 횟수", example = "15")
    private Integer totalParticipations;

    @Schema(description = "총 완료 횟수", example = "10")
    private Integer totalCompletions;

    @Schema(description = "총 사망 횟수", example = "3")
    private Integer totalDeaths;

    @Schema(description = "총 추방 횟수", example = "2")
    private Integer totalKicks;

    @Schema(description = "완료율 (%)", example = "66.67")
    private Double completionRate;
}

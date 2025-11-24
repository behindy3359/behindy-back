package com.example.backend.dto.multiplayer;

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
@Schema(description = "멀티플레이어 방 투표 정보")
public class RoomVoteResponse {

    @Schema(description = "투표 ID", example = "5")
    private Long voteId;

    @Schema(description = "방 ID", example = "10")
    private Long roomId;

    @Schema(description = "투표 유형", example = "KICK")
    private String voteType;

    @Schema(description = "대상 사용자 ID", example = "15")
    private Long targetUserId;

    @Schema(description = "대상 사용자 이름", example = "문익점")
    private String targetUsername;

    @Schema(description = "투표 생성자 ID", example = "8")
    private Long initiatedByUserId;

    @Schema(description = "투표 생성자 이름", example = "홍길동")
    private String initiatedByUsername;

    @Schema(description = "투표 상태", example = "PENDING")
    private String status;

    @Schema(description = "투표 시작 시각")
    private LocalDateTime createdAt;

    @Schema(description = "투표 만료 시각")
    private LocalDateTime expiresAt;

    @Schema(description = "찬성 표 수", example = "2")
    private long yesCount;

    @Schema(description = "반대 표 수", example = "0")
    private long noCount;

    @Schema(description = "필요한 총 투표 수", example = "2")
    private long requiredVotes;
}

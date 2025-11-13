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
@Schema(description = "방 참여자 정보")
public class ParticipantResponse {

    @Schema(description = "참여자 ID", example = "1")
    private Long participantId;

    @Schema(description = "사용자 ID", example = "10")
    private Long userId;

    @Schema(description = "캐릭터 ID", example = "5")
    private Long characterId;

    @Schema(description = "캐릭터 이름", example = "모험가")
    private String characterName;

    @Schema(description = "현재 체력", example = "80")
    private Integer hp;

    @Schema(description = "현재 정신력", example = "90")
    private Integer sanity;

    @Schema(description = "활성 상태", example = "true")
    private Boolean isActive;

    @Schema(description = "참가 시간", example = "2025-11-10T10:00:00")
    private LocalDateTime joinedAt;
}

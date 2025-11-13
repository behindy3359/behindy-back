package com.example.backend.dto.multiplayer;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "방 정보 응답")
public class RoomResponse {

    @Schema(description = "방 ID", example = "1")
    private Long roomId;

    @Schema(description = "방 이름", example = "강남역 모험")
    private String roomName;

    @Schema(description = "역 ID", example = "5")
    private Long stationId;

    @Schema(description = "역 이름", example = "강남역")
    private String stationName;

    @Schema(description = "현재 인원", example = "2")
    private Integer currentPlayers;

    @Schema(description = "최대 인원", example = "3")
    private Integer maxPlayers;

    @Schema(description = "현재 Phase", example = "1")
    private Integer currentPhase;

    @Schema(description = "방 상태", example = "PLAYING")
    private String status;

    @Schema(description = "방장 사용자 ID", example = "10")
    private Long ownerId;

    @Schema(description = "방장 캐릭터 이름", example = "모험가")
    private String ownerCharacterName;

    @Schema(description = "참여자 목록")
    private List<ParticipantResponse> participants;

    @Schema(description = "생성 시간", example = "2025-11-10T10:00:00")
    private LocalDateTime createdAt;
}

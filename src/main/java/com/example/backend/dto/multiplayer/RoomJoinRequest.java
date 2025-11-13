package com.example.backend.dto.multiplayer;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "방 참가 요청")
public class RoomJoinRequest {

    @NotNull(message = "캐릭터 ID는 필수입니다")
    @Schema(description = "캐릭터 ID", example = "1", required = true)
    private Long characterId;
}

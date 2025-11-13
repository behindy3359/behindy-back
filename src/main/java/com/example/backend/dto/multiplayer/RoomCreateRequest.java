package com.example.backend.dto.multiplayer;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "방 생성 요청")
public class RoomCreateRequest {

    @NotNull(message = "역 ID는 필수입니다")
    @Schema(description = "역 ID", example = "1", required = true)
    private Long stationId;

    @NotNull(message = "캐릭터 ID는 필수입니다")
    @Schema(description = "캐릭터 ID", example = "1", required = true)
    private Long characterId;

    @NotBlank(message = "방 이름은 필수입니다")
    @Size(min = 2, max = 50, message = "방 이름은 2-50자여야 합니다")
    @Schema(description = "방 이름", example = "강남역 모험", required = true)
    private String roomName;
}

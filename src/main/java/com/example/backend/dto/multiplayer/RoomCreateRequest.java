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

    @Schema(description = "역 ID (선택)", example = "1")
    private Long stationId;

    @Schema(description = "역 이름 (stationId가 없을 때 사용)", example = "강남")
    private String stationName;

    @Schema(description = "노선 번호 (stationId가 없을 때 사용)", example = "2")
    private Integer lineNumber;

    @NotNull(message = "캐릭터 ID는 필수입니다")
    @Schema(description = "캐릭터 ID", example = "1", required = true)
    private Long characterId;

    @NotBlank(message = "방 이름은 필수입니다")
    @Size(min = 2, max = 50, message = "방 이름은 2-50자여야 합니다")
    @Schema(description = "방 이름", example = "강남역 모험", required = true)
    private String roomName;
}

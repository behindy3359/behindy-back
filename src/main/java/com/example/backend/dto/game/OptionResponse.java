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
@Schema(description = "선택지 정보 응답")
public class OptionResponse {
    @Schema(description = "선택지 ID", example = "1")
    private Long optionId;

    @Schema(description = "선택지 내용", example = "소리가 나는 쪽으로 가본다")
    private String content;

    @Schema(description = "효과 타입 (health, sanity, both, none)", example = "health", nullable = true)
    private String effect;

    @Schema(description = "효과 수치 (양수: 증가, 음수: 감소)", example = "-10", nullable = true)
    private Integer amount;

    @Schema(description = "효과 미리보기 텍스트", example = "체력 -10", nullable = true)
    private String effectPreview;
}

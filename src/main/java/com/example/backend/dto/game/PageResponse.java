package com.example.backend.dto.game;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "게임 페이지 정보 응답")
public class PageResponse {
    @Schema(description = "페이지 ID", example = "1")
    private Long pageId;

    @Schema(description = "페이지 번호 (1부터 시작)", example = "1")
    private Long pageNumber;

    @Schema(description = "페이지 내용 (스토리 텍스트)", example = "어두운 지하철역에 도착했다. 이상한 소리가 들린다...")
    private String content;

    @Schema(description = "선택 가능한 옵션 목록")
    private List<OptionResponse> options;

    @Schema(description = "마지막 페이지 여부", example = "false")
    private boolean isLastPage;

    @Schema(description = "전체 페이지 수", example = "10")
    private Integer totalPages;
}

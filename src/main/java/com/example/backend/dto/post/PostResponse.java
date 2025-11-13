package com.example.backend.dto.post;

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
@Schema(description = "게시글 응답")
public class PostResponse {
    @Schema(description = "게시글 ID", example = "1")
    private Long id;

    @Schema(description = "게시글 제목", example = "강남역 스토리 공략법")
    private String title;

    @Schema(description = "게시글 내용", example = "이번에 강남역 스토리를 클리어했습니다...")
    private String content;

    @Schema(description = "작성자 이름", example = "모험가123")
    private String authorName;

    @Schema(description = "작성자 ID", example = "1")
    private Long authorId;

    @Schema(description = "조회수", example = "42")
    private Long viewCount;

    @Schema(description = "댓글 개수", example = "5")
    private Integer commentCount;

    @Schema(description = "현재 사용자가 수정 가능한지 여부", example = "true")
    private boolean isEditable;

    @Schema(description = "현재 사용자가 삭제 가능한지 여부", example = "true")
    private boolean isDeletable;

    @Schema(description = "작성 시간", example = "2025-10-26T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "수정 시간", example = "2025-10-26T11:00:00")
    private LocalDateTime updatedAt;
}

package com.example.backend.dto.comment;

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
@Schema(description = "댓글 응답")
public class CommentResponse {
    @Schema(description = "댓글 ID", example = "1")
    private Long id;

    @Schema(description = "게시글 ID", example = "1")
    private Long postId;

    @Schema(description = "댓글 내용", example = "좋은 공략 감사합니다!")
    private String content;

    @Schema(description = "작성자 이름", example = "탐험가456")
    private String authorName;

    @Schema(description = "작성자 ID", example = "2")
    private Long authorId;

    @Schema(description = "현재 사용자가 수정 가능한지 여부", example = "false")
    private boolean isEditable;

    @Schema(description = "현재 사용자가 삭제 가능한지 여부", example = "false")
    private boolean isDeletable;

    @Schema(description = "좋아요 개수", example = "3")
    private Long likeCount;

    @Schema(description = "현재 사용자가 좋아요를 눌렀는지 여부", example = "true")
    private boolean isLiked;

    @Schema(description = "작성 시간", example = "2025-10-26T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "수정 시간", example = "2025-10-26T10:30:00")
    private LocalDateTime updatedAt;
}
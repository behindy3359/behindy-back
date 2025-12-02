package com.example.backend.controller;

import com.example.backend.dto.comment.CommentCreateRequest;
import com.example.backend.dto.comment.CommentResponse;
import com.example.backend.dto.comment.CommentUpdateRequest;
import com.example.backend.dto.common.PageResponse;
import com.example.backend.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "댓글 API", description = "게시글 댓글 작성, 조회, 수정, 삭제 관련 API")
@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "댓글 작성", description = "게시글에 새로운 댓글을 작성합니다. 인증된 사용자만 사용할 수 있습니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "댓글 작성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음")
    })
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommentResponse> createComment(
            @Valid @RequestBody CommentCreateRequest request) {
        CommentResponse response = commentService.createComment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "댓글 단건 조회", description = "댓글 ID를 사용하여 특정 댓글의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "댓글 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "댓글을 찾을 수 없음")
    })
    @GetMapping("/{commentId}")
    public ResponseEntity<CommentResponse> getComment(
            @Parameter(description = "조회할 댓글 ID", required = true) @PathVariable Long commentId) {
        CommentResponse response = commentService.getCommentById(commentId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "특정 게시글의 댓글 목록 조회", description = "특정 게시글에 달린 모든 댓글을 페이지네이션하여 조회합니다. 최신 댓글이 먼저 표시됩니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "댓글 목록 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음")
    })
    @GetMapping("/posts/{postId}")
    public ResponseEntity<PageResponse<CommentResponse>> getCommentsByPost(
            @Parameter(description = "댓글을 조회할 게시글 ID", required = true) @PathVariable Long postId,
            @Parameter(description = "페이지 번호 (0부터 시작, 기본값: 0)", required = false) @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지당 댓글 수 (기본값: 10)", required = false) @RequestParam(defaultValue = "10") int size) {

        PageResponse<CommentResponse> response = commentService.getCommentsByPost(postId, page, size);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "댓글 수정", description = "작성자 본인이 자신의 댓글을 수정합니다. 인증된 사용자만 사용할 수 있습니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "댓글 수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "수정 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "댓글을 찾을 수 없음")
    })
    @PutMapping("/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommentResponse> updateComment(
            @Parameter(description = "수정할 댓글 ID", required = true) @PathVariable Long commentId,
            @Valid @RequestBody CommentUpdateRequest request) {

        CommentResponse response = commentService.updateComment(commentId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "댓글 삭제", description = "작성자 본인이 자신의 댓글을 삭제합니다. 소프트 삭제로 처리되어 '삭제된 댓글입니다'로 표시됩니다. 인증된 사용자만 사용할 수 있습니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "댓글 삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "삭제 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "댓글을 찾을 수 없음")
    })
    @DeleteMapping("/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteComment(
            @Parameter(description = "삭제할 댓글 ID", required = true) @PathVariable Long commentId) {
        commentService.deleteComment(commentId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "내가 작성한 댓글 목록 조회", description = "현재 로그인한 사용자가 작성한 모든 댓글을 페이지네이션하여 조회합니다. 인증된 사용자만 사용할 수 있습니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "댓글 목록 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<CommentResponse>> getMyComments(
            @Parameter(description = "페이지 번호 (0부터 시작, 기본값: 0)", required = false) @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지당 댓글 수 (기본값: 10)", required = false) @RequestParam(defaultValue = "10") int size) {

        PageResponse<CommentResponse> response = commentService.getMyComments(page, size);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "댓글 좋아요 토글", description = "댓글에 좋아요를 추가하거나 제거합니다. 이미 좋아요를 눌렀다면 취소되고, 누르지 않았다면 추가됩니다. 인증된 사용자만 사용할 수 있습니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "좋아요 토글 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "댓글을 찾을 수 없음")
    })
    
    @PostMapping("/{commentId}/like")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommentResponse> toggleLike(
            @Parameter(description = "좋아요를 토글할 댓글 ID", required = true) @PathVariable Long commentId) {
        CommentResponse response = commentService.toggleLike(commentId);
        return ResponseEntity.ok(response);
    }
}
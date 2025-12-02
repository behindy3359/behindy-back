package com.example.backend.service;

import com.example.backend.dto.comment.CommentCreateRequest;
import com.example.backend.dto.comment.CommentListResponse;
import com.example.backend.dto.comment.CommentResponse;
import com.example.backend.dto.comment.CommentUpdateRequest;
import com.example.backend.entity.Comment;
import com.example.backend.entity.CommentLike;
import com.example.backend.entity.Post;
import com.example.backend.entity.User;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.CommentLikeRepository;
import com.example.backend.repository.CommentRepository;
import com.example.backend.repository.PostRepository;
import com.example.backend.service.mapper.EntityDtoMapper;
import com.example.backend.util.HtmlSanitizer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final PostRepository postRepository;
    private final HtmlSanitizer htmlSanitizer;
    private final AuthService authService;
    private final EntityDtoMapper entityDtoMapper;

    @Transactional
    public CommentResponse createComment(CommentCreateRequest request) {
        User currentUser = authService.getCurrentUser();

        Post post = postRepository.findById(request.getPostId())
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", request.getPostId()));

        String sanitizedContent = htmlSanitizer.sanitize(request.getContent());

        Comment comment = Comment.builder()
                .user(currentUser)
                .post(post)
                .cmtContents(sanitizedContent)
                .build();

        Comment savedComment = commentRepository.save(comment);

        return entityDtoMapper.toCommentResponse(savedComment);
    }

    @Transactional(readOnly = true)
    public CommentResponse getCommentById(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));

        return entityDtoMapper.toCommentResponse(comment);
    }

    @Transactional(readOnly = true)
    public CommentListResponse getCommentsByPost(Long postId, int page, int size) {
        Post post = postRepository.findById(postId)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Comment> commentsPage = commentRepository.findByPostIdAndNotDeleted(postId, pageable);

        List<CommentResponse> comments = commentsPage.getContent().stream()
                .map(entityDtoMapper::toCommentResponse)
                .collect(Collectors.toList());

        return CommentListResponse.builder()
                .comments(comments)
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .totalElements(commentsPage.getTotalElements())
                .totalPages(commentsPage.getTotalPages())
                .hasNext(commentsPage.hasNext())
                .hasPrevious(commentsPage.hasPrevious())
                .build();
    }

    @Transactional
    public CommentResponse updateComment(Long commentId, CommentUpdateRequest request) {
        Comment comment = commentRepository.findById(commentId)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));

        User currentUser = authService.getCurrentUser();

        if (!comment.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new AccessDeniedException("댓글을 수정할 권한이 없습니다.");
        }

        String sanitizedContent = htmlSanitizer.sanitize(request.getContent());
        comment.setCmtContents(sanitizedContent);

        Comment updatedComment = commentRepository.save(comment);

        return entityDtoMapper.toCommentResponse(updatedComment);
    }

    @Transactional
    public void deleteComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));

        User currentUser = authService.getCurrentUser();

        if (!comment.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new AccessDeniedException("댓글을 삭제할 권한이 없습니다.");
        }

        comment.delete();
        commentRepository.save(comment);
    }

    @Transactional(readOnly = true)
    public CommentListResponse getMyComments(int page, int size) {
        User currentUser = authService.getCurrentUser();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Comment> commentsPage = commentRepository.findByUserActive(currentUser, pageable);

        List<CommentResponse> comments = commentsPage.getContent().stream()
                .map(entityDtoMapper::toCommentResponse)
                .collect(Collectors.toList());

        return CommentListResponse.builder()
                .comments(comments)
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .totalElements(commentsPage.getTotalElements())
                .totalPages(commentsPage.getTotalPages())
                .hasNext(commentsPage.hasNext())
                .hasPrevious(commentsPage.hasPrevious())
                .build();
    }

    @Transactional
    public CommentResponse toggleLike(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));

        User currentUser = authService.getCurrentUser();

        var existingLike = commentLikeRepository.findByCommentAndUser(comment, currentUser);

        if (existingLike.isPresent()) {
            commentLikeRepository.delete(existingLike.get());
        } else {
            CommentLike newLike = CommentLike.builder()
                    .comment(comment)
                    .user(currentUser)
                    .build();
            commentLikeRepository.save(newLike);
        }

        return entityDtoMapper.toCommentResponse(comment);
    }
}
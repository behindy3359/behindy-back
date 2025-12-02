package com.example.backend.repository;

import com.example.backend.entity.Comment;
import com.example.backend.entity.User;
import com.example.backend.repository.common.UserOwnedSoftDeleteRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface CommentRepository extends UserOwnedSoftDeleteRepository<Comment, Long> {

    @Query("SELECT c FROM Comment c WHERE c.post.postId = :postId AND c.deletedAt IS NULL ORDER BY c.createdAt DESC")
    Page<Comment> findByPostIdAndNotDeleted(@Param("postId") Long postId, Pageable pageable);

    @Query("SELECT COUNT(c) FROM Comment c WHERE c.post.postId = :postId AND c.deletedAt IS NULL")
    Long countByPostIdAndNotDeleted(@Param("postId") Long postId);

    @Query("SELECT c FROM Comment c WHERE c.user = :user")
    java.util.List<Comment> findByUser(@Param("user") User user);

    Long countByDeletedAtIsNull();
    Long countByCreatedAtAfter(LocalDateTime date);

}
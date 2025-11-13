package com.example.backend.repository;

import com.example.backend.entity.Comment;
import com.example.backend.entity.CommentLike;
import com.example.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

    boolean existsByCommentAndUser(Comment comment, User user);

    Optional<CommentLike> findByCommentAndUser(Comment comment, User user);

    long countByComment(Comment comment);

    @Query("SELECT COUNT(cl) FROM CommentLike cl WHERE cl.comment.cmtId = :commentId")
    long countByCommentId(@Param("commentId") Long commentId);

    @Query("SELECT CASE WHEN COUNT(cl) > 0 THEN true ELSE false END " +
           "FROM CommentLike cl WHERE cl.comment.cmtId = :commentId AND cl.user.userId = :userId")
    boolean existsByCommentIdAndUserId(@Param("commentId") Long commentId, @Param("userId") Long userId);
}

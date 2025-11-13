package com.example.backend.repository;

import com.example.backend.entity.Post;
import com.example.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    @Query("SELECT p FROM Post p WHERE p.deletedAt IS NULL")
    Page<Post> findAllActive(Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.user = :user AND p.deletedAt IS NULL")
    Page<Post> findByUserAndDeletedAtIsNull(@Param("user") User user, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.user = :user")
    java.util.List<Post> findByUser(@Param("user") User user);

    Long countByDeletedAtIsNull();
    Long countByCreatedAtAfter(LocalDateTime date);
}

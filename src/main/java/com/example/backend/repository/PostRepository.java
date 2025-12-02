package com.example.backend.repository;

import com.example.backend.entity.Post;
import com.example.backend.entity.User;
import com.example.backend.repository.common.UserOwnedSoftDeleteRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface PostRepository extends UserOwnedSoftDeleteRepository<Post, Long> {

    @Query("SELECT p FROM Post p WHERE p.user = :user")
    java.util.List<Post> findByUser(@Param("user") User user);

    Long countByDeletedAtIsNull();
    Long countByCreatedAtAfter(LocalDateTime date);
}

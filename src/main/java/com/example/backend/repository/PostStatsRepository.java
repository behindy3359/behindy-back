package com.example.backend.repository;

import com.example.backend.entity.PostStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostStatsRepository extends JpaRepository<PostStats, Long> {

    Optional<PostStats> findByPostId(Long postId);
}

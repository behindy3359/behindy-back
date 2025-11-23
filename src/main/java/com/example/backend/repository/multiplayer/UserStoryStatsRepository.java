package com.example.backend.repository.multiplayer;

import com.example.backend.entity.multiplayer.UserStoryStats;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserStoryStatsRepository extends JpaRepository<UserStoryStats, Long> {

    @Query("SELECT s FROM UserStoryStats s WHERE s.userId = :userId")
    Optional<UserStoryStats> findByUserId(@Param("userId") Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM UserStoryStats s WHERE s.userId = :userId")
    Optional<UserStoryStats> findByUserIdForUpdate(@Param("userId") Long userId);

    @Query("SELECT s FROM UserStoryStats s ORDER BY s.totalCompletions DESC")
    List<UserStoryStats> findTopByCompletions(Pageable pageable);

    @Query("SELECT s FROM UserStoryStats s ORDER BY s.totalParticipations DESC")
    List<UserStoryStats> findTopByParticipations(Pageable pageable);
}

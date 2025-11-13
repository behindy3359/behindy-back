package com.example.backend.repository.multiplayer;

import com.example.backend.entity.multiplayer.StoryCompletion;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoryCompletionRepository extends JpaRepository<StoryCompletion, Long> {

    @Query("SELECT c FROM StoryCompletion c WHERE c.room.roomId = :roomId")
    Optional<StoryCompletion> findByRoomId(@Param("roomId") Long roomId);

    @Query("SELECT c FROM StoryCompletion c WHERE c.stationId = :stationId ORDER BY c.completedAt DESC")
    List<StoryCompletion> findByStationIdOrderByCompletedAtDesc(@Param("stationId") Long stationId, Pageable pageable);

    @Query("SELECT c FROM StoryCompletion c ORDER BY c.completedAt DESC")
    List<StoryCompletion> findRecentCompletions(Pageable pageable);

    @Query("SELECT COUNT(c) FROM StoryCompletion c WHERE c.stationId = :stationId")
    long countByStationId(@Param("stationId") Long stationId);
}

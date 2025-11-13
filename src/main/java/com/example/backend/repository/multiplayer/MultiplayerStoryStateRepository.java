package com.example.backend.repository.multiplayer;

import com.example.backend.entity.multiplayer.MultiplayerStoryState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MultiplayerStoryStateRepository extends JpaRepository<MultiplayerStoryState, Long> {

    @Query("SELECT s FROM MultiplayerStoryState s WHERE s.room.roomId = :roomId ORDER BY s.phase DESC")
    List<MultiplayerStoryState> findByRoomIdOrderByPhaseDesc(@Param("roomId") Long roomId);

    @Query("SELECT s FROM MultiplayerStoryState s WHERE s.room.roomId = :roomId AND s.phase = :phase")
    Optional<MultiplayerStoryState> findByRoomIdAndPhase(@Param("roomId") Long roomId, @Param("phase") Integer phase);

    @Query("SELECT s FROM MultiplayerStoryState s WHERE s.room.roomId = :roomId ORDER BY s.phase DESC LIMIT 1")
    Optional<MultiplayerStoryState> findLatestByRoomId(@Param("roomId") Long roomId);

    @Query("SELECT COUNT(s) FROM MultiplayerStoryState s WHERE s.room.roomId = :roomId")
    long countByRoomId(@Param("roomId") Long roomId);
}

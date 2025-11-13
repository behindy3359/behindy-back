package com.example.backend.repository.multiplayer;

import com.example.backend.entity.multiplayer.MultiplayerRoom;
import com.example.backend.entity.multiplayer.RoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MultiplayerRoomRepository extends JpaRepository<MultiplayerRoom, Long> {

    @Query("SELECT r FROM MultiplayerRoom r WHERE r.station.staId = :stationId AND r.status = :status")
    List<MultiplayerRoom> findByStationIdAndStatus(@Param("stationId") Long stationId, @Param("status") RoomStatus status);

    @Query("SELECT r FROM MultiplayerRoom r WHERE r.station.staId = :stationId AND r.status IN :statuses")
    List<MultiplayerRoom> findByStationIdAndStatusIn(@Param("stationId") Long stationId, @Param("statuses") List<RoomStatus> statuses);

    @Query("SELECT r FROM MultiplayerRoom r WHERE r.status = :status ORDER BY r.createdAt DESC")
    List<MultiplayerRoom> findByStatusOrderByCreatedAtDesc(@Param("status") RoomStatus status);

    @Query("SELECT COUNT(r) FROM MultiplayerRoom r WHERE r.station.staId = :stationId AND r.status IN ('WAITING', 'PLAYING')")
    long countActiveRoomsByStationId(@Param("stationId") Long stationId);

    @Query("SELECT r FROM MultiplayerRoom r " +
           "LEFT JOIN FETCH r.participants p " +
           "WHERE r.roomId = :roomId")
    Optional<MultiplayerRoom> findByIdWithParticipants(@Param("roomId") Long roomId);

    @Query("SELECT r FROM MultiplayerRoom r " +
           "LEFT JOIN FETCH r.participants p " +
           "WHERE r.station.staId = :stationId AND r.status = :status")
    List<MultiplayerRoom> findByStationIdAndStatusWithParticipants(@Param("stationId") Long stationId, @Param("status") RoomStatus status);
}

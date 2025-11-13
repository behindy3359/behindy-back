package com.example.backend.repository.multiplayer;

import com.example.backend.entity.multiplayer.RoomParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomParticipantRepository extends JpaRepository<RoomParticipant, Long> {

    @Query("SELECT p FROM RoomParticipant p WHERE p.room.roomId = :roomId AND p.isActive = true")
    List<RoomParticipant> findActiveParticipantsByRoomId(@Param("roomId") Long roomId);

    @Query("SELECT p FROM RoomParticipant p WHERE p.room.roomId = :roomId AND p.user.userId = :userId AND p.isActive = true")
    Optional<RoomParticipant> findByRoomIdAndUserId(@Param("roomId") Long roomId, @Param("userId") Long userId);

    @Query("SELECT COUNT(p) FROM RoomParticipant p WHERE p.room.roomId = :roomId AND p.isActive = true")
    long countActiveParticipantsByRoomId(@Param("roomId") Long roomId);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END " +
           "FROM RoomParticipant p WHERE p.room.roomId = :roomId AND p.user.userId = :userId AND p.isActive = true")
    boolean existsByRoomIdAndUserId(@Param("roomId") Long roomId, @Param("userId") Long userId);

    @Query("SELECT p FROM RoomParticipant p " +
           "LEFT JOIN FETCH p.character c " +
           "WHERE p.room.roomId = :roomId AND p.isActive = true")
    List<RoomParticipant> findActiveParticipantsWithCharacter(@Param("roomId") Long roomId);

    @Query("SELECT p FROM RoomParticipant p WHERE p.user.userId = :userId AND p.isActive = true")
    List<RoomParticipant> findActiveParticipantsByUserId(@Param("userId") Long userId);
}

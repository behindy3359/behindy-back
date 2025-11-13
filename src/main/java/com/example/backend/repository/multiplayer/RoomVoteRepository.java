package com.example.backend.repository.multiplayer;

import com.example.backend.entity.multiplayer.RoomVote;
import com.example.backend.entity.multiplayer.VoteStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomVoteRepository extends JpaRepository<RoomVote, Long> {

    @Query("SELECT v FROM RoomVote v WHERE v.room.roomId = :roomId AND v.status = :status")
    Optional<RoomVote> findByRoomIdAndStatus(@Param("roomId") Long roomId, @Param("status") VoteStatus status);

    @Query("SELECT CASE WHEN COUNT(v) > 0 THEN true ELSE false END " +
           "FROM RoomVote v WHERE v.room.roomId = :roomId AND v.status = 'PENDING'")
    boolean hasPendingVote(@Param("roomId") Long roomId);

    @Query("SELECT v FROM RoomVote v " +
           "LEFT JOIN FETCH v.ballots b " +
           "WHERE v.voteId = :voteId")
    Optional<RoomVote> findByIdWithBallots(@Param("voteId") Long voteId);

    @Query("SELECT v FROM RoomVote v WHERE v.status = 'PENDING' AND v.expiresAt <= :now")
    List<RoomVote> findExpiredVotes(@Param("now") LocalDateTime now);

    @Query("SELECT v FROM RoomVote v WHERE v.room.roomId = :roomId ORDER BY v.createdAt DESC")
    List<RoomVote> findByRoomIdOrderByCreatedAtDesc(@Param("roomId") Long roomId);
}

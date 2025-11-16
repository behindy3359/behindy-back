package com.example.backend.repository.multiplayer;

import com.example.backend.entity.multiplayer.VoteBallot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VoteBallotRepository extends JpaRepository<VoteBallot, Long> {

    @Query("SELECT b FROM VoteBallot b WHERE b.roomVote.voteId = :voteId AND b.user.userId = :userId")
    Optional<VoteBallot> findByVoteIdAndUserId(@Param("voteId") Long voteId, @Param("userId") Long userId);

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END " +
           "FROM VoteBallot b WHERE b.roomVote.voteId = :voteId AND b.user.userId = :userId")
    boolean existsByVoteIdAndUserId(@Param("voteId") Long voteId, @Param("userId") Long userId);

    @Query("SELECT b FROM VoteBallot b WHERE b.roomVote.voteId = :voteId")
    List<VoteBallot> findByVoteId(@Param("voteId") Long voteId);

    @Query("SELECT COUNT(b) FROM VoteBallot b WHERE b.roomVote.voteId = :voteId AND b.vote = true")
    long countYesVotes(@Param("voteId") Long voteId);

    @Query("SELECT COUNT(b) FROM VoteBallot b WHERE b.roomVote.voteId = :voteId AND b.vote = false")
    long countNoVotes(@Param("voteId") Long voteId);

    @Query("SELECT COUNT(b) FROM VoteBallot b WHERE b.roomVote.voteId = :voteId")
    long countByVoteId(@Param("voteId") Long voteId);
}

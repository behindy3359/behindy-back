package com.example.backend.service.multiplayer;

import com.example.backend.entity.User;
import com.example.backend.entity.multiplayer.*;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.multiplayer.*;
import com.example.backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoteService {

    private static final long VOTE_DURATION_MINUTES = 1;

    private final RoomVoteRepository voteRepository;
    private final VoteBallotRepository ballotRepository;
    private final MultiplayerRoomRepository roomRepository;
    private final RoomParticipantRepository participantRepository;
    private final AuthService authService;

    @Transactional
    public Long startKickVote(Long roomId, Long targetUserId) {
        User currentUser = authService.getCurrentUser();

        MultiplayerRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("방을 찾을 수 없습니다"));

        if (!room.getOwner().getUserId().equals(currentUser.getUserId())) {
            throw new AccessDeniedException("방장만 추방 투표를 시작할 수 있습니다");
        }

        if (currentUser.getUserId().equals(targetUserId)) {
            throw new IllegalArgumentException("자기 자신을 추방할 수 없습니다");
        }

        boolean targetIsParticipant = participantRepository
                .existsByRoomIdAndUserId(roomId, targetUserId);
        if (!targetIsParticipant) {
            throw new IllegalArgumentException("대상자가 방 참가자가 아닙니다");
        }

        if (voteRepository.hasPendingVote(roomId)) {
            throw new IllegalStateException("이미 진행 중인 투표가 있습니다");
        }

        User targetUser = participantRepository
                .findByRoomIdAndUserId(roomId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("대상자를 찾을 수 없습니다"))
                .getUser();

        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(VOTE_DURATION_MINUTES);

        RoomVote vote = RoomVote.builder()
                .room(room)
                .voteType(VoteType.KICK)
                .targetUser(targetUser)
                .initiatedBy(currentUser)
                .status(VoteStatus.PENDING)
                .expiresAt(expiresAt)
                .build();

        vote = voteRepository.save(vote);

        log.info("Kick vote started in room {} for user {}", roomId, targetUserId);

        return vote.getVoteId();
    }

    @Transactional
    public VoteResult submitBallot(Long voteId, boolean voteValue) {
        User currentUser = authService.getCurrentUser();

        RoomVote vote = voteRepository.findByIdWithBallots(voteId)
                .orElseThrow(() -> new ResourceNotFoundException("투표를 찾을 수 없습니다"));

        if (vote.getStatus() != VoteStatus.PENDING) {
            throw new IllegalStateException("이미 종료된 투표입니다");
        }

        if (vote.isExpired()) {
            vote.expire();
            voteRepository.save(vote);
            throw new IllegalStateException("투표 시간이 만료되었습니다");
        }

        if (vote.getTargetUser().getUserId().equals(currentUser.getUserId())) {
            throw new IllegalArgumentException("투표 대상자는 투표할 수 없습니다");
        }

        boolean isParticipant = participantRepository
                .existsByRoomIdAndUserId(vote.getRoom().getRoomId(), currentUser.getUserId());
        if (!isParticipant) {
            throw new IllegalStateException("방 참가자만 투표할 수 있습니다");
        }

        if (ballotRepository.existsByVoteIdAndUserId(voteId, currentUser.getUserId())) {
            throw new IllegalStateException("이미 투표했습니다");
        }

        VoteBallot ballot = VoteBallot.builder()
                .roomVote(vote)
                .user(currentUser)
                .vote(voteValue)
                .build();

        ballotRepository.save(ballot);

        long activeParticipants = participantRepository
                .countActiveParticipantsByRoomId(vote.getRoom().getRoomId());
        long requiredVotes = activeParticipants - 1;
        long currentVotes = ballotRepository.countByVoteId(voteId);

        VoteResult result = new VoteResult();
        result.setVoteId(voteId);
        result.setCurrentVotes(currentVotes);
        result.setRequiredVotes(requiredVotes);

        if (currentVotes >= requiredVotes) {
            long yesVotes = ballotRepository.countYesVotes(voteId);

            if (yesVotes == requiredVotes) {
                vote.pass();
                result.setStatus(VoteStatus.PASSED);

                RoomParticipant targetParticipant = participantRepository
                        .findByRoomIdAndUserId(vote.getRoom().getRoomId(),
                                             vote.getTargetUser().getUserId())
                        .orElseThrow();
                targetParticipant.leave();
                participantRepository.save(targetParticipant);

                log.info("Vote {} passed - user {} kicked from room {}",
                    voteId, vote.getTargetUser().getUserId(), vote.getRoom().getRoomId());
            } else {
                vote.fail();
                result.setStatus(VoteStatus.FAILED);
                log.info("Vote {} failed", voteId);
            }

            voteRepository.save(vote);
        } else {
            result.setStatus(VoteStatus.PENDING);
        }

        return result;
    }

    @Transactional
    public void processExpiredVotes() {
        List<RoomVote> expiredVotes = voteRepository.findExpiredVotes(LocalDateTime.now());

        for (RoomVote vote : expiredVotes) {
            if (vote.getStatus() == VoteStatus.PENDING) {
                vote.expire();
                voteRepository.save(vote);
                log.info("Vote {} expired", vote.getVoteId());
            }
        }
    }

    public static class VoteResult {
        private Long voteId;
        private VoteStatus status;
        private long currentVotes;
        private long requiredVotes;

        public Long getVoteId() { return voteId; }
        public void setVoteId(Long voteId) { this.voteId = voteId; }

        public VoteStatus getStatus() { return status; }
        public void setStatus(VoteStatus status) { this.status = status; }

        public long getCurrentVotes() { return currentVotes; }
        public void setCurrentVotes(long currentVotes) { this.currentVotes = currentVotes; }

        public long getRequiredVotes() { return requiredVotes; }
        public void setRequiredVotes(long requiredVotes) { this.requiredVotes = requiredVotes; }
    }
}

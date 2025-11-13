package com.example.backend.entity.multiplayer;

import com.example.backend.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
@Table(name = "room_vote",
    indexes = {
        @Index(name = "idx_vote_room_status", columnList = "room_id, status")
    }
)
public class RoomVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vote_id")
    private Long voteId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private MultiplayerRoom room;

    @Enumerated(EnumType.STRING)
    @Column(name = "vote_type", nullable = false, length = 20)
    private VoteType voteType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id", nullable = false)
    private User targetUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiated_by_user_id", nullable = false)
    private User initiatedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private VoteStatus status = VoteStatus.PENDING;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @OneToMany(mappedBy = "vote", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<VoteBallot> ballots = new ArrayList<>();

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public int getYesCount() {
        return (int) ballots.stream().filter(VoteBallot::getVote).count();
    }

    public int getNoCount() {
        return (int) ballots.stream().filter(b -> !b.getVote()).count();
    }

    public void pass() {
        this.status = VoteStatus.PASSED;
    }

    public void fail() {
        this.status = VoteStatus.FAILED;
    }

    public void expire() {
        this.status = VoteStatus.EXPIRED;
    }
}

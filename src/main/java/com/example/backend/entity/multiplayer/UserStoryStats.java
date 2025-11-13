package com.example.backend.entity.multiplayer;

import com.example.backend.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
@Table(name = "user_story_stats",
    indexes = {
        @Index(name = "idx_stats_completions", columnList = "total_completions DESC")
    }
)
public class UserStoryStats {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "total_participations", nullable = false)
    @Builder.Default
    private Integer totalParticipations = 0;

    @Column(name = "total_completions", nullable = false)
    @Builder.Default
    private Integer totalCompletions = 0;

    @Column(name = "total_deaths", nullable = false)
    @Builder.Default
    private Integer totalDeaths = 0;

    @Column(name = "total_kicks", nullable = false)
    @Builder.Default
    private Integer totalKicks = 0;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void incrementParticipations() {
        this.totalParticipations++;
    }

    public void incrementCompletions() {
        this.totalCompletions++;
    }

    public void incrementDeaths() {
        this.totalDeaths++;
    }

    public void incrementKicks() {
        this.totalKicks++;
    }

    public double getCompletionRate() {
        if (totalParticipations == 0) return 0.0;
        return (double) totalCompletions / totalParticipations * 100;
    }
}

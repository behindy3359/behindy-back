package com.example.backend.entity.multiplayer;

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
@Table(name = "story_completion",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_completion_room", columnNames = {"room_id"})
    },
    indexes = {
        @Index(name = "idx_completion_station", columnList = "station_id"),
        @Index(name = "idx_completion_date", columnList = "completed_at DESC")
    }
)
public class StoryCompletion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "completion_id")
    private Long completionId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false, unique = true)
    private MultiplayerRoom room;

    @Column(name = "station_id", nullable = false)
    private Long stationId;

    @Column(name = "total_phases", nullable = false)
    private Integer totalPhases;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @CreatedDate
    @Column(name = "completed_at", updatable = false)
    private LocalDateTime completedAt;

    @OneToMany(mappedBy = "completion", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CompletionParticipant> participants = new ArrayList<>();
}

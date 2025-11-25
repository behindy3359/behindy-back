package com.example.backend.entity.multiplayer;

import com.example.backend.entity.Station;
import com.example.backend.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
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
@Table(name = "multiplayer_room",
    indexes = {
        @Index(name = "idx_room_station", columnList = "station_id"),
        @Index(name = "idx_room_status", columnList = "status"),
        @Index(name = "idx_room_owner", columnList = "owner_user_id")
    }
)
public class MultiplayerRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id")
    private Long roomId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    @Column(name = "room_name", nullable = false, length = 100)
    private String roomName;

    @Column(name = "max_players", nullable = false)
    @Builder.Default
    private Integer maxPlayers = 3;

    @Column(name = "current_phase", nullable = false)
    @Builder.Default
    private Integer currentPhase = 0;

    @Column(name = "is_llm_processing", nullable = false)
    @Builder.Default
    private Boolean isLlmProcessing = false;

    @Column(name = "story_outline", columnDefinition = "TEXT")
    private String storyOutline;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private RoomStatus status = RoomStatus.WAITING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private User owner;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RoomParticipant> participants = new ArrayList<>();

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ChatMessage> messages = new ArrayList<>();

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MultiplayerStoryState> storyStates = new ArrayList<>();

    public int getCurrentPlayerCount() {
        return (int) participants.stream()
                .filter(RoomParticipant::isActive)
                .count();
    }

    public boolean isFull() {
        return getCurrentPlayerCount() >= maxPlayers;
    }

    public boolean canStart() {
        return status == RoomStatus.WAITING && getCurrentPlayerCount() > 0;
    }

    public void start() {
        this.status = RoomStatus.PLAYING;
        this.currentPhase = 1;
    }

    public void finish() {
        this.status = RoomStatus.FINISHED;
    }

    public void nextPhase() {
        this.currentPhase++;
    }
}

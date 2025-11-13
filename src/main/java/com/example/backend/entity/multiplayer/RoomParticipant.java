package com.example.backend.entity.multiplayer;

import com.example.backend.entity.Character;
import com.example.backend.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
@Table(name = "room_participant",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_room_user", columnNames = {"room_id", "user_id"})
    },
    indexes = {
        @Index(name = "idx_participant_room", columnList = "room_id"),
        @Index(name = "idx_participant_user", columnList = "user_id")
    }
)
public class RoomParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "participant_id")
    private Long participantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private MultiplayerRoom room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_id", nullable = false)
    private Character character;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "hp", nullable = false)
    private Integer hp;

    @Column(name = "sanity", nullable = false)
    private Integer sanity;

    @CreatedDate
    @Column(name = "joined_at", updatable = false)
    private LocalDateTime joinedAt;

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    public void leave() {
        this.isActive = false;
        this.leftAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return this.isActive;
    }

    public boolean isAlive() {
        return hp > 0;
    }

    public void takeDamage(int damage) {
        this.hp = Math.max(0, this.hp - damage);
    }

    public void heal(int amount) {
        this.hp = Math.min(character.getCharHealth(), this.hp + amount);
    }

    public void loseSanity(int amount) {
        this.sanity = Math.max(0, this.sanity - amount);
    }

    public void restoreSanity(int amount) {
        this.sanity = Math.min(character.getCharSanity(), this.sanity + amount);
    }
}

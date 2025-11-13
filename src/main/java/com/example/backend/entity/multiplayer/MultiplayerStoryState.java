package com.example.backend.entity.multiplayer;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
@Table(name = "multiplayer_story_state",
    indexes = {
        @Index(name = "idx_story_room_phase", columnList = "room_id, phase DESC")
    }
)
public class MultiplayerStoryState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "state_id")
    private Long stateId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private MultiplayerRoom room;

    @Column(name = "phase", nullable = false)
    private Integer phase;

    @Column(name = "llm_response", nullable = false, columnDefinition = "TEXT")
    private String llmResponse;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "context", columnDefinition = "jsonb")
    @Type(JsonType.class)
    private Map<String, Object> context;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

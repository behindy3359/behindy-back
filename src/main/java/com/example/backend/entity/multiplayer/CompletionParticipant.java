package com.example.backend.entity.multiplayer;

import com.example.backend.entity.Character;
import com.example.backend.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "completion_participant",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_completion_user", columnNames = {"completion_id", "user_id"})
    }
)
public class CompletionParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "participant_id")
    private Long participantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "completion_id", nullable = false)
    private StoryCompletion completion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_id", nullable = false)
    private Character character;

    @Column(name = "final_hp", nullable = false)
    private Integer finalHp;

    @Column(name = "final_sanity", nullable = false)
    private Integer finalSanity;

    @Column(name = "survived", nullable = false)
    @Builder.Default
    private Boolean survived = true;
}

package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter@Setter@Builder @AllArgsConstructor@NoArgsConstructor
@Table(name="STO",
    indexes = {
        @Index(name = "idx_story_station_id", columnList = "sta_id"),
        @Index(name = "idx_story_theme", columnList = "sto_theme"),
        @Index(name = "idx_story_length", columnList = "sto_length")
    }
)
public class Story {

    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name="sto_id")
    private Long stoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sta_id")
    private Station station;

    @Column(name="sto_title")
    private String stoTitle;

    @Column(name="sto_length")
    private int stoLength;

    @Column(name="sto_description", columnDefinition = "TEXT")
    private String stoDescription;

    @Column(name="sto_theme")
    private String stoTheme;

    @Column(name="sto_keywords", columnDefinition = "TEXT")
    private String stoKeywords;
}

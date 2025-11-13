package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter@Setter@NoArgsConstructor@AllArgsConstructor@Builder
@Table(name = "OPTIONS")
public class Options {
    @Id@GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name="opt_id")
    private long optId;

    @Column(name="page_id")
    private long pageId;

    @Column(name="opt_Contents")
    private String optContents;

    @Column(name="opt_effect")
    private String optEffect;

    @Column(name = "opt_amount")
    private int optAmount;

    @Column(name = "next_page_id")
    private Long nextPageId;

    @Column(name = "condition_type")
    private String conditionType;

    @Column(name = "condition_value")
    private Integer conditionValue;
    
    @OneToMany(mappedBy = "options", orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<LogO> logOS = new ArrayList<>();
}

package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter@Setter@Builder @NoArgsConstructor@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "LOGE")
public class LogE {

    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "loge_id")
    private Long logeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "char_id")
    private Character character;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sto_id")
    private Story story;

    @Column(name = "loge_result")
    private String logeResult;

    @Column(name = "loge_ending")
    private int logeEnding;

    @CreatedDate
    @Column(name="created_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "loge", orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<OpsLogB> opsLogBs = new ArrayList<>();
}

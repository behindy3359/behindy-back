package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter@Setter@Builder@NoArgsConstructor@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name="OPS_LOGB")
public class OpsLogB {
    @Id@GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "logb_id")
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loge_id", nullable = true)
    private LogE loge;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "char_id", nullable = true)
    private Character character;

    @Column(name = "logb_page")
    private long logbPage;

    @Column(name = "lob_opt")
    private long logbOpt;

    @Column(name = "logb_dur")
    private long logbDur;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

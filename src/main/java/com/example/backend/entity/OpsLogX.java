package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter@Setter@NoArgsConstructor@AllArgsConstructor@Builder
@EntityListeners(AuditingEntityListener.class)
@Table(name="OPS_LOGX")
public class OpsLogX {

    @Id@GeneratedValue(strategy = GenerationType.AUTO)
    private long logxId;

    @Column(name="logx_service")
    private String logxService;

    @Column(name="logx_message", columnDefinition="TEXT")
    private String logxMessage;

    @Column(name="logx_stktrace", columnDefinition = "TEXT")
    private String logxStktrace;

    @CreatedDate
    @Column(name="created_at", updatable = false)
    private LocalDateTime createdAt;
}

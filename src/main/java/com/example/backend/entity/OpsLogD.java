package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter@Setter@NoArgsConstructor@AllArgsConstructor@Builder
@Table(name="OPS_LOGD")
public class OpsLogD {
    @Id@GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "logd_id")
    private long logdId;

    @Column(name = "logd_date", updatable = false)
    private LocalDateTime logdDate;

    @Column(name = "logd_Total")
    private long logdTotal;

    @Column(name="logd_unique")
    private long logdUnique;

    @Column(name="logd_login")
    private long logdLogin;

    @Column(name="logd_counts")
    private long logdCounts;

    @Column(name="logd_success")
    private long logdSuccess;

    @Column(name="logd_fail")
    private long logdFail;
}

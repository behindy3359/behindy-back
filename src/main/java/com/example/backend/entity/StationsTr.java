package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter@Setter @Builder@NoArgsConstructor@AllArgsConstructor
@Table(name="TRA")
public class StationsTr {

    @Id@GeneratedValue(strategy = GenerationType.AUTO )
    @Column(name="tr_id")
    private long trId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sta_id")
    private Station station;

    @Column(name="tr_day")
    private String trDay;

    @Column(name="tr_time")
    private String trTime;

    @Column(name="tr_traffic")
    private String trTraffic;
}

package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Table(name="STA",
    indexes = {
        @Index(name = "idx_station_api_id", columnList = "api_station_id", unique = true),
        @Index(name = "idx_station_line", columnList = "sta_line"),
        @Index(name = "idx_station_name", columnList = "sta_name"),
        @Index(name = "idx_station_line_name", columnList = "sta_line, sta_name")
    }
)
public class Station {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long staId;

    @Column(name = "api_station_id", unique = true)
    private String apiStationId;

    @Column(name = "sta_name", nullable = false)
    private String staName;

    @Column(name = "sta_line", nullable = false)
    private Integer staLine;

    @OneToMany(mappedBy = "station", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Story> stories = new ArrayList<>();
}
package com.example.backend.dto.station;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class StationArrivalDto {
    private String trainNo;
    private String stationName;
    private Integer arrivalTime;
    private String arrivalMessage;
    private String trainLine;
    private String direction;
    private String trainStatus;
    private LocalDateTime fetchTime;
}

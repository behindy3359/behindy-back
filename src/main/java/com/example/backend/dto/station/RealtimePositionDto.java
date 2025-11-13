package com.example.backend.dto.station;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RealtimePositionDto {
    private String trainNo;
    private String stationId;
    private String stationName;
    private String subwayLine;
    private String direction;
    private String destination;
    private Integer trainStatus;
    private LocalDateTime receptionTime;
}

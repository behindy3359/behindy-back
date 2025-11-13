package com.example.backend.dto.metro;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainPosition {

    private String trainId;
    private Integer lineNumber;
    private String stationId;
    private String stationName;
    private String frontendStationId;
    private String direction;

    private Double x;
    private Double y;

    private LocalDateTime lastUpdated;
    private String dataSource;

    @JsonProperty("isRealtime")
    private boolean realtime;

    public boolean isUpDirection() {
        return "up".equalsIgnoreCase(direction) || "상행".equals(direction);
    }

    public boolean isDownDirection() {
        return "down".equalsIgnoreCase(direction) || "하행".equals(direction);
    }

    public boolean isFresh() {
        if (lastUpdated == null) return false;
        return lastUpdated.isAfter(LocalDateTime.now().minusMinutes(5));
    }

    public boolean isMockData() {
        return "MOCK".equalsIgnoreCase(dataSource);
    }

    public String getDisplayDirection() {
        if (isUpDirection()) return "상행";
        if (isDownDirection()) return "하행";
        return direction != null ? direction : "미정";
    }

    public String getDisplayLineName() {
        return lineNumber != null ? lineNumber + "호선" : "미정";
    }

    public boolean hasValidCoordinates() {
        return x != null && y != null && x >= 0 && y >= 0;
    }

    @Override
    public String toString() {
        return String.format("Train[%s] %s %s역(%s) (%s) [%.2f,%.2f] %s",
                trainId, getDisplayLineName(), stationName, frontendStationId, getDisplayDirection(),
                x != null ? x : 0.0, y != null ? y : 0.0, dataSource);
    }
}
package com.example.backend.dto.metro;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetroPositionResponse {

    private List<TrainPosition> positions;
    private Integer totalTrains;
    private Map<String, Integer> lineStatistics;
    private LocalDateTime lastUpdated;
    private LocalDateTime nextUpdate;
    private String dataSource;

    @JsonProperty("isRealtime")
    private boolean realtime;

    private String systemStatus;

    public Integer getTrainCountForLine(String lineNumber) {
        return lineStatistics != null ? lineStatistics.getOrDefault(lineNumber, 0) : 0;
    }

    public List<TrainPosition> getTrainsForLine(String lineNumber) {
        if (positions == null) return List.of();

        return positions.stream()
                .filter(train -> lineNumber.equals(String.valueOf(train.getLineNumber())))
                .toList();
    }

    public long getUpTrainCount() {
        if (positions == null) return 0;
        return positions.stream().filter(TrainPosition::isUpDirection).count();
    }

    public long getDownTrainCount() {
        if (positions == null) return 0;
        return positions.stream().filter(TrainPosition::isDownDirection).count();
    }

    public boolean isFresh() {
        if (lastUpdated == null) return false;
        return lastUpdated.isAfter(LocalDateTime.now().minusMinutes(5));
    }

    public boolean isMockData() {
        return "MOCK".equalsIgnoreCase(dataSource);
    }

    public boolean isSystemHealthy() {
        return "HEALTHY".equalsIgnoreCase(systemStatus);
    }

    public int getActiveLineCount() {
        return lineStatistics != null ? lineStatistics.size() : 0;
    }

    public String getLineStatisticsSummary() {
        if (lineStatistics == null || lineStatistics.isEmpty()) {
            return "통계 없음";
        }

        StringBuilder summary = new StringBuilder();
        lineStatistics.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    if (summary.length() > 0) summary.append(", ");
                    summary.append(entry.getKey()).append("호선: ").append(entry.getValue()).append("대");
                });

        return summary.toString();
    }

    public String getSummary() {
        return String.format("Metro[%d trains, %d lines, %s, %s]",
                totalTrains != null ? totalTrains : 0,
                getActiveLineCount(),
                dataSource != null ? dataSource : "UNKNOWN",
                isFresh() ? "FRESH" : "STALE");
    }
}
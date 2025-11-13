package com.example.backend.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeriodStatisticsResponse {

    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Long totalVisitors;
    private Long uniqueVisitors;
    private Long totalPlays;
    private Long totalSuccess;
    private Long totalFail;
    private Double averageClearRate;
    private List<DailyStatisticsResponse> dailyStats;
}

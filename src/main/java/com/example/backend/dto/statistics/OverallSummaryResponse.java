package com.example.backend.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OverallSummaryResponse {

    private Long totalPlays;
    private Long totalSuccess;
    private Long totalFail;
    private Double averageDailyVisitors;
    private Double averageDailyUniqueVisitors;
    private Double averageClearRate;
    private Long totalErrorCount;
    private Long recentErrorCount;
}

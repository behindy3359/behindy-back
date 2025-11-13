package com.example.backend.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyStatisticsResponse {

    private LocalDateTime date;
    private Long totalVisitors;
    private Long uniqueVisitors;
    private Long loginCount;
    private Long playCount;
    private Long successCount;
    private Long failCount;
    private Double clearRate;
}

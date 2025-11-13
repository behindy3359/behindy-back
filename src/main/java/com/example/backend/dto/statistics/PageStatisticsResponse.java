package com.example.backend.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageStatisticsResponse {

    private Long pageId;
    private Long pageNumber;
    private Long visitCount;
    private Double averageDurationSeconds;
    private Long minDurationMs;
    private Long maxDurationMs;
}

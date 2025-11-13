package com.example.backend.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptionStatisticsResponse {

    private Long optionId;
    private Long pageId;
    private String optionText;
    private Long selectionCount;
    private Long uniquePlayerCount;
    private Double selectionRate;
}

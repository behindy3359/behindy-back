package com.example.backend.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PopularStoryResponse {

    private Long storyId;
    private String storyTitle;
    private String stationName;
    private Integer stationLine;
    private Long playCount;
    private Long successCount;
    private Long failCount;
    private Double clearRate;
    private Integer ranking;
}

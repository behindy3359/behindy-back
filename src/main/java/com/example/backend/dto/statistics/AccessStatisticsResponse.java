package com.example.backend.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccessStatisticsResponse {

    private String path;
    private String method;
    private String statusCode;
    private Long accessCount;
    private Double percentage;
}

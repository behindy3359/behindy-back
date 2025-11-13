package com.example.backend.dto.metro;

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
public class MetroPositionCache {
    private Integer lineNumber;              
    private List<TrainPosition> positions;  
    private LocalDateTime lastUpdated;
    private LocalDateTime nextUpdateTime;
    private Boolean isHealthy;
    private String dataSource;
}

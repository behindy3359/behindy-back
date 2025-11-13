package com.example.backend.service;

import com.example.backend.dto.metro.TrainPosition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetroDataScheduler {

    private final MetroApiService metroApiService;
    private final MetroCacheService metroCacheService;
    private final MetroStationFilter stationFilter;

    @Value("${seoul.metro.api.enabled:true}")
    private boolean apiEnabled;

    @Value("${seoul.metro.monitoring.daily-limit:950}")
    private int dailyLimit;

    private final AtomicBoolean isUpdating = new AtomicBoolean(false);
    private LocalDateTime lastSuccessfulUpdate = null;
    private LocalDateTime lastLimitWarningTime = null;
    private int consecutiveFailures = 0;
    private static final int MAX_CONSECUTIVE_FAILURES = 5;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("=== 지하철 실시간 위치 시스템 시작 ===");
        log.info("API 활성화: {}", apiEnabled);
        log.info("프론트엔드 역 필터링: {}개 역",
                stationFilter.getFrontendStationIds() != null ?
                        stationFilter.getFrontendStationIds().size() : 0);

        new Thread(() -> {
            try {
                Thread.sleep(5000);
                updateAllMetroPositions();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    @Scheduled(fixedRateString = "${seoul.metro.api.update-interval:360000}")
    public void scheduledUpdate() {
        updateAllMetroPositions();
    }

    private boolean isOperatingHours() {
        int currentHour = LocalDateTime.now().getHour();
        return currentHour >= 5 && currentHour < 24;
    }

    public void updateAllMetroPositions() {
        if (!apiEnabled) {
            return;
        }

        if (!isOperatingHours()) {
            if (metroCacheService != null) {
                metroCacheService.cacheHealthStatus("NIGHT_MODE", "심야시간 - 지하철 운행 중단");
            }
            return;
        }

        if (!isUpdating.compareAndSet(false, true)) {
            return;
        }

        try {
            if (!checkApiLimit()) {
                return;
            }

            if (metroApiService != null) {
                metroApiService.getAllLinesRealtime()
                        .subscribe(
                                this::handleSuccessfulUpdate,
                                this::handleFailedUpdate
                        );
            } else {
                log.error("MetroApiService is null");
            }

        } catch (Exception e) {
            log.error("Metro update failed: {}", e.getMessage());
            handleFailedUpdate(e);
        } finally {
            isUpdating.set(false);
        }
    }

    public void updateLineData(String lineNumber) {
        if (!apiEnabled || !checkApiLimit() || metroApiService == null) {
            return;
        }

        log.info("{}호선 위치 데이터 업데이트 시작", lineNumber);

        metroApiService.getRealtimePositions(lineNumber)
                .subscribe(
                        allTrains -> {
                            if (allTrains != null && stationFilter != null) {
                                try {
                                    List<TrainPosition> filteredTrains = stationFilter.filterLineStations(
                                            allTrains, Integer.parseInt(lineNumber));

                                    if (metroCacheService != null) {
                                        metroCacheService.cacheLinePositions(lineNumber, filteredTrains);
                                    }

                                    log.info("{}호선 업데이트 완료: {}대 → {}대",
                                            lineNumber, allTrains.size(), filteredTrains.size());
                                } catch (Exception e) {
                                    log.error("{}호선 데이터 처리 중 오류: {}", lineNumber, e.getMessage());
                                }
                            }
                        },
                        error -> {
                            log.error("{}호선 업데이트 실패: {}", lineNumber, error.getMessage());
                            handleLineUpdateFailure(lineNumber, error);
                        }
                );
    }

    private void handleSuccessfulUpdate(List<TrainPosition> allTrains) {
        if (allTrains == null) {
            log.warn("Update data is null");
            return;
        }

        try {
            List<TrainPosition> filteredTrains = null;
            if (stationFilter != null) {
                filteredTrains = stationFilter.filterFrontendStations(allTrains);
            } else {
                filteredTrains = allTrains;
                log.warn("StationFilter is null - proceeding without filtering");
            }

            if (metroCacheService != null && filteredTrains != null) {
                metroCacheService.cacheAllPositions(filteredTrains);
            } else {
                log.error("Cache failed - metroCacheService: {}, filteredTrains: {}",
                    metroCacheService != null, filteredTrains != null);
            }

            if (metroApiService != null && metroCacheService != null && filteredTrains != null) {
                List<String> enabledLines = metroApiService.getEnabledLines();
                if (enabledLines != null) {
                    for (String lineNum : enabledLines) {
                        List<TrainPosition> lineTrains = filteredTrains.stream()
                                .filter(train -> train != null &&
                                        lineNum.equals(String.valueOf(train.getLineNumber())))
                                .toList();
                        metroCacheService.cacheLinePositions(lineNum, lineTrains);
                    }
                }
            }

            lastSuccessfulUpdate = LocalDateTime.now();
            consecutiveFailures = 0;
            if (metroCacheService != null) {
                metroCacheService.setLastUpdateTime(lastSuccessfulUpdate);
            }

            String statsMessage = "정상 업데이트 완료";
            if (stationFilter != null && filteredTrains != null) {
                MetroStationFilter.FilteringStatistics stats =
                        stationFilter.generateFilteringStats(allTrains, filteredTrains);
                if (stats != null) {
                    statsMessage = String.format("정상 업데이트 완료. %s", stats.getSummary());
                }
            }

            if (metroCacheService != null) {
                metroCacheService.cacheHealthStatus("HEALTHY", statsMessage);
            }

            int filteredCount = filteredTrains != null ? filteredTrains.size() : 0;
            int dailyCalls = metroApiService != null ? metroApiService.getDailyCallCount() : 0;

            log.info("Metro update success: {} → {} trains, API calls: {}/{}",
                allTrains.size(), filteredCount, dailyCalls, dailyLimit);

        } catch (Exception e) {
            log.error("Post-update processing failed: {}", e.getMessage(), e);
        }
    }

    private void handleFailedUpdate(Throwable error) {
        consecutiveFailures++;
        String errorMsg = String.format("업데이트 실패 (%d/%d): %s",
                consecutiveFailures, MAX_CONSECUTIVE_FAILURES,
                error != null ? error.getMessage() : "Unknown error");

        log.error(errorMsg, error);

        if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
            handleCriticalFailure();
        } else {
            if (metroCacheService != null) {
                metroCacheService.cacheHealthStatus("WARNING", errorMsg);
            }
        }
    }

    private void handleLineUpdateFailure(String lineNumber, Throwable error) {
        log.error("{}호선 업데이트 실패: {}", lineNumber,
                error != null ? error.getMessage() : "Unknown error");

        if (metroCacheService != null) {
            MetroCacheService.PositionCacheData existingData =
                    metroCacheService.getLinePositions(lineNumber);
            if (existingData != null && metroCacheService.isCacheValid(existingData)) {
                log.info("{}호선 기존 캐시 데이터 유지", lineNumber);
            }
        }
    }

        private void handleCriticalFailure() {
        String criticalMsg = String.format("연속 %d회 업데이트 실패", MAX_CONSECUTIVE_FAILURES);
        log.error("=== 심각한 오류: {} ===", criticalMsg);
        if (metroCacheService != null) {
            metroCacheService.cacheHealthStatus("CRITICAL", criticalMsg);
        }
    }

    private boolean checkApiLimit() {
        if (metroApiService == null) {
            return false;
        }

        int currentCalls = metroApiService.getDailyCallCount();

        if (currentCalls >= dailyLimit) {
            LocalDateTime now = LocalDateTime.now();
            if (lastLimitWarningTime == null || lastLimitWarningTime.isBefore(now.minusHours(1))) {
                log.warn("일일 API 호출 한도 도달: {}/{}", currentCalls, dailyLimit);
                lastLimitWarningTime = now;
            }
            if (metroCacheService != null) {
                metroCacheService.cacheHealthStatus("LIMITED", "일일 API 호출 한도 도달");
            }
            return false;
        }

        if (currentCalls >= dailyLimit * 0.9) {
            log.warn("일일 API 호출 한도 임박: {}/{}", currentCalls, dailyLimit);
        }

        return true;
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void resetDailyApiCount() {
        if (metroApiService != null) {
            metroApiService.resetDailyCallCount();
        }
        consecutiveFailures = 0;
        log.info("=== 일일 API 카운트 초기화 ===");
        if (metroCacheService != null) {
            metroCacheService.cacheHealthStatus("RESET", "일일 시스템 상태 초기화 완료");
        }
    }

    @Scheduled(cron = "0 0 * * * *")
    public void hourlyHealthCheck() {
        try {
            if (!isOperatingHours()) {
                if (metroCacheService != null) {
                    metroCacheService.cacheHealthStatus("NIGHT_MODE",
                        "심야시간 - 지하철 운행 중단 (정상)");
                }
                return;
            }

            MetroCacheService.CacheStatistics stats = metroCacheService != null ?
                    metroCacheService.getCacheStatistics() : null;
            int currentCalls = metroApiService != null ? metroApiService.getDailyCallCount() : 0;
            double usagePercentage = (double) currentCalls / dailyLimit * 100;

            LocalDateTime lastUpdate = metroCacheService != null ?
                    metroCacheService.getLastUpdateTime() : null;
            boolean isDataFresh = lastUpdate != null &&
                    lastUpdate.isAfter(LocalDateTime.now().minusMinutes(10));

            int frontendStationCount = stationFilter != null &&
                    stationFilter.getFrontendStationIds() != null ?
                    stationFilter.getFrontendStationIds().size() : 0;

            String healthStatus;
            String healthDetails;

            if (!isDataFresh) {
                healthStatus = "WARNING";
                healthDetails = String.format("데이터가 오래됨. 마지막 업데이트: %s", lastUpdate);
            } else if (usagePercentage > 95) {
                healthStatus = "LIMITED";
                healthDetails = String.format("API 사용량 위험: %.1f%% (%d/%d)",
                        usagePercentage, currentCalls, dailyLimit);
            } else if (consecutiveFailures > 0) {
                healthStatus = "WARNING";
                healthDetails = String.format("연속 실패 %d회", consecutiveFailures);
            } else {
                healthStatus = "HEALTHY";
                int activeCaches = stats != null ? stats.getActiveLinesCaches() : 0;
                healthDetails = String.format("정상 운영중. API: %.1f%%, 활성캐시: %d개, 필터링: %d개역",
                        usagePercentage, activeCaches, frontendStationCount);
            }

            if (metroCacheService != null) {
                metroCacheService.cacheHealthStatus(healthStatus, healthDetails);
            }

            if (!"HEALTHY".equals(healthStatus)) {
                log.warn("System health: {} - {}", healthStatus, healthDetails);
            }

        } catch (Exception e) {
            log.error("Health check failed: {}", e.getMessage(), e);
            if (metroCacheService != null) {
                metroCacheService.cacheHealthStatus("ERROR", "상태 점검 실패: " + e.getMessage());
            }
        }
    }

    public SystemStatus getSystemStatus() {
        MetroCacheService.HealthStatus health = metroCacheService != null ?
                metroCacheService.getHealthStatus() : null;
        MetroCacheService.CacheStatistics stats = metroCacheService != null ?
                metroCacheService.getCacheStatistics() : null;

        Map<Integer, Integer> frontendStationsByLine = new HashMap<>();
        if (stationFilter != null) {
            Map<Integer, Integer> filterStats = stationFilter.getFrontendStationCountByLine();
            if (filterStats != null) {
                frontendStationsByLine.putAll(filterStats);
            }
        }

        return SystemStatus.builder()
                .healthStatus(health != null ? health.getStatus() : "UNKNOWN")
                .healthDetails(health != null ? health.getDetails() : "상태 정보 없음")
                .lastHealthCheck(health != null ? health.getTimestamp() : LocalDateTime.now())
                .lastSuccessfulUpdate(lastSuccessfulUpdate)
                .lastUpdateTime(metroCacheService != null ?
                        metroCacheService.getLastUpdateTime() : null)
                .consecutiveFailures(consecutiveFailures)
                .dailyApiCalls(metroApiService != null ? metroApiService.getDailyCallCount() : 0)
                .dailyApiLimit(dailyLimit)
                .apiUsagePercentage(metroApiService != null ?
                        (double) metroApiService.getDailyCallCount() / dailyLimit * 100 : 0.0)
                .activeCaches(stats != null ? stats.getActiveLinesCaches() : 0)
                .totalTrains(stats != null ? stats.getTotalTrains() : 0)
                .hasAllPositionsCache(stats != null ? stats.isHasAllPositionsCache() : false)
                .isUpdating(isUpdating.get())
                .apiEnabled(apiEnabled)
                .filteringEnabled(stationFilter != null)
                .frontendStationCount(stationFilter != null &&
                        stationFilter.getFrontendStationIds() != null ?
                        stationFilter.getFrontendStationIds().size() : 0)
                .frontendStationsByLine(frontendStationsByLine)
                .build();
    }

    @lombok.Data
    @lombok.Builder
    public static class SystemStatus {
        private String healthStatus;
        private String healthDetails;
        private LocalDateTime lastHealthCheck;
        private LocalDateTime lastSuccessfulUpdate;
        private LocalDateTime lastUpdateTime;
        private int consecutiveFailures;
        private int dailyApiCalls;
        private int dailyApiLimit;
        private double apiUsagePercentage;
        private int activeCaches;
        private int totalTrains;
        private boolean hasAllPositionsCache;
        private boolean isUpdating;
        private boolean apiEnabled;
        private boolean filteringEnabled;
        private int frontendStationCount;
        private Map<Integer, Integer> frontendStationsByLine;
    }
}
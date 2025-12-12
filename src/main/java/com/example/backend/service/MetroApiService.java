package com.example.backend.service;

import com.example.backend.dto.metro.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MetroApiService {

    @Value("${seoul.metro.api.key}")
    private String apiKey;

    @Value("${seoul.metro.api.base-url}")
    private String baseUrl;

    @Value("${seoul.metro.api.enabled:true}")
    private boolean apiEnabled;

    @Value("${seoul.metro.api.timeout:10000}")
    private int timeoutMs;

    @Value("${seoul.metro.api.retry-count:3}")
    private int retryCount;

    @Value("${seoul.metro.api.enabled-lines:1,2,3,4}")
    private String enabledLinesConfig;

    private final WebClient webClient;
    private final AtomicInteger dailyCallCount = new AtomicInteger(0);
    private List<String> enabledLines;

    public MetroApiService(@Qualifier("metroWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    @PostConstruct
    public void init() {
        this.enabledLines = Arrays.stream(enabledLinesConfig.split(","))
                .map(String::trim)
                .filter(line -> line.matches("\\d+"))
                .collect(Collectors.toList());

        log.info("=== Metro API 서비스 초기화 (OpenAPI 연동 개선) ===");
        log.info("활성 노선: {}", enabledLines);
        log.info("API 키 상태: {}", isValidApiKey() ? "정상" : "테스트키/없음");
        log.info("API URL: {}", baseUrl);
    }

    public Mono<List<TrainPosition>> getRealtimePositions(String lineNumber) {
        if (!apiEnabled || !isValidApiKey()) {
            return createRealisticMockData(lineNumber);
        }

        return callSeoulMetroAPI(lineNumber)
                .doOnSuccess(positions -> incrementCallCount())
                .onErrorResume(error -> createRealisticMockData(lineNumber));
    }

    private Mono<List<TrainPosition>> callSeoulMetroAPI(String lineNumber) {
        String url = buildOpenApiUrl(lineNumber);

        return webClient.get()
                .uri(url)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                    Mono.error(new RuntimeException("OpenAPI HTTP 에러: " + response.statusCode())))
                .bodyToMono(RealtimePositionResponse.class)
                .timeout(Duration.ofMillis(timeoutMs))
                .retryWhen(Retry.fixedDelay(retryCount, Duration.ofSeconds(2)))
                .map(response -> processOpenApiResponse(response, lineNumber))
                .onErrorMap(Exception.class, error ->
                        new RuntimeException("OpenAPI 호출 완전 실패: " + error.getMessage(), error));
    }

    private List<TrainPosition> processOpenApiResponse(RealtimePositionResponse response, String lineNumber) {
        if (response.isAnyError()) {
            String errorMsg = response.getUnifiedErrorMessage();
            throw new RuntimeException("API_ERROR: " + errorMsg);
        }

        if (response.isEmpty()) {
            throw new RuntimeException("API_EMPTY: 운행 데이터 없음");
        }

        List<RealtimePositionInfo> apiData = response.getRealtimePositionList();
        return apiData.stream()
                .map(this::convertApiDataToTrainPosition)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private TrainPosition convertApiDataToTrainPosition(RealtimePositionInfo apiData) {
        try {
            return TrainPosition.builder()
                    .trainId(apiData.getTrainNo())
                    .lineNumber(Integer.valueOf(extractLineNumber(apiData.getSubwayId())))
                    .stationId(apiData.getStatnId())
                    .stationName(cleanStationName(apiData.getStatnNm()))
                    .frontendStationId(cleanStationName(apiData.getStatnNm()))
                    .direction(convertApiDirection(apiData.getUpdnLine()))
                    .lastUpdated(LocalDateTime.now())
                    .dataSource("SEOUL_OPENAPI")
                    .realtime(true)
                    .build();
        } catch (Exception e) {
            log.error("OpenAPI 데이터 변환 실패: {}", e.getMessage());
            return null;
        }
    }

        public Mono<List<TrainPosition>> getAllLinesRealtime() {
        log.info("🚇 배치 시작: {}개 노선 조회 [{}]", enabledLines.size(), String.join(", ", enabledLines));
        long startTime = System.currentTimeMillis();

        List<Mono<List<TrainPosition>>> requests = enabledLines.stream()
                .map(this::getRealtimePositions)
                .collect(Collectors.toList());

        return Mono.zip(requests, results -> {
            List<TrainPosition> allTrains = Arrays.stream(results)
                    .flatMap(result -> ((List<TrainPosition>) result).stream())
                    .collect(Collectors.toList());

            long realCount = allTrains.stream().filter(t -> "SEOUL_OPENAPI".equals(t.getDataSource())).count();
            long mockCount = allTrains.stream().filter(t -> "MOCK_REALISTIC".equals(t.getDataSource())).count();
            long duration = System.currentTimeMillis() - startTime;

            log.info("🚇 배치 완료: 총 {}대 열차 (실제: {}대, Mock: {}대) | 소요시간: {}ms | API 호출수: {}",
                    allTrains.size(), realCount, mockCount, duration, dailyCallCount.get());

            return allTrains;
        });
    }

    private Mono<List<TrainPosition>> createRealisticMockData(String lineNumber) {
        List<String> stations = getStationsForLine(lineNumber);
        int trainCount = getRealisticTrainCountForTime(lineNumber);
        List<TrainPosition> mockData = new ArrayList<>();
        Random random = new Random();

        for (int i = 0; i < trainCount; i++) {
            String stationName = stations.get(random.nextInt(stations.size()));

            TrainPosition position = TrainPosition.builder()
                    .trainId(generateRealisticTrainId(lineNumber, i))
                    .lineNumber(Integer.parseInt(lineNumber))
                    .stationId("MOCK_" + lineNumber + String.format("%03d", i))
                    .stationName(stationName)
                    .frontendStationId(stationName)
                    .direction(random.nextBoolean() ? "up" : "down")
                    .lastUpdated(LocalDateTime.now().minusSeconds(random.nextInt(300)))
                    .dataSource("MOCK_REALISTIC")
                    .realtime(false)
                    .build();

            mockData.add(position);
        }

        return Mono.just(mockData);
    }

    private String buildOpenApiUrl(String lineNumber) {
        return String.format("%s/%s/json/realtimePosition/0/100/%s호선",
                baseUrl, apiKey, lineNumber);
    }

    private boolean isValidApiKey() {
        return apiKey != null &&
                !apiKey.trim().isEmpty() &&
                !apiKey.equals("test_key") &&
                !apiKey.equals("TEMP_KEY") &&
                apiKey.length() > 10;
    }

    private List<String> getStationsForLine(String lineNumber) {
        Map<String, List<String>> stationsByLine = Map.of(
                "1", Arrays.asList("서울역", "종각", "시청", "용산", "영등포", "구로", "온수"),
                "2", Arrays.asList("을지로입구", "건대입구", "잠실", "강남", "사당", "홍대입구", "신촌"),
                "3", Arrays.asList("구파발", "불광", "독립문", "종로3가", "압구정", "교대", "양재"),
                "4", Arrays.asList("당고개", "창동", "혜화", "명동", "서울역", "사당")
        );
        return stationsByLine.getOrDefault(lineNumber, Arrays.asList("역1", "역2", "역3"));
    }

    private int getRealisticTrainCountForTime(String lineNumber) {
        Map<String, Integer> baseCounts = Map.of("1", 8, "2", 12, "3", 7, "4", 6);
        int baseCount = baseCounts.getOrDefault(lineNumber, 5);

        int hour = LocalDateTime.now().getHour();
        if (hour >= 7 && hour <= 9 || hour >= 18 && hour <= 20) {
            return baseCount + 3;
        } else if (hour >= 0 && hour <= 5) {
            return Math.max(2, baseCount - 3);
        }
        return baseCount;
    }

    private String generateRealisticTrainId(String lineNumber, int index) {
        return String.format("%s%04d", lineNumber, 1000 + index);
    }

    private String cleanStationName(String stationName) {
        if (stationName == null) return "미정";
        return stationName.replaceAll("\\([^)]*\\)", "").trim();
    }

    private String convertApiDirection(String updnLine) {
        if ("0".equals(updnLine)) return "up";
        if ("1".equals(updnLine)) return "down";
        return updnLine != null ? updnLine : "unknown";
    }

    private String extractLineNumber(String subwayId) {
        if (subwayId == null || subwayId.length() < 4) {
            return "1";
        }
        try {
            return subwayId.substring(3, 4);
        } catch (Exception e) {
            return "1";
        }
    }

    private void incrementCallCount() {
        int count = dailyCallCount.incrementAndGet();
        if (count % 10 == 0) {
            log.info("일일 OpenAPI 호출 수: {}", count);
        }
    }

    public List<String> getEnabledLines() {
        return new ArrayList<>(enabledLines);
    }

    public boolean isLineEnabled(String lineNumber) {
        return enabledLines.contains(lineNumber);
    }

    public int getDailyCallCount() {
        return dailyCallCount.get();
    }

    public void resetDailyCallCount() {
        dailyCallCount.set(0);
        log.info("일일 API 호출 카운트 초기화");
    }

    public boolean isApiHealthy() {
        return apiEnabled && isValidApiKey();
    }

    public Map<String, Object> getSystemStatus() {
        return Map.of(
                "apiEnabled", apiEnabled,
                "validApiKey", isValidApiKey(),
                "dailyCalls", dailyCallCount.get(),
                "enabledLines", enabledLines,
                "baseUrl", baseUrl,
                "timeout", timeoutMs
        );
    }
}
package com.example.backend.service;

import com.example.backend.dto.metro.TrainPosition;
import com.example.backend.dto.metro.MetroPositionResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetroPositionService {

    @Value("${seoul.metro.api.enabled:true}")
    private boolean apiEnabled;

    @Value("${seoul.metro.api.enabled-lines:1,2,3,4}")
    private String enabledLinesConfig;

    private final MetroStationFilter stationFilter;

    private List<Integer> enabledLines;

    private static final Map<Integer, Integer> REALISTIC_TRAIN_COUNTS = Map.of(
            1, 8,
            2, 12,
            3, 7,
            4, 6
    );

    private static final Map<Integer, List<StationInfo>> LINE_STATIONS = Map.of(
            1, Arrays.asList(
                    new StationInfo("도봉산", "1001000117"),
                    new StationInfo("창동", "1001000116"),
                    new StationInfo("종로5가", "1001000129"),
                    new StationInfo("종각", "1001000131"),
                    new StationInfo("시청", "1001000132"),
                    new StationInfo("서울역", "1001000133"),
                    new StationInfo("용산", "1001000135"),
                    new StationInfo("영등포", "1001000139"),
                    new StationInfo("구로", "1001000141"),
                    new StationInfo("온수", "1001000145")
            ),
            2, Arrays.asList(
                    new StationInfo("을지로입구", "1002000202"),
                    new StationInfo("동대문역사문화공원", "1002000205"),
                    new StationInfo("건대입구", "1002000212"),
                    new StationInfo("잠실", "1002000216"),
                    new StationInfo("삼성", "1002000219"),
                    new StationInfo("강남", "1002000222"),
                    new StationInfo("사당", "1002000226"),
                    new StationInfo("신림", "1002000230"),
                    new StationInfo("구로디지털단지", "1002000232"),
                    new StationInfo("홍대입구", "1002000239"),
                    new StationInfo("신촌", "1002000240")
            ),
            3, Arrays.asList(
                    new StationInfo("구파발", "1003000301"),
                    new StationInfo("불광", "1003000303"),
                    new StationInfo("독립문", "1003000307"),
                    new StationInfo("종로3가", "1003000310"),
                    new StationInfo("충무로", "1003000328"),
                    new StationInfo("압구정", "1003000323"),
                    new StationInfo("교대", "1003000319"),
                    new StationInfo("양재", "1003000344"),
                    new StationInfo("수서", "1003000351"),
                    new StationInfo("오금", "1003000352")
            ),
            4, Arrays.asList(
                    new StationInfo("당고개", "1004000401"),
                    new StationInfo("상계", "1004000402"),
                    new StationInfo("창동", "1004000412"),
                    new StationInfo("한성대입구", "1004000419"),
                    new StationInfo("동대문", "1004000421"),
                    new StationInfo("충무로", "1004000423"),
                    new StationInfo("명동", "1004000424"),
                    new StationInfo("서울역", "1004000426"),
                    new StationInfo("사당", "1004000433")
            )
    );

    @PostConstruct
    public void init() {
        if (enabledLinesConfig != null) {
            this.enabledLines = Arrays.stream(enabledLinesConfig.split(","))
                    .map(String::trim)
                    .filter(line -> line != null && line.matches("\\d+"))
                    .map(Integer::parseInt)
                    .sorted()
                    .collect(Collectors.toList());
        } else {
            this.enabledLines = Arrays.asList(1, 2, 3, 4);
        }

        int stationCount = 0;
        if (stationFilter != null && stationFilter.getFrontendStationIds() != null) {
            stationCount = stationFilter.getFrontendStationIds().size();
        }

        log.info("지하철 위치 서비스 초기화 완료: 활성 노선 {}", enabledLines);
        log.info("프론트엔드 역 필터링 활성화: {}개 역", stationCount);
    }

    public MetroPositionResponse getAllPositions() {
        try {
            List<TrainPosition> allPositions = new ArrayList<>();
            if (enabledLines != null) {
                for (Integer lineNumber : enabledLines) {
                    if (lineNumber != null) {
                        List<TrainPosition> linePositions = generateRealisticLinePositions(lineNumber);
                        if (linePositions != null) {
                            allPositions.addAll(linePositions);
                        }
                    }
                }
            }

            List<TrainPosition> filteredPositions = allPositions;
            if (stationFilter != null) {
                filteredPositions = stationFilter.filterFrontendStations(allPositions);
            } else {
                log.warn("StationFilter가 null - 필터링 없이 진행");
            }

            Map<String, Integer> lineStats = createLineStatistics(filteredPositions);

            MetroPositionResponse response = MetroPositionResponse.builder()
                    .positions(filteredPositions != null ? filteredPositions : new ArrayList<>())
                    .totalTrains(filteredPositions != null ? filteredPositions.size() : 0)
                    .lineStatistics(lineStats != null ? lineStats : new HashMap<>())
                    .lastUpdated(LocalDateTime.now())
                    .nextUpdate(LocalDateTime.now().plusMinutes(6))
                    .dataSource(apiEnabled ? "FILTERED_MOCK" : "MOCK")
                    .realtime(false)
                    .systemStatus("HEALTHY")
                    .build();

            log.debug("Mock 데이터 생성: {}대 → {}대 (필터링)", allPositions.size(),
                    filteredPositions != null ? filteredPositions.size() : 0);

            return response;

        } catch (Exception e) {
            log.error("전체 위치 정보 조회 실패: {}", e.getMessage(), e);
            return createErrorResponse("전체 위치 정보 조회 중 오류 발생");
        }
    }

    public MetroPositionResponse getLinePositions(Integer lineNumber) {
        try {
            if (lineNumber == null) {
                log.warn("노선 번호가 null입니다");
                return createErrorResponse("유효하지 않은 노선 번호");
            }

            if (enabledLines == null || !enabledLines.contains(lineNumber)) {
                log.warn("비활성 노선 요청: {}호선", lineNumber);
                return createEmptyResponse(lineNumber, "비활성 노선");
            }

            List<TrainPosition> allLinePositions = generateRealisticLinePositions(lineNumber);
            if (allLinePositions == null) {
                allLinePositions = new ArrayList<>();
            }

            List<TrainPosition> filteredPositions = allLinePositions;
            if (stationFilter != null) {
                filteredPositions = stationFilter.filterLineStations(allLinePositions, lineNumber);
            }
            if (filteredPositions == null) {
                filteredPositions = new ArrayList<>();
            }

            Map<String, Integer> lineStats = new HashMap<>();
            lineStats.put(lineNumber.toString(), filteredPositions.size());

            MetroPositionResponse response = MetroPositionResponse.builder()
                    .positions(filteredPositions)
                    .totalTrains(filteredPositions.size())
                    .lineStatistics(lineStats)
                    .lastUpdated(LocalDateTime.now())
                    .nextUpdate(LocalDateTime.now().plusMinutes(6))
                    .dataSource("FILTERED_MOCK")
                    .realtime(false)
                    .systemStatus("HEALTHY")
                    .build();

            log.debug("{}호선 Mock 생성: {}대 → {}대 (필터링)", lineNumber,
                    allLinePositions.size(), filteredPositions.size());

            return response;

        } catch (Exception e) {
            log.error("{}호선 위치 정보 조회 실패: {}", lineNumber, e.getMessage(), e);
            return createErrorResponse(lineNumber + "호선 위치 정보 조회 중 오류 발생");
        }
    }

    private List<TrainPosition> generateRealisticLinePositions(Integer lineNumber) {
        if (lineNumber == null) {
            return new ArrayList<>();
        }

        List<StationInfo> stations = LINE_STATIONS.get(lineNumber);
        if (stations == null || stations.isEmpty()) {
            log.warn("{}호선 역 정보 없음", lineNumber);
            return new ArrayList<>();
        }

        int trainCount = getRealisticTrainCount(lineNumber);
        trainCount = adjustTrainCountByTime(trainCount);

        List<TrainPosition> positions = new ArrayList<>();
        Random random = new Random();

        for (int i = 0; i < trainCount; i++) {
            StationInfo station = getDistributedStation(stations, i, trainCount);
            if (station == null) continue;

            String direction = getRealisticDirection(random, lineNumber);

            TrainPosition position = TrainPosition.builder()
                    .trainId(String.format("%d%04d", lineNumber, 1000 + i))
                    .lineNumber(lineNumber)
                    .stationId(station.getId())
                    .stationName(station.getName())
                    .frontendStationId(station.getName())
                    .direction(direction)
                    .lastUpdated(LocalDateTime.now().minusSeconds(random.nextInt(120)))
                    .dataSource("CLEAN_MOCK")
                    .realtime(false)
                    .build();

            positions.add(position);
        }

        return positions;
    }

    private int getRealisticTrainCount(Integer lineNumber) {
        if (lineNumber == null) return 3;
        return REALISTIC_TRAIN_COUNTS.getOrDefault(lineNumber, 5);
    }

    private int adjustTrainCountByTime(int baseCount) {
        LocalTime now = LocalTime.now();
        int hour = now.getHour();

        if (hour >= 7 && hour <= 9) {
            return baseCount + 2;
        }
        else if (hour >= 18 && hour <= 20) {
            return baseCount + 1;
        }
        else if (hour >= 0 && hour <= 5) {
            return Math.max(2, baseCount - 2);
        }

        return baseCount;
    }

    private StationInfo getDistributedStation(List<StationInfo> stations, int trainIndex, int totalTrains) {
        if (stations == null || stations.isEmpty()) {
            return null;
        }
        int interval = Math.max(1, stations.size() / totalTrains);
        int stationIndex = (trainIndex * interval) % stations.size();
        return stations.get(stationIndex);
    }

    private String getRealisticDirection(Random random, Integer lineNumber) {
        if (random == null) {
            return "up";
        }
        return random.nextDouble() < 0.6 ? "up" : "down";
    }

    private Map<String, Integer> createLineStatistics(List<TrainPosition> positions) {
        if (positions == null) {
            return new HashMap<>();
        }

        return positions.stream()
                .filter(pos -> pos != null && pos.getLineNumber() != null)
                .collect(Collectors.groupingBy(
                        pos -> pos.getLineNumber().toString(),
                        Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
                ));
    }

    private MetroPositionResponse createEmptyResponse(Integer lineNumber, String reason) {
        return MetroPositionResponse.builder()
                .positions(new ArrayList<>())
                .totalTrains(0)
                .lineStatistics(new HashMap<>())
                .lastUpdated(LocalDateTime.now())
                .dataSource("NONE")
                .realtime(false)
                .systemStatus("WARNING")
                .build();
    }

    private MetroPositionResponse createErrorResponse(String errorMessage) {
        log.error("오류 응답 생성: {}", errorMessage);

        return MetroPositionResponse.builder()
                .positions(new ArrayList<>())
                .totalTrains(0)
                .lineStatistics(new HashMap<>())
                .lastUpdated(LocalDateTime.now())
                .dataSource("ERROR")
                .realtime(false)
                .systemStatus("ERROR")
                .build();
    }

    public List<Integer> getEnabledLines() {
        return enabledLines != null ? new ArrayList<>(enabledLines) : new ArrayList<>();
    }

    public boolean isLineEnabled(Integer lineNumber) {
        return enabledLines != null && lineNumber != null && enabledLines.contains(lineNumber);
    }

    public Map<String, Object> getFilterInfo() {
        Map<String, Object> filterInfo = new HashMap<>();

        if (stationFilter != null) {
            Set<String> stationIds = stationFilter.getFrontendStationIds();
            Map<Integer, Integer> stationsByLine = stationFilter.getFrontendStationCountByLine();

            filterInfo.put("totalFrontendStations", stationIds != null ? stationIds.size() : 0);
            filterInfo.put("stationsByLine", stationsByLine != null ? stationsByLine : new HashMap<>());
        } else {
            filterInfo.put("totalFrontendStations", 0);
            filterInfo.put("stationsByLine", new HashMap<>());
        }

        filterInfo.put("enabledLines", enabledLines != null ? enabledLines : new ArrayList<>());
        filterInfo.put("filteringEnabled", stationFilter != null);

        return filterInfo;
    }

    @Getter
    private static class StationInfo {
        private final String name;
        private final String id;

        public StationInfo(String name, String id) {
            this.name = name != null ? name : "미정";
            this.id = id != null ? id : "unknown";
        }
    }
}
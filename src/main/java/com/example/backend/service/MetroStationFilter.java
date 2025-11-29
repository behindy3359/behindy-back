package com.example.backend.service;

import com.example.backend.dto.metro.TrainPosition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class MetroStationFilter {

    private static final Set<String> FRONTEND_STATION_IDS = Set.of(
            "1001000117",
            "1001000118",
            "1001000119",
            "1001000116",
            "1001000115",
            "1001000114",
            "1001000113",
            "1001000112",
            "1001000111",
            "1001000110",
            "1001000109",
            "1001000108",
            "1001000125",
            "1001000126",
            "1001000127",
            "1001000129",
            "1001000130",
            "1001000131",
            "1001000132",
            "1001000133",
            "1001000134",
            "1001000135",
            "1001000136",
            "1001000137",
            "1001000138",
            "1001000139",
            "1001000141",
            "1001000142",
            "1001000143",
            "1001000144",
            "1001000145",
            "1001000140",
            "1001000146",

            "1002000202",
            "1002000203",
            "1002000204",
            "1002000205",
            "1002000206",
            "1002000207",
            "1002000208",
            "1002000209",
            "1002000210",
            "1002000211",
            "1002000212",
            "1002000213",
            "1002000214",
            "1002000215",
            "1002000216",
            "1002000217",
            "1002000218",
            "1002000219",
            "1002000220",
            "1002000221",
            "1002000222",
            "1002000223",
            "1002000224",
            "1002000225",
            "1002000226",
            "1002000227",
            "1002000228",
            "1002000229",
            "1002000230",
            "1002000231",
            "1002000232",
            "1002000233",
            "1002000234",
            "1002000235",
            "1002000236",
            "1002000237",
            "1002000238",
            "1002000239",
            "1002000240",
            "1002000241",
            "1002000242",
            "1002000243",
            "1002000244",
            "1002000201",
            "1002000245",
            "1002000246",
            "1002000247",
            "1002000248",
            "1002000249",

            "1003000301",
            "1003000302",
            "1003000303",
            "1003000304",
            "1003000305",
            "1003000306",
            "1003000307",
            "1003000308",
            "1003000309",
            "1003000310",
            "1003000328",
            "1003000327",
            "1003000326",
            "1003000325",
            "1003000324",
            "1003000323",
            "1003000322",
            "1003000321",
            "1003000320",
            "1003000319",
            "1003000343",
            "1003000344",
            "1003000345",
            "1003000346",
            "1003000347",
            "1003000348",
            "1003000349",
            "1003000350",
            "1003000351",
            "1003000352",

            "1004000401",
            "1004000402",
            "1004000411",
            "1004000412",
            "1004000413",
            "1004000414",
            "1004000415",
            "1004000416",
            "1004000417",
            "1004000418",
            "1004000419",
            "1004000420",
            "1004000421",
            "1004000423",
            "1004000424",
            "1004000425",
            "1004000426",
            "1004000427",
            "1004000428",
            "1004000429",
            "1004000430",
            "1004000431",
            "1004000432",
            "1004000433",
            "1004000434",
            "1004000435",
            "1004000436",
            "1004000437",
            "1004000438",
            "1004000439",
            "1004000440",
            "1004000441",
            "1004000442",
            "1004000443",
            "1004000444",
            "1004000445",
            "1004000446",
            "1004000447",
            "1004000448",
            "1004000449",
            "1004000450",
            "1004000451",
            "1004000452",
            "1004000453",
            "1004000454",
            "1004000455",
            "1004000456"
    );

    private Map<Integer, Integer> frontendStationsByLine;

    @PostConstruct
    public void init() {
        frontendStationsByLine = Map.of(
                1, (int) FRONTEND_STATION_IDS.stream().filter(id -> id.startsWith("1001")).count(),
                2, (int) FRONTEND_STATION_IDS.stream().filter(id -> id.startsWith("1002")).count(),
                3, (int) FRONTEND_STATION_IDS.stream().filter(id -> id.startsWith("1003")).count(),
                4, (int) FRONTEND_STATION_IDS.stream().filter(id -> id.startsWith("1004")).count()
        );

        log.info("=== 지하철 역 필터 초기화 ===");
        log.info("총 프론트엔드 역: {}개", FRONTEND_STATION_IDS.size());
        log.info("노선별 역 수: {}", frontendStationsByLine);
    }

    public List<TrainPosition> filterFrontendStations(List<TrainPosition> allPositions) {
        if (allPositions == null || allPositions.isEmpty()) {
            return new ArrayList<>();
        }

        List<TrainPosition> filteredPositions = allPositions.stream()
                .filter(position -> {
                    if (position.getStationId() == null) {
                        return false;
                    }
                    return FRONTEND_STATION_IDS.contains(position.getStationId());
                })
                .collect(Collectors.toList());

        return filteredPositions;
    }

    public List<TrainPosition> filterLineStations(List<TrainPosition> allPositions, Integer lineNumber) {
        if (allPositions == null || allPositions.isEmpty()) {
            return new ArrayList<>();
        }

        String linePrefix = "100" + lineNumber;

        List<TrainPosition> filteredPositions = allPositions.stream()
                .filter(position -> {
                    if (position.getStationId() == null) return false;
                    return position.getStationId().startsWith(linePrefix) &&
                            FRONTEND_STATION_IDS.contains(position.getStationId());
                })
                .collect(Collectors.toList());

        return filteredPositions;
    }

    public boolean isFrontendStation(String stationId) {
        return stationId != null && FRONTEND_STATION_IDS.contains(stationId);
    }

    public Set<String> getFrontendStationIds() {
        return new HashSet<>(FRONTEND_STATION_IDS);
    }

    public Map<Integer, Integer> getFrontendStationCountByLine() {
        return new HashMap<>(frontendStationsByLine);
    }

    public FilteringStatistics generateFilteringStats(List<TrainPosition> original, List<TrainPosition> filtered) {
        Map<Integer, Long> originalByLine = original.stream()
                .collect(Collectors.groupingBy(TrainPosition::getLineNumber, Collectors.counting()));

        Map<Integer, Long> filteredByLine = filtered.stream()
                .collect(Collectors.groupingBy(TrainPosition::getLineNumber, Collectors.counting()));

        return FilteringStatistics.builder()
                .originalCount(original.size())
                .filteredCount(filtered.size())
                .reductionCount(original.size() - filtered.size())
                .reductionPercentage((double)(original.size() - filtered.size()) / original.size() * 100)
                .originalByLine(originalByLine)
                .filteredByLine(filteredByLine)
                .build();
    }

    private void logFilteringStatsByLine(List<TrainPosition> original, List<TrainPosition> filtered) {
        Map<Integer, Long> originalByLine = original.stream()
                .filter(p -> p.getLineNumber() != null)
                .collect(Collectors.groupingBy(TrainPosition::getLineNumber, Collectors.counting()));

        Map<Integer, Long> filteredByLine = filtered.stream()
                .filter(p -> p.getLineNumber() != null)
                .collect(Collectors.groupingBy(TrainPosition::getLineNumber, Collectors.counting()));

        log.info("   노선별 필터링 결과:");
        for (int line = 1; line <= 4; line++) {
            long original_count = originalByLine.getOrDefault(line, 0L);
            long filtered_count = filteredByLine.getOrDefault(line, 0L);

            if (original_count > 0) {
                log.info("   {}호선: {}대 → {}대 (-{}대)",
                        line, original_count, filtered_count, original_count - filtered_count);
            }
        }
    }

    public List<String> getExcludedStations(List<TrainPosition> allPositions) {
        return allPositions.stream()
                .filter(position -> position.getStationId() != null)
                .filter(position -> !FRONTEND_STATION_IDS.contains(position.getStationId()))
                .map(TrainPosition::getStationId)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    @lombok.Data
    @lombok.Builder
    public static class FilteringStatistics {
        private int originalCount;
        private int filteredCount;
        private int reductionCount;
        private double reductionPercentage;
        private Map<Integer, Long> originalByLine;
        private Map<Integer, Long> filteredByLine;

        public String getSummary() {
            return String.format("필터링: %d대 → %d대 (%.1f%% 감소)",
                    originalCount, filteredCount, reductionPercentage);
        }
    }
}
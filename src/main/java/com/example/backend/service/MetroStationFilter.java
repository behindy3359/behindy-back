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
            // === 1호선 ===
            "1001000117", // 도봉산
            "1001000118", // 도봉
            "1001000119", // 방학
            "1001000116", // 창동
            "1001000115", // 녹천
            "1001000114", // 월계
            "1001000113", // 광운대
            "1001000112", // 석계
            "1001000111", // 신이문
            "1001000110", // 외대앞
            "1001000109", // 회기
            "1001000108", // 청량리
            "1001000125", // 제기동
            "1001000126", // 신설동
            "1001000127", // 동묘앞
            "1001000129", // 종로5가
            "1001000130", // 종로3가
            "1001000131", // 종각
            "1001000132", // 시청
            "1001000133", // 서울역
            "1001000134", // 남영
            "1001000135", // 용산
            "1001000136", // 노량진
            "1001000137", // 대방
            "1001000138", // 신길
            "1001000139", // 영등포
            "1001000141", // 구로
            "1001000142", // 구일
            "1001000143", // 개봉
            "1001000144", // 오류동
            "1001000145", // 온수
            "1001000140", // 가산디지털단지
            "1001000146", // 독산

            // === 2호선 ===
            "1002000202", // 을지로입구
            "1002000203", // 을지로3가
            "1002000204", // 을지로4가
            "1002000205", // 동대문역사문화공원
            "1002000206", // 신당
            "1002000207", // 상왕십리
            "1002000208", // 왕십리
            "1002000209", // 한양대
            "1002000210", // 뚝섬
            "1002000211", // 성수
            "1002000212", // 건대입구
            "1002000213", // 구의
            "1002000214", // 강변
            "1002000215", // 잠실나루
            "1002000216", // 잠실
            "1002000217", // 잠실새내
            "1002000218", // 종합운동장
            "1002000219", // 삼성
            "1002000220", // 선릉
            "1002000221", // 역삼
            "1002000222", // 강남
            "1002000223", // 교대
            "1002000224", // 서초
            "1002000225", // 방배
            "1002000226", // 사당
            "1002000227", // 낙성대
            "1002000228", // 서울대입구
            "1002000229", // 봉천
            "1002000230", // 신림
            "1002000231", // 신대방
            "1002000232", // 구로디지털단지
            "1002000233", // 대림
            "1002000234", // 신도림
            "1002000235", // 문래
            "1002000236", // 영등포구청
            "1002000237", // 당산
            "1002000238", // 합정
            "1002000239", // 홍대입구
            "1002000240", // 신촌
            "1002000241", // 이대
            "1002000242", // 아현
            "1002000243", // 용답
            "1002000244", // 신답
            "1002000201", // 충정로
            "1002000245", // 용두
            "1002000246", // 도림천
            "1002000247", // 양천구청
            "1002000248", // 신정네거리
            "1002000249", // 까치산

            // === 3호선 ===
            "1003000301", // 구파발
            "1003000302", // 연신내
            "1003000303", // 불광
            "1003000304", // 녹번
            "1003000305", // 홍제
            "1003000306", // 무악재
            "1003000307", // 독립문
            "1003000308", // 경복궁
            "1003000309", // 안국
            "1003000310", // 종로3가
            "1003000328", // 충무로
            "1003000327", // 동대입구
            "1003000326", // 약수
            "1003000325", // 금고
            "1003000324", // 옥수
            "1003000323", // 압구정
            "1003000322", // 신사
            "1003000321", // 잠원
            "1003000320", // 고속터미널
            "1003000319", // 교대
            "1003000343", // 남부터미널
            "1003000344", // 양재
            "1003000345", // 매봉
            "1003000346", // 도곡
            "1003000347", // 대치
            "1003000348", // 학여울
            "1003000349", // 대청
            "1003000350", // 일원
            "1003000351", // 수서
            "1003000352", // 오금

            // === 4호선 ===
            "1004000401", // 당고개
            "1004000402", // 상계
            "1004000411", // 노원
            "1004000412", // 창동
            "1004000413", // 쌍문
            "1004000414", // 수유
            "1004000415", // 미아
            "1004000416", // 미아사거리
            "1004000417", // 길음
            "1004000418", // 성신여대입구
            "1004000419", // 한성대입구
            "1004000420", // 혜화
            "1004000421", // 동대문
            "1004000423", // 충무로
            "1004000424", // 명동
            "1004000425", // 회현
            "1004000426", // 서울역
            "1004000427", // 숙대입구
            "1004000428", // 삼각지
            "1004000429", // 신용산
            "1004000430", // 이촌
            "1004000431", // 동작
            "1004000432", // 총신대입구(이수)
            "1004000433", // 사당
            "1004000434", // 남태령
            "1004000435", // 선바위
            "1004000436", // 경마공원
            "1004000437", // 대공원
            "1004000438", // 과천
            "1004000439", // 정부과천청사
            "1004000440", // 인덕원
            "1004000441", // 평촌
            "1004000442", // 범계
            "1004000443", // 금정
            "1004000444", // 산본
            "1004000445", // 수리산
            "1004000446", // 대야미
            "1004000447", // 반월
            "1004000448", // 상록수
            "1004000449", // 한대앞
            "1004000450", // 중앙
            "1004000451", // 고잔
            "1004000452", // 초지
            "1004000453", // 안산
            "1004000454", // 신길온천
            "1004000455", // 정왕
            "1004000456"  // 오이도
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
package com.example.backend.service;

import com.example.backend.entity.OpsLogD;
import com.example.backend.repository.LogERepository;
import com.example.backend.repository.OpsLogARepository;
import com.example.backend.repository.OpsLogDRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyStatisticsService {

    private final OpsLogDRepository opsLogDRepository;
    private final OpsLogARepository opsLogARepository;
    private final LogERepository logERepository;

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void generateDailyStatistics() {
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1)
                .with(LocalTime.of(0, 0, 0));

        log.info("일일 통계 생성 시작: {}", yesterday.toLocalDate());

        try {
            if (opsLogDRepository.existsByDate(yesterday)) {
                log.warn("이미 생성된 통계가 있습니다: {}", yesterday.toLocalDate());
                return;
            }

            OpsLogD statistics = generateStatisticsForDate(yesterday);

            opsLogDRepository.save(statistics);

            log.info("일일 통계 생성 완료: date={}, total={}, unique={}, login={}, plays={}, success={}, fail={}",
                    yesterday.toLocalDate(),
                    statistics.getLogdTotal(),
                    statistics.getLogdUnique(),
                    statistics.getLogdLogin(),
                    statistics.getLogdCounts(),
                    statistics.getLogdSuccess(),
                    statistics.getLogdFail());

        } catch (Exception e) {
            log.error("일일 통계 생성 실패: date={}", yesterday.toLocalDate(), e);
        }
    }

    @Transactional
    public OpsLogD generateStatisticsForDate(LocalDateTime date) {
        LocalDateTime startOfDay = date.with(LocalTime.of(0, 0, 0));
        LocalDateTime endOfDay = date.with(LocalTime.of(23, 59, 59));

        Long totalVisitors = opsLogARepository.countTotalVisitorsByDate(startOfDay);
        if (totalVisitors == null) totalVisitors = 0L;

        Long uniqueVisitors = opsLogARepository.countUniqueVisitorsByDate(startOfDay);
        if (uniqueVisitors == null) uniqueVisitors = 0L;

        Long loginCount = opsLogARepository.countUniqueVisitorsByDate(startOfDay);
        if (loginCount == null) loginCount = 0L;

        Long playCount = logERepository.countTotalPlaysByCharacter(null);
        if (playCount == null) playCount = 0L;

        Long successCount = countSuccessByDate(startOfDay, endOfDay);

        Long failCount = countFailByDate(startOfDay, endOfDay);

        return OpsLogD.builder()
                .logdDate(startOfDay)
                .logdTotal(totalVisitors)
                .logdUnique(uniqueVisitors)
                .logdLogin(loginCount)
                .logdCounts(playCount)
                .logdSuccess(successCount)
                .logdFail(failCount)
                .build();
    }

    private Long countSuccessByDate(LocalDateTime startDate, LocalDateTime endDate) {
        try {
            return logERepository.findRecentCompletions(startDate).stream()
                    .filter(log -> log.getCreatedAt().isBefore(endDate))
                    .count();
        } catch (Exception e) {
            log.warn("성공 횟수 계산 실패", e);
            return 0L;
        }
    }

    private Long countFailByDate(LocalDateTime startDate, LocalDateTime endDate) {
        try {
            Long totalPlays = logERepository.findRecentCompletions(startDate).stream()
                    .filter(log -> log.getCreatedAt().isBefore(endDate))
                    .count();
            Long successCount = countSuccessByDate(startDate, endDate);
            return Math.max(0L, totalPlays - successCount);
        } catch (Exception e) {
            log.warn("실패 횟수 계산 실패", e);
            return 0L;
        }
    }

    @Transactional(readOnly = true)
    public java.util.List<OpsLogD> getRecentStatistics(int days) {
        return opsLogDRepository.findRecentDays(days);
    }

    @Transactional(readOnly = true)
    public java.util.List<OpsLogD> getStatisticsBetween(LocalDateTime startDate, LocalDateTime endDate) {
        return opsLogDRepository.findByDateBetween(startDate, endDate);
    }

    @Transactional(readOnly = true)
    public StatisticsSummary getOverallSummary() {
        Long totalPlays = opsLogDRepository.getTotalPlayCount();
        Long totalSuccess = opsLogDRepository.getTotalSuccessCount();
        Long totalFail = opsLogDRepository.getTotalFailCount();
        Double avgVisitors = opsLogDRepository.getAverageDailyVisitors();
        Double avgUnique = opsLogDRepository.getAverageDailyUniqueVisitors();
        Double avgClearRate = opsLogDRepository.getAverageClearRate();

        return StatisticsSummary.builder()
                .totalPlays(totalPlays != null ? totalPlays : 0L)
                .totalSuccess(totalSuccess != null ? totalSuccess : 0L)
                .totalFail(totalFail != null ? totalFail : 0L)
                .averageDailyVisitors(avgVisitors != null ? avgVisitors : 0.0)
                .averageDailyUniqueVisitors(avgUnique != null ? avgUnique : 0.0)
                .averageClearRate(avgClearRate != null ? avgClearRate * 100 : 0.0)
                .build();
    }

    @lombok.Data
    @lombok.Builder
    public static class StatisticsSummary {
        private Long totalPlays;
        private Long totalSuccess;
        private Long totalFail;
        private Double averageDailyVisitors;
        private Double averageDailyUniqueVisitors;
        private Double averageClearRate;
    }
}

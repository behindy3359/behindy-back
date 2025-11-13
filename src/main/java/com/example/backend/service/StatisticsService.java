package com.example.backend.service;

import com.example.backend.dto.statistics.*;
import com.example.backend.entity.*;
import com.example.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final OpsLogDRepository opsLogDRepository;
    private final OpsLogARepository opsLogARepository;
    private final OpsLogXRepository opsLogXRepository;
    private final OpsLogBRepository opsLogBRepository;
    private final LogERepository logERepository;
    private final LogORepository logORepository;
    private final StoryRepository storyRepository;
    private final DailyStatisticsService dailyStatisticsService;

    @Transactional(readOnly = true)
    public DailyStatisticsResponse getDailyStatistics(LocalDateTime date) {
        LocalDateTime targetDate = date.with(LocalTime.of(0, 0, 0));

        OpsLogD opsLogD = opsLogDRepository.findByDate(targetDate)
                .orElse(null);

        if (opsLogD == null) {
            return DailyStatisticsResponse.builder()
                    .date(targetDate)
                    .totalVisitors(0L)
                    .uniqueVisitors(0L)
                    .loginCount(0L)
                    .playCount(0L)
                    .successCount(0L)
                    .failCount(0L)
                    .clearRate(0.0)
                    .build();
        }

        double clearRate = opsLogD.getLogdCounts() > 0 ?
                (opsLogD.getLogdSuccess() * 100.0 / opsLogD.getLogdCounts()) : 0.0;

        return DailyStatisticsResponse.builder()
                .date(opsLogD.getLogdDate())
                .totalVisitors(opsLogD.getLogdTotal())
                .uniqueVisitors(opsLogD.getLogdUnique())
                .loginCount(opsLogD.getLogdLogin())
                .playCount(opsLogD.getLogdCounts())
                .successCount(opsLogD.getLogdSuccess())
                .failCount(opsLogD.getLogdFail())
                .clearRate(clearRate)
                .build();
    }

    @Transactional(readOnly = true)
    public PeriodStatisticsResponse getPeriodStatistics(LocalDateTime startDate, LocalDateTime endDate) {
        LocalDateTime start = startDate.with(LocalTime.of(0, 0, 0));
        LocalDateTime end = endDate.with(LocalTime.of(23, 59, 59));

        List<OpsLogD> dailyStats = opsLogDRepository.findByDateBetween(start, end);

        long totalVisitors = dailyStats.stream()
                .mapToLong(OpsLogD::getLogdTotal)
                .sum();

        long uniqueVisitors = dailyStats.stream()
                .mapToLong(OpsLogD::getLogdUnique)
                .sum();

        long totalPlays = dailyStats.stream()
                .mapToLong(OpsLogD::getLogdCounts)
                .sum();

        long totalSuccess = dailyStats.stream()
                .mapToLong(OpsLogD::getLogdSuccess)
                .sum();

        long totalFail = dailyStats.stream()
                .mapToLong(OpsLogD::getLogdFail)
                .sum();

        double averageClearRate = totalPlays > 0 ?
                (totalSuccess * 100.0 / totalPlays) : 0.0;

        List<DailyStatisticsResponse> dailyResponses = dailyStats.stream()
                .map(d -> {
                    double clearRate = d.getLogdCounts() > 0 ?
                            (d.getLogdSuccess() * 100.0 / d.getLogdCounts()) : 0.0;
                    return DailyStatisticsResponse.builder()
                            .date(d.getLogdDate())
                            .totalVisitors(d.getLogdTotal())
                            .uniqueVisitors(d.getLogdUnique())
                            .loginCount(d.getLogdLogin())
                            .playCount(d.getLogdCounts())
                            .successCount(d.getLogdSuccess())
                            .failCount(d.getLogdFail())
                            .clearRate(clearRate)
                            .build();
                })
                .collect(Collectors.toList());

        return PeriodStatisticsResponse.builder()
                .startDate(start)
                .endDate(end)
                .totalVisitors(totalVisitors)
                .uniqueVisitors(uniqueVisitors)
                .totalPlays(totalPlays)
                .totalSuccess(totalSuccess)
                .totalFail(totalFail)
                .averageClearRate(averageClearRate)
                .dailyStats(dailyResponses)
                .build();
    }

    @Transactional(readOnly = true)
    public List<DailyStatisticsResponse> getRecentStatistics(int days) {
        List<OpsLogD> recentStats = opsLogDRepository.findRecentDays(days);

        return recentStats.stream()
                .map(d -> {
                    double clearRate = d.getLogdCounts() > 0 ?
                            (d.getLogdSuccess() * 100.0 / d.getLogdCounts()) : 0.0;
                    return DailyStatisticsResponse.builder()
                            .date(d.getLogdDate())
                            .totalVisitors(d.getLogdTotal())
                            .uniqueVisitors(d.getLogdUnique())
                            .loginCount(d.getLogdLogin())
                            .playCount(d.getLogdCounts())
                            .successCount(d.getLogdSuccess())
                            .failCount(d.getLogdFail())
                            .clearRate(clearRate)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PopularStoryResponse> getPopularStories(int limit) {
        List<Story> allStories = storyRepository.findAll();

        List<PopularStoryResponse> storyStats = allStories.stream()
                .map(story -> {
                    Long playCount = logERepository.countTotalPlaysByStory(story.getStoId());
                    Long successCount = logERepository.countCompletionsByStory(story.getStoId());
                    Long failCount = playCount - successCount;
                    double clearRate = playCount > 0 ? (successCount * 100.0 / playCount) : 0.0;

                    return PopularStoryResponse.builder()
                            .storyId(story.getStoId())
                            .storyTitle(story.getStoTitle())
                            .stationName(story.getStation().getStaName())
                            .stationLine(story.getStation().getStaLine())
                            .playCount(playCount)
                            .successCount(successCount)
                            .failCount(failCount)
                            .clearRate(clearRate)
                            .build();
                })
                .sorted((a, b) -> Long.compare(b.getPlayCount(), a.getPlayCount()))
                .limit(limit)
                .collect(Collectors.toList());

        for (int i = 0; i < storyStats.size(); i++) {
            storyStats.get(i).setRanking(i + 1);
        }

        return storyStats;
    }

    @Transactional(readOnly = true)
    public List<OptionStatisticsResponse> getOptionStatistics(Long pageId) {
        List<Object[]> optionStats = logORepository.findOptionStatisticsByPageId(pageId);

        long totalSelections = optionStats.stream()
                .mapToLong(row -> ((Number) row[1]).longValue())
                .sum();

        return optionStats.stream()
                .map(row -> {
                    Options option = (Options) row[0];
                    Long count = ((Number) row[1]).longValue();
                    Long uniquePlayers = logORepository.countDistinctCharactersByOptionId(option.getOptId());
                    double rate = totalSelections > 0 ? (count * 100.0 / totalSelections) : 0.0;

                    return OptionStatisticsResponse.builder()
                            .optionId(option.getOptId())
                            .pageId(pageId)
                            .optionText(option.getOptContents())
                            .selectionCount(count)
                            .uniquePlayerCount(uniquePlayers)
                            .selectionRate(rate)
                            .build();
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PageStatisticsResponse> getPageDurationStatistics() {
        List<Object[]> pageStats = opsLogBRepository.getPageDurationStatistics();

        return pageStats.stream()
                .map(row -> {
                    Long pageId = ((Number) row[0]).longValue();
                    Double avgDuration = ((Number) row[1]).doubleValue();
                    Long visitCount = ((Number) row[2]).longValue();

                    return PageStatisticsResponse.builder()
                            .pageId(pageId)
                            .visitCount(visitCount)
                            .averageDurationSeconds(avgDuration / 1000.0)
                            .build();
                })
                .sorted((a, b) -> Double.compare(b.getAverageDurationSeconds(), a.getAverageDurationSeconds()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ErrorStatisticsResponse> getErrorStatistics() {
        List<Object[]> errorStats = opsLogXRepository.getErrorCountByService();

        return errorStats.stream()
                .map(row -> {
                    String serviceName = (String) row[0];
                    Long count = ((Number) row[1]).longValue();

                    List<OpsLogX> serviceLogs = opsLogXRepository.findByServiceNameOrderByCreatedAtDesc(serviceName);
                    LocalDateTime firstOccurrence = serviceLogs.isEmpty() ? null :
                            serviceLogs.get(serviceLogs.size() - 1).getCreatedAt();
                    LocalDateTime lastOccurrence = serviceLogs.isEmpty() ? null :
                            serviceLogs.get(0).getCreatedAt();

                    return ErrorStatisticsResponse.builder()
                            .serviceName(serviceName)
                            .errorCount(count)
                            .firstOccurrence(firstOccurrence)
                            .lastOccurrence(lastOccurrence)
                            .build();
                })
                .sorted((a, b) -> Long.compare(b.getErrorCount(), a.getErrorCount()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AccessStatisticsResponse> getAccessStatisticsByPath() {
        List<Object[]> accessStats = opsLogARepository.getAccessStatisticsByPath();

        long totalAccess = accessStats.stream()
                .mapToLong(row -> ((Number) row[1]).longValue())
                .sum();

        return accessStats.stream()
                .map(row -> {
                    String path = (String) row[0];
                    Long count = ((Number) row[1]).longValue();
                    double percentage = totalAccess > 0 ? (count * 100.0 / totalAccess) : 0.0;

                    return AccessStatisticsResponse.builder()
                            .path(path)
                            .accessCount(count)
                            .percentage(percentage)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AccessStatisticsResponse> getAccessStatisticsByMethod() {
        List<Object[]> methodStats = opsLogARepository.getStatisticsByMethod();

        long totalAccess = methodStats.stream()
                .mapToLong(row -> ((Number) row[1]).longValue())
                .sum();

        return methodStats.stream()
                .map(row -> {
                    String method = (String) row[0];
                    Long count = ((Number) row[1]).longValue();
                    double percentage = totalAccess > 0 ? (count * 100.0 / totalAccess) : 0.0;

                    return AccessStatisticsResponse.builder()
                            .method(method)
                            .accessCount(count)
                            .percentage(percentage)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AccessStatisticsResponse> getAccessStatisticsByStatusCode() {
        List<Object[]> statusStats = opsLogARepository.getStatisticsByStatusCode();

        long totalAccess = statusStats.stream()
                .mapToLong(row -> ((Number) row[1]).longValue())
                .sum();

        return statusStats.stream()
                .map(row -> {
                    String statusCode = (String) row[0];
                    Long count = ((Number) row[1]).longValue();
                    double percentage = totalAccess > 0 ? (count * 100.0 / totalAccess) : 0.0;

                    return AccessStatisticsResponse.builder()
                            .statusCode(statusCode)
                            .accessCount(count)
                            .percentage(percentage)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OverallSummaryResponse getOverallSummary() {
        DailyStatisticsService.StatisticsSummary summary = dailyStatisticsService.getOverallSummary();

        LocalDateTime last24Hours = LocalDateTime.now().minusHours(24);
        Long recentErrorCount = opsLogXRepository.countErrorsBetween(last24Hours, LocalDateTime.now());

        Long totalErrorCount = opsLogXRepository.count();

        return OverallSummaryResponse.builder()
                .totalPlays(summary.getTotalPlays())
                .totalSuccess(summary.getTotalSuccess())
                .totalFail(summary.getTotalFail())
                .averageDailyVisitors(summary.getAverageDailyVisitors())
                .averageDailyUniqueVisitors(summary.getAverageDailyUniqueVisitors())
                .averageClearRate(summary.getAverageClearRate())
                .totalErrorCount(totalErrorCount)
                .recentErrorCount(recentErrorCount)
                .build();
    }
}

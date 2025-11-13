package com.example.backend.controller;

import com.example.backend.dto.statistics.*;
import com.example.backend.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;


@Slf4j
@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/summary")
    public ResponseEntity<OverallSummaryResponse> getOverallSummary() {
        log.info("전체 통계 요약 조회 요청");
        OverallSummaryResponse summary = statisticsService.getOverallSummary();
        return ResponseEntity.ok(summary);
    }


    @GetMapping("/daily")
    public ResponseEntity<DailyStatisticsResponse> getDailyStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime date) {
        log.info("일별 통계 조회 요청: date={}", date);
        DailyStatisticsResponse stats = statisticsService.getDailyStatistics(date);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/recent")
    public ResponseEntity<List<DailyStatisticsResponse>> getRecentStatistics(
            @RequestParam(defaultValue = "7") int days) {
        log.info("최근 {}일 통계 조회 요청", days);
        List<DailyStatisticsResponse> stats = statisticsService.getRecentStatistics(days);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/period")
    public ResponseEntity<PeriodStatisticsResponse> getPeriodStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        log.info("기간별 통계 조회 요청: {} ~ {}", startDate, endDate);
        PeriodStatisticsResponse stats = statisticsService.getPeriodStatistics(startDate, endDate);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/popular-stories")
    public ResponseEntity<List<PopularStoryResponse>> getPopularStories(
            @RequestParam(defaultValue = "10") int limit) {
        log.info("인기 스토리 TOP {} 조회 요청", limit);
        List<PopularStoryResponse> stories = statisticsService.getPopularStories(limit);
        return ResponseEntity.ok(stories);
    }

    @GetMapping("/options")
    public ResponseEntity<List<OptionStatisticsResponse>> getOptionStatistics(
            @RequestParam Long pageId) {
        log.info("페이지 {}의 선택지 통계 조회 요청", pageId);
        List<OptionStatisticsResponse> options = statisticsService.getOptionStatistics(pageId);
        return ResponseEntity.ok(options);
    }

    @GetMapping("/page-duration")
    public ResponseEntity<List<PageStatisticsResponse>> getPageDurationStatistics() {
        log.info("페이지 체류 시간 통계 조회 요청");
        List<PageStatisticsResponse> pageStats = statisticsService.getPageDurationStatistics();
        return ResponseEntity.ok(pageStats);
    }

    @GetMapping("/errors")
    public ResponseEntity<List<ErrorStatisticsResponse>> getErrorStatistics() {
        log.info("에러 통계 조회 요청");
        List<ErrorStatisticsResponse> errors = statisticsService.getErrorStatistics();
        return ResponseEntity.ok(errors);
    }

    @GetMapping("/access/by-path")
    public ResponseEntity<List<AccessStatisticsResponse>> getAccessStatisticsByPath() {
        log.info("경로별 접속 통계 조회 요청");
        List<AccessStatisticsResponse> accessStats = statisticsService.getAccessStatisticsByPath();
        return ResponseEntity.ok(accessStats);
    }

    @GetMapping("/access/by-method")
    public ResponseEntity<List<AccessStatisticsResponse>> getAccessStatisticsByMethod() {
        log.info("HTTP 메서드별 접속 통계 조회 요청");
        List<AccessStatisticsResponse> accessStats = statisticsService.getAccessStatisticsByMethod();
        return ResponseEntity.ok(accessStats);
    }

    @GetMapping("/access/by-status")
    public ResponseEntity<List<AccessStatisticsResponse>> getAccessStatisticsByStatusCode() {
        log.info("상태 코드별 접속 통계 조회 요청");
        List<AccessStatisticsResponse> accessStats = statisticsService.getAccessStatisticsByStatusCode();
        return ResponseEntity.ok(accessStats);
    }
}

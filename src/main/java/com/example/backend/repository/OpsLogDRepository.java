package com.example.backend.repository;

import com.example.backend.entity.OpsLogD;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OpsLogDRepository extends JpaRepository<OpsLogD, Long> {

    @Query("SELECT d FROM OpsLogD d WHERE DATE(d.logdDate) = DATE(:date)")
    Optional<OpsLogD> findByDate(@Param("date") LocalDateTime date);

    @Query("SELECT d FROM OpsLogD d WHERE d.logdDate BETWEEN :startDate AND :endDate ORDER BY d.logdDate ASC")
    List<OpsLogD> findByDateBetween(@Param("startDate") LocalDateTime startDate,
                                    @Param("endDate") LocalDateTime endDate);

    @Query("SELECT d FROM OpsLogD d ORDER BY d.logdDate DESC LIMIT :days")
    List<OpsLogD> findRecentDays(@Param("days") int days);

    @Query("SELECT CASE WHEN COUNT(d) > 0 THEN true ELSE false END FROM OpsLogD d WHERE DATE(d.logdDate) = DATE(:date)")
    boolean existsByDate(@Param("date") LocalDateTime date);

    @Query("SELECT SUM(d.logdCounts) FROM OpsLogD d")
    Long getTotalPlayCount();

    @Query("SELECT SUM(d.logdSuccess) FROM OpsLogD d")
    Long getTotalSuccessCount();

    @Query("SELECT SUM(d.logdFail) FROM OpsLogD d")
    Long getTotalFailCount();

    @Query("SELECT AVG(d.logdTotal) FROM OpsLogD d")
    Double getAverageDailyVisitors();

    @Query("SELECT AVG(d.logdUnique) FROM OpsLogD d")
    Double getAverageDailyUniqueVisitors();

    @Query("SELECT AVG(CAST(d.logdSuccess AS double) / NULLIF(d.logdCounts, 0)) FROM OpsLogD d WHERE d.logdCounts > 0")
    Double getAverageClearRate();

    @Query("SELECT d FROM OpsLogD d ORDER BY d.logdTotal DESC LIMIT 1")
    Optional<OpsLogD> findDayWithMostVisitors();

    @Query("SELECT d FROM OpsLogD d ORDER BY d.logdCounts DESC LIMIT 1")
    Optional<OpsLogD> findDayWithMostPlays();

    @Query("SELECT d.logdDate, d.logdTotal, d.logdUnique FROM OpsLogD d ORDER BY d.logdDate DESC LIMIT :days")
    List<Object[]> getGrowthTrend(@Param("days") int days);

    @Query("SELECT d FROM OpsLogD d WHERE YEAR(d.logdDate) = :year AND MONTH(d.logdDate) = :month ORDER BY d.logdDate ASC")
    List<OpsLogD> findByYearAndMonth(@Param("year") int year, @Param("month") int month);

    @Query("SELECT YEAR(d.logdDate), MONTH(d.logdDate), " +
            "SUM(d.logdTotal), SUM(d.logdUnique), SUM(d.logdCounts), " +
            "SUM(d.logdSuccess), SUM(d.logdFail) FROM OpsLogD d " +
            "GROUP BY YEAR(d.logdDate), MONTH(d.logdDate) ORDER BY YEAR(d.logdDate), MONTH(d.logdDate)")
    List<Object[]> getMonthlyStatistics();

    @Query("SELECT MAX(d.logdDate) FROM OpsLogD d")
    LocalDateTime findLatestDate();

    @Query("DELETE FROM OpsLogD d WHERE d.logdDate < :cutoffDate")
    void deleteOldStatistics(@Param("cutoffDate") LocalDateTime cutoffDate);
}

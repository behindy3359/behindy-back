package com.example.backend.repository;

import com.example.backend.entity.OpsLogA;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OpsLogARepository extends JpaRepository<OpsLogA, Long> {

    @Query("SELECT a FROM OpsLogA a WHERE a.user.userId = :userId ORDER BY a.createdAt DESC")
    List<OpsLogA> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    @Query("SELECT a FROM OpsLogA a WHERE a.logaAddress = :ipAddress ORDER BY a.createdAt DESC")
    List<OpsLogA> findByIpAddress(@Param("ipAddress") String ipAddress);

    @Query("SELECT a FROM OpsLogA a WHERE a.createdAt BETWEEN :startDate AND :endDate ORDER BY a.createdAt DESC")
    List<OpsLogA> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);

    @Query("SELECT a FROM OpsLogA a WHERE a.logaPath = :path ORDER BY a.createdAt DESC")
    List<OpsLogA> findByPath(@Param("path") String path);

    @Query("SELECT a FROM OpsLogA a WHERE a.logaMethod = :method ORDER BY a.createdAt DESC")
    List<OpsLogA> findByMethod(@Param("method") String method);

    @Query("SELECT a FROM OpsLogA a WHERE a.logaStatusCode = :statusCode ORDER BY a.createdAt DESC")
    List<OpsLogA> findByStatusCode(@Param("statusCode") String statusCode);

    @Query("SELECT a.logaPath, COUNT(a) as count FROM OpsLogA a " +
            "GROUP BY a.logaPath ORDER BY count DESC")
    List<Object[]> getAccessStatisticsByPath();

    @Query("SELECT COUNT(DISTINCT a.user.userId) FROM OpsLogA a " +
            "WHERE DATE(a.createdAt) = DATE(:date) AND a.user IS NOT NULL")
    Long countUniqueVisitorsByDate(@Param("date") LocalDateTime date);

    @Query("SELECT COUNT(a) FROM OpsLogA a WHERE DATE(a.createdAt) = DATE(:date)")
    Long countTotalVisitorsByDate(@Param("date") LocalDateTime date);

    @Query("SELECT COUNT(DISTINCT a.user.userId) FROM OpsLogA a " +
            "WHERE a.createdAt BETWEEN :startDate AND :endDate AND a.user IS NOT NULL")
    Long countUniqueVisitorsBetween(@Param("startDate") LocalDateTime startDate,
                                    @Param("endDate") LocalDateTime endDate);

    @Query("SELECT a.logaPath, COUNT(a) as count FROM OpsLogA a " +
            "GROUP BY a.logaPath ORDER BY count DESC")
    List<Object[]> findMostAccessedEndpoints();

    @Query("SELECT COUNT(a) FROM OpsLogA a WHERE a.user.userId = :userId")
    Long countByUserId(@Param("userId") Long userId);

    @Query("SELECT a.logaMethod, COUNT(a) as count FROM OpsLogA a " +
            "GROUP BY a.logaMethod ORDER BY count DESC")
    List<Object[]> getStatisticsByMethod();

    @Query("SELECT a.logaStatusCode, COUNT(a) as count FROM OpsLogA a " +
            "GROUP BY a.logaStatusCode ORDER BY a.logaStatusCode")
    List<Object[]> getStatisticsByStatusCode();

    @Query("SELECT HOUR(a.createdAt) as hour, COUNT(a) as count FROM OpsLogA a " +
            "WHERE a.createdAt >= :startTime GROUP BY HOUR(a.createdAt) ORDER BY hour")
    List<Object[]> getHourlyAccessTrend(@Param("startTime") LocalDateTime startTime);

    @Query("SELECT a.logaAddress, COUNT(a) as count FROM OpsLogA a " +
            "WHERE a.createdAt >= :startTime GROUP BY a.logaAddress " +
            "HAVING COUNT(a) > :threshold ORDER BY count DESC")
    List<Object[]> findSuspiciousIPs(@Param("startTime") LocalDateTime startTime,
                                      @Param("threshold") long threshold);

    @Query("DELETE FROM OpsLogA a WHERE a.createdAt < :cutoffDate")
    void deleteOldAccessLogs(@Param("cutoffDate") LocalDateTime cutoffDate);

    @Query("SELECT a FROM OpsLogA a WHERE a.user IS NOT NULL ORDER BY a.createdAt DESC")
    List<OpsLogA> findAuthenticatedAccessLogs();

    @Query("SELECT a FROM OpsLogA a WHERE a.user IS NULL ORDER BY a.createdAt DESC")
    List<OpsLogA> findUnauthenticatedAccessLogs();
}

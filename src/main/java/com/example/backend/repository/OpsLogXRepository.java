package com.example.backend.repository;

import com.example.backend.entity.OpsLogX;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OpsLogXRepository extends JpaRepository<OpsLogX, Long> {

    @Query("SELECT x FROM OpsLogX x WHERE x.logxService = :serviceName ORDER BY x.createdAt DESC")
    List<OpsLogX> findByServiceNameOrderByCreatedAtDesc(@Param("serviceName") String serviceName);

    @Query("SELECT x FROM OpsLogX x WHERE x.createdAt BETWEEN :startDate AND :endDate ORDER BY x.createdAt DESC")
    List<OpsLogX> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);

    @Query("SELECT x FROM OpsLogX x WHERE x.logxMessage LIKE %:keyword% ORDER BY x.createdAt DESC")
    List<OpsLogX> findByMessageContaining(@Param("keyword") String keyword);

    @Query("SELECT x.logxService, COUNT(x) as errorCount FROM OpsLogX x " +
            "GROUP BY x.logxService ORDER BY errorCount DESC")
    List<Object[]> getErrorCountByService();

    @Query("SELECT COUNT(x) FROM OpsLogX x WHERE x.logxService = :serviceName")
    Long countErrorsByService(@Param("serviceName") String serviceName);

    @Query("SELECT x FROM OpsLogX x ORDER BY x.createdAt DESC LIMIT :limit")
    List<OpsLogX> findRecentErrors(@Param("limit") int limit);

    @Query("SELECT COUNT(x) FROM OpsLogX x WHERE x.createdAt BETWEEN :startDate AND :endDate")
    Long countErrorsBetween(@Param("startDate") LocalDateTime startDate,
                           @Param("endDate") LocalDateTime endDate);

    @Query("SELECT x.logxMessage, COUNT(x) as count FROM OpsLogX x " +
            "GROUP BY x.logxMessage ORDER BY count DESC")
    List<Object[]> findMostFrequentErrors();

    @Query("SELECT x FROM OpsLogX x WHERE DATE(x.createdAt) = DATE(:date) ORDER BY x.createdAt DESC")
    List<OpsLogX> findErrorsByDate(@Param("date") LocalDateTime date);

    @Query("SELECT HOUR(x.createdAt) as hour, COUNT(x) as count FROM OpsLogX x " +
            "WHERE x.createdAt >= :startTime GROUP BY HOUR(x.createdAt) ORDER BY hour")
    List<Object[]> getHourlyErrorTrend(@Param("startTime") LocalDateTime startTime);

    @Query("DELETE FROM OpsLogX x WHERE x.createdAt < :cutoffDate")
    void deleteOldErrors(@Param("cutoffDate") LocalDateTime cutoffDate);

    @Query("SELECT x FROM OpsLogX x WHERE x.logxService = :serviceName " +
            "AND x.createdAt BETWEEN :startDate AND :endDate ORDER BY x.createdAt DESC")
    List<OpsLogX> findByServiceAndDateRange(@Param("serviceName") String serviceName,
                                            @Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);
}

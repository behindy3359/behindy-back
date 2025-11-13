package com.example.backend.repository;

import com.example.backend.entity.LogE;
import com.example.backend.entity.OpsLogB;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OpsLogBRepository extends JpaRepository<OpsLogB, Long> {

    @Query("SELECT b FROM OpsLogB b WHERE b.loge.logeId = :logeId ORDER BY b.createdAt ASC")
    List<OpsLogB> findByLogeIdOrderByCreatedAtAsc(@Param("logeId") Long logeId);

    @Query("SELECT AVG(b.logbDur) FROM OpsLogB b WHERE b.logbPage = :pageId")
    Double getAverageDurationByPageId(@Param("pageId") Long pageId);

    @Query("SELECT COUNT(b) FROM OpsLogB b WHERE b.logbPage = :pageId")
    Long countVisitsByPageId(@Param("pageId") Long pageId);

    @Query("SELECT COUNT(b) FROM OpsLogB b WHERE b.logbOpt = :optionId")
    Long countByOptionId(@Param("optionId") Long optionId);

    @Query("SELECT b FROM OpsLogB b WHERE b.createdAt BETWEEN :startDate AND :endDate ORDER BY b.createdAt DESC")
    List<OpsLogB> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);

    @Query("SELECT b.logbPage, AVG(b.logbDur) as avgDuration, COUNT(b) as visitCount " +
            "FROM OpsLogB b GROUP BY b.logbPage ORDER BY avgDuration DESC")
    List<Object[]> getPageDurationStatistics();

    @Query("SELECT b.logbOpt, COUNT(b) as selectionCount FROM OpsLogB b " +
            "GROUP BY b.logbOpt ORDER BY selectionCount DESC")
    List<Object[]> getOptionSelectionStatistics();

    @Query("SELECT SUM(b.logbDur) FROM OpsLogB b WHERE b.loge.logeId = :logeId")
    Long getTotalPlayTimeByLogeId(@Param("logeId") Long logeId);

    @Query("SELECT b.logbPage, AVG(b.logbDur) as avgDuration FROM OpsLogB b " +
            "GROUP BY b.logbPage ORDER BY avgDuration DESC")
    List<Object[]> findPagesWithLongestDuration();

    @Query("SELECT b.logbOpt, COUNT(b) as count FROM OpsLogB b " +
            "WHERE b.logbPage = :pageId GROUP BY b.logbOpt ORDER BY count DESC")
    List<Object[]> findMostSelectedOptionsByPageId(@Param("pageId") Long pageId);

    @Query("SELECT COUNT(b) FROM OpsLogB b WHERE b.loge.logeId = :logeId")
    Long countByLogeId(@Param("logeId") Long logeId);

    @Query("SELECT b FROM OpsLogB b WHERE b.logbDur < :thresholdMs ORDER BY b.logbDur ASC")
    List<OpsLogB> findQuickSkippedPages(@Param("thresholdMs") Long thresholdMs);

    void deleteByLogeLogeId(Long logeId);

    @Query("SELECT b FROM OpsLogB b WHERE b.character.charId = :charId AND b.loge IS NULL ORDER BY b.createdAt ASC")
    List<OpsLogB> findUnlinkedPlayLogsByCharacter(@Param("charId") Long charId);

    @Query("SELECT COUNT(b) FROM OpsLogB b WHERE b.character.charId = :charId AND b.loge IS NULL")
    Long countUnlinkedPlayLogsByCharacter(@Param("charId") Long charId);
}

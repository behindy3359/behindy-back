package com.example.backend.repository;

import com.example.backend.entity.LogO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LogORepository extends JpaRepository<LogO, Long> {

    @Query("SELECT l FROM LogO l WHERE l.character.charId = :charId ORDER BY l.createdAt DESC")
    List<LogO> findByCharacterIdOrderByCreatedAtDesc(@Param("charId") Long charId);

    @Query("SELECT COUNT(l) FROM LogO l WHERE l.options.optId = :optionId")
    Long countByOptionId(@Param("optionId") Long optionId);

    @Query("SELECT COUNT(DISTINCT l.character.charId) FROM LogO l WHERE l.options.optId = :optionId")
    Long countDistinctCharactersByOptionId(@Param("optionId") Long optionId);

    @Query("SELECT COUNT(l) FROM LogO l WHERE l.character.charId = :charId AND l.options.optId = :optionId")
    Long countByCharacterAndOption(@Param("charId") Long charId, @Param("optionId") Long optionId);

    @Query("SELECT l.options, COUNT(l) as selectionCount FROM LogO l " +
            "WHERE l.options.pageId = :pageId GROUP BY l.options ORDER BY selectionCount DESC")
    List<Object[]> findOptionStatisticsByPageId(@Param("pageId") Long pageId);

    @Query("SELECT l FROM LogO l WHERE l.createdAt BETWEEN :startDate AND :endDate ORDER BY l.createdAt DESC")
    List<LogO> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(l) FROM LogO l WHERE l.character.charId = :charId")
    Long countByCharacterId(@Param("charId") Long charId);

    @Query("SELECT l.options, COUNT(l) as selectionCount FROM LogO l " +
            "GROUP BY l.options ORDER BY selectionCount DESC")
    List<Object[]> findMostPopularOptions();

    @Query("SELECT l FROM LogO l WHERE l.options.pageId IN " +
            "(SELECT p.pageId FROM Page p WHERE p.stoId = :storyId) " +
            "ORDER BY l.createdAt DESC")
    List<LogO> findByStoryId(@Param("storyId") Long storyId);

    @Query("SELECT l FROM LogO l WHERE l.character.charId = :charId " +
            "ORDER BY l.createdAt DESC LIMIT :limit")
    List<LogO> findRecentChoicesByCharacter(@Param("charId") Long charId, @Param("limit") int limit);
}

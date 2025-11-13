package com.example.backend.repository;

import com.example.backend.entity.Character;
import com.example.backend.entity.LogE;
import com.example.backend.entity.Story;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LogERepository extends JpaRepository<LogE, Long> {

    @Query("SELECT l.story.stoId FROM LogE l WHERE l.character.charId = :charId AND l.logeEnding = 1")
    List<Long> findCompletedStoryIdsByCharacter(@Param("charId") Long charId);

    @Query("SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END FROM LogE l " +
            "WHERE l.character.charId = :charId AND l.story.stoId = :storyId AND l.logeEnding = 1")
    boolean hasCharacterCompletedStory(@Param("charId") Long charId, @Param("storyId") Long storyId);

    @Query("SELECT l FROM LogE l WHERE l.character.charId = :charId ORDER BY l.createdAt DESC")
    List<LogE> findByCharacterIdOrderByCreatedAtDesc(@Param("charId") Long charId);

    @Query("SELECT COUNT(l) FROM LogE l WHERE l.story.stoId = :storyId AND l.logeEnding = 1")
    Long countCompletionsByStory(@Param("storyId") Long storyId);

    @Query("SELECT COUNT(l) FROM LogE l WHERE l.story.stoId = :storyId")
    Long countTotalPlaysByStory(@Param("storyId") Long storyId);

    @Query("SELECT COUNT(l) FROM LogE l WHERE l.character.charId = :charId AND l.logeEnding = 1")
    Long countCompletionsByCharacter(@Param("charId") Long charId);

    @Query("SELECT COUNT(l) FROM LogE l WHERE l.character.charId = :charId")
    Long countTotalPlaysByCharacter(@Param("charId") Long charId);

    @Query("SELECT COUNT(l) FROM LogE l WHERE l.story.station.staName = :stationName " +
            "AND l.story.station.staLine = :lineNumber AND l.logeEnding = 1")
    Long countCompletionsByStation(@Param("stationName") String stationName, @Param("lineNumber") Integer lineNumber);

    @Query("SELECT l FROM LogE l WHERE l.createdAt >= :startDate AND l.logeEnding = 1 ORDER BY l.createdAt DESC")
    List<LogE> findRecentCompletions(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT l FROM LogE l WHERE l.character.charId = :charId ORDER BY l.createdAt DESC")
    List<LogE> findRecentGamesByCharacter(@Param("charId") Long charId);

    @Query("SELECT l.story, COUNT(l) as completionCount FROM LogE l " +
            "WHERE l.logeEnding = 1 GROUP BY l.story ORDER BY completionCount DESC")
    List<Object[]> findPopularStoriesByCompletions();

    @Query("SELECT l.story.station.staName, l.story.station.staLine, " +
            "SUM(CASE WHEN l.logeEnding = 1 THEN 1 ELSE 0 END) as clearCount, " +
            "COUNT(l) as totalPlayCount, " +
            "MAX(l.createdAt) as lastVisitedAt " +
            "FROM LogE l " +
            "WHERE l.character.charId = :charId " +
            "GROUP BY l.story.station.staName, l.story.station.staLine " +
            "ORDER BY clearCount DESC, totalPlayCount DESC")
    List<Object[]> findVisitedStationsByCharacter(@Param("charId") Long charId);
}
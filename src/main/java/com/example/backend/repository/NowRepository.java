package com.example.backend.repository;

import com.example.backend.entity.Character;
import com.example.backend.entity.Now;
import com.example.backend.entity.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface NowRepository extends JpaRepository<Now, Long> {

    Optional<Now> findByCharacter(Character character);

    @Query("SELECT n FROM Now n WHERE n.character.charId = :charId")
    Optional<Now> findByCharacterId(@Param("charId") Long charId);

    boolean existsByCharacter(Character character);

    List<Now> findByPage(Page page);

    @Query("SELECT COUNT(n) FROM Now n WHERE n.page.pageId = :pageId")
    Long countCharactersAtPage(@Param("pageId") Long pageId);

    @Query("SELECT n FROM Now n JOIN FETCH n.character c JOIN FETCH n.page p WHERE c.deletedAt IS NULL")
    List<Now> findAllActiveGameSessions();

    @Query("SELECT n FROM Now n JOIN FETCH n.character c JOIN n.page p WHERE p.stoId = :storyId AND c.deletedAt IS NULL")
    List<Now> findCharactersInStory(@Param("storyId") Long storyId);

    @Modifying
    @Transactional
    void deleteByCharacter(Character character);

    @Modifying
    @Transactional
    @Query("DELETE FROM Now n WHERE n.character.charId = :charId")
    void deleteByCharacterId(@Param("charId") Long charId);

    @Query("SELECT n FROM Now n JOIN FETCH n.page p WHERE n.character.charId = :charId")
    Optional<Now> findByCharacterIdWithPage(@Param("charId") Long charId);

    @Query("SELECT n FROM Now n WHERE n.createdAt < :cutoffDate")
    List<Now> findOldGameSessions(@Param("cutoffDate") java.time.LocalDateTime cutoffDate);
}
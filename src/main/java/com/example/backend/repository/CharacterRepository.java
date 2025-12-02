package com.example.backend.repository;

import com.example.backend.entity.Character;
import com.example.backend.entity.User;
import com.example.backend.repository.common.UserOwnedSoftDeleteRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CharacterRepository extends UserOwnedSoftDeleteRepository<Character, Long> {

    @Query("SELECT c FROM Character c WHERE c.user = :user AND c.deletedAt IS NULL")
    Optional<Character> findByUserAndDeletedAtIsNull(@Param("user") User user);

    boolean existsByCharNameAndDeletedAtIsNull(String charName);

    @Query("SELECT COUNT(c) > 0 FROM Character c WHERE c.user = :user AND c.deletedAt IS NULL")
    boolean existsByUserAndDeletedAtIsNull(@Param("user") User user);

    @Query("SELECT c FROM Character c WHERE c.user = :user ORDER BY c.createdAt DESC")
    List<Character> findByUserOrderByCreatedAtDesc(@Param("user") User user);

    @Query("SELECT c FROM Character c WHERE c.user = :user AND c.deletedAt IS NULL ORDER BY c.createdAt DESC")
    List<Character> findByUserAndDeletedAtIsNullOrderByCreatedAtDesc(@Param("user") User user);

    @Query("SELECT c FROM Character c WHERE c.deletedAt IS NULL AND " +
            "(c.charHealth <= :threshold OR c.charSanity <= :threshold)")
    List<Character> findCharactersInDanger(@Param("threshold") Integer threshold);

    @Query("SELECT c FROM Character c WHERE c.deletedAt IS NULL AND " +
            "(c.charHealth <= 0 OR c.charSanity <= 0)")
    List<Character> findCharactersToKill();

    @Query("SELECT c FROM Character c WHERE c.charId = :charId AND c.deletedAt IS NULL")
    Optional<Character> findAliveCharacterById(@Param("charId") Long charId);
}
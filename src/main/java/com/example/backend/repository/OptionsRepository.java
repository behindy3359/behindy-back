package com.example.backend.repository;


import com.example.backend.entity.Options;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OptionsRepository extends JpaRepository<Options, Long> {

    List<Options> findByPageId(Long pageId);

    @Query("SELECT COUNT(o) FROM Options o WHERE o.pageId = :pageId")
    Long countOptionsByPageId(@Param("pageId") Long pageId);

    @Query("SELECT o FROM Options o WHERE o.pageId = :pageId AND o.optEffect = 'health'")
    List<Options> findHealthOptionsByPageId(@Param("pageId") Long pageId);

    @Query("SELECT o FROM Options o WHERE o.pageId = :pageId AND o.optEffect = 'sanity'")
    List<Options> findSanityOptionsByPageId(@Param("pageId") Long pageId);

    @Query("SELECT o FROM Options o WHERE o.pageId = :pageId AND o.optAmount > 0")
    List<Options> findPositiveOptionsByPageId(@Param("pageId") Long pageId);

    @Query("SELECT o FROM Options o WHERE o.pageId = :pageId AND o.optAmount < 0")
    List<Options> findNegativeOptionsByPageId(@Param("pageId") Long pageId);

    @Query("SELECT o FROM Options o WHERE o.pageId = :pageId AND o.optAmount = 0")
    List<Options> findNeutralOptionsByPageId(@Param("pageId") Long pageId);

    @Query("SELECT o FROM Options o WHERE o.pageId = :pageId AND o.optAmount BETWEEN :minAmount AND :maxAmount")
    List<Options> findOptionsByAmountRange(@Param("pageId") Long pageId, @Param("minAmount") Integer minAmount, @Param("maxAmount") Integer maxAmount);
}
package com.example.backend.repository;

import com.example.backend.entity.Station;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StationRepository extends JpaRepository<Station, Long> {

    Optional<Station> findByStaNameAndStaLine(String staName, Integer staLine);

    List<Station> findByStaLineOrderByStaName(Integer staLine);

    List<Station> findByStaNameOrderByStaLine(String staName);

    Optional<Station> findByApiStationId(String apiStationId);

    @Query("SELECT DISTINCT s.station FROM Story s")
    List<Station> findStationsWithStories();

    @Query("SELECT DISTINCT s.station FROM Story s WHERE s.station.staLine = :lineNumber")
    List<Station> findStationsWithStoriesByLine(@Param("lineNumber") Integer lineNumber);

    @Query("SELECT s, COUNT(st) as storyCount FROM Station s LEFT JOIN Story st ON s = st.station GROUP BY s")
    List<Object[]> findStationsWithStoryCount();

    @Query("SELECT s FROM Station s WHERE s.staName LIKE %:keyword%")
    List<Station> findByStaNameContaining(@Param("keyword") String keyword);

    @Query("SELECT COUNT(s) FROM Station s")
    Long countAllStations();

    @Query("SELECT COUNT(s) FROM Station s WHERE s.staLine = :lineNumber")
    Long countStationsByLine(@Param("lineNumber") Integer lineNumber);

    @Query("SELECT COUNT(DISTINCT s.staLine) FROM Station s")
    Long countDistinctStaLine();
}
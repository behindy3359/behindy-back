package com.example.backend.repository;

import com.example.backend.entity.Station;
import com.example.backend.entity.Story;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StoryRepository extends JpaRepository<Story, Long> {

    List<Story> findByStation(Station station);

    @Query("SELECT s FROM Story s WHERE s.station.staLine = :lineNumber")
    List<Story> findByStationLine(@Param("lineNumber") Integer lineNumber);

    @Query("SELECT s FROM Story s WHERE s.station.staName = :stationName")
    List<Story> findByStationName(@Param("stationName") String stationName);

    @Query("SELECT s FROM Story s WHERE s.station.staName = :stationName AND s.station.staLine = :lineNumber")
    List<Story> findByStationNameAndLine(@Param("stationName") String stationName, @Param("lineNumber") Integer lineNumber);

    @Query("SELECT s FROM Story s WHERE s.stoLength BETWEEN :minLength AND :maxLength")
    List<Story> findByLengthRange(@Param("minLength") Integer minLength, @Param("maxLength") Integer maxLength);

    @Query("SELECT s FROM Story s WHERE s.stoLength <= 5")
    List<Story> findShortStories();

    @Query("SELECT s FROM Story s WHERE s.stoLength >= 10")
    List<Story> findLongStories();

    @Query(value = "SELECT * FROM STO ORDER BY RANDOM() LIMIT :limit", nativeQuery = true)
    List<Story> findRandomStories(@Param("limit") Integer limit);

    @Query(value = "SELECT s.* FROM STO s JOIN STA st ON s.sta_id = st.sta_id WHERE st.sta_line = :lineNumber ORDER BY RANDOM() LIMIT :limit", nativeQuery = true)
    List<Story> findRandomStoriesByLine(@Param("lineNumber") Integer lineNumber, @Param("limit") Integer limit);
}
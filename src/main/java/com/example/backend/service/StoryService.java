package com.example.backend.service;

import com.example.backend.dto.game.StoryListResponse;
import com.example.backend.dto.game.StoryResponse;
import com.example.backend.entity.Now;
import com.example.backend.entity.Station;
import com.example.backend.entity.Story;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.LogERepository;
import com.example.backend.repository.NowRepository;
import com.example.backend.repository.StationRepository;
import com.example.backend.repository.StoryRepository;
import com.example.backend.service.mapper.EntityDtoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoryService {

    private final StoryRepository storyRepository;
    private final StationRepository stationRepository;
    private final NowRepository nowRepository;
    private final LogERepository logERepository;
    private final AuthService authService;
    private final CharacterService characterService;
    private final EntityDtoMapper entityDtoMapper;

    @Transactional(readOnly = true)
    public List<StoryResponse> getUncompletedStoriesByStation(String stationName, Integer lineNumber, Long characterId) {
        List<Story> allStories = storyRepository.findByStationNameAndLine(stationName, lineNumber);

        if (allStories.isEmpty()) {
            log.info("{}역 {}호선에 스토리가 없습니다.", stationName, lineNumber);
            return List.of();
        }

        List<Long> completedStoryIds = logERepository.findCompletedStoryIdsByCharacter(characterId);

        List<Story> uncompletedStories = allStories.stream()
                .filter(story -> !completedStoryIds.contains(story.getStoId()))
                .collect(Collectors.toList());

        log.info("{}역 {}호선 - 전체: {}개, 클리어: {}개, 미완료: {}개",
                stationName, lineNumber, allStories.size(), completedStoryIds.size(), uncompletedStories.size());

        return uncompletedStories.stream()
                .map(entityDtoMapper::toStoryResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<StoryResponse> selectStoryForGameEntry(String stationName, Integer lineNumber, Long characterId) {
        List<StoryResponse> uncompletedStories = getUncompletedStoriesByStation(stationName, lineNumber, characterId);

        if (uncompletedStories.isEmpty()) {
            log.info("{}역 {}호선에 플레이 가능한 스토리가 없습니다.", stationName, lineNumber);
            return Optional.empty();
        }

        StoryResponse selectedStory = uncompletedStories.get(0);

        log.info("게임 진입 스토리 선택: {} (ID: {})", selectedStory.getStoryTitle(), selectedStory.getStoryId());

        return Optional.of(selectedStory);
    }

    @Transactional(readOnly = true)
    public boolean hasCharacterCompletedStory(Long characterId, Long storyId) {
        return logERepository.hasCharacterCompletedStory(characterId, storyId);
    }

    @Transactional(readOnly = true)
    public CharacterStoryStatistics getCharacterStoryStatistics(Long characterId) {
        Long totalClears = logERepository.countCompletionsByCharacter(characterId);
        Long totalPlays = logERepository.countTotalPlaysByCharacter(characterId);

        double clearRate = totalPlays > 0 ? (double) totalClears / totalPlays * 100 : 0.0;

        return CharacterStoryStatistics.builder()
                .characterId(characterId)
                .totalClears(totalClears)
                .totalPlays(totalPlays)
                .clearRate(clearRate)
                .build();
    }

    @Transactional(readOnly = true)
    public List<StoryResponse> getAllStories() {
        List<Story> stories = storyRepository.findAll();

        return stories.stream()
                .map(entityDtoMapper::toStoryResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StoryResponse> getStoriesByLine(Integer lineNumber) {
        List<Story> stories = storyRepository.findByStationLine(lineNumber);

        return stories.stream()
                .map(entityDtoMapper::toStoryResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StoryListResponse getStoriesByStation(String stationName) {
        List<Story> stories = storyRepository.findByStationName(stationName);

        if (stories.isEmpty()) {
            throw new ResourceNotFoundException("Stories", "stationName", stationName);
        }

        Station station = stories.get(0).getStation();

        List<StoryResponse> storyResponses = stories.stream()
                .map(entityDtoMapper::toStoryResponse)
                .collect(Collectors.toList());

        boolean hasActiveGame = checkHasActiveGame();

        return StoryListResponse.builder()
                .stories(storyResponses)
                .stationName(station.getStaName())
                .stationLine(station.getStaLine())
                .hasActiveGame(hasActiveGame)
                .build();
    }

    @Cacheable(value = "stories", key = "#stationName + '_' + #lineNumber")
    @Transactional(readOnly = true)
    public StoryListResponse getStoriesByStationAndLine(String stationName, Integer lineNumber) {
        List<Story> stories = storyRepository.findByStationNameAndLine(stationName, lineNumber);

        if (stories.isEmpty()) {
            Optional<Station> station = stationRepository.findByStaNameAndStaLine(stationName, lineNumber);
            if (station.isEmpty()) {
                throw new ResourceNotFoundException("Station", "name and line", stationName + " " + lineNumber);
            }

            return StoryListResponse.builder()
                    .stories(List.of())
                    .stationName(stationName)
                    .stationLine(lineNumber)
                    .hasActiveGame(checkHasActiveGame())
                    .build();
        }

        List<StoryResponse> storyResponses = stories.stream()
                .map(entityDtoMapper::toStoryResponse)
                .collect(Collectors.toList());

        return StoryListResponse.builder()
                .stories(storyResponses)
                .stationName(stationName)
                .stationLine(lineNumber)
                .hasActiveGame(checkHasActiveGame())
                .build();
    }

    @Cacheable(value = "stories", key = "'story_' + #storyId")
    @Transactional(readOnly = true)
    public StoryResponse getStoryById(Long storyId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException("Story", "id", storyId));

        return entityDtoMapper.toStoryResponse(story);
    }

    @Transactional(readOnly = true)
    public List<StoryResponse> getRandomStories(Integer count) {
        List<Story> stories = storyRepository.findRandomStories(count);

        return stories.stream()
                .map(entityDtoMapper::toStoryResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StoryResponse> getRandomStoriesByLine(Integer lineNumber, Integer count) {
        List<Story> stories = storyRepository.findRandomStoriesByLine(lineNumber, count);

        return stories.stream()
                .map(entityDtoMapper::toStoryResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StoryResponse> getStoriesByDifficulty(String difficulty) {
        List<Story> stories;

        switch (difficulty.toLowerCase()) {
            case "easy", "쉬움" -> stories = storyRepository.findShortStories();
            case "hard", "어려움" -> stories = storyRepository.findLongStories();
            case "medium", "보통" -> stories = storyRepository.findByLengthRange(6, 9);
            default -> stories = storyRepository.findByLengthRange(6, 9);
        }

        return stories.stream()
                .map(entityDtoMapper::toStoryResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public boolean canPlayStory(Long storyId) {
        try {
            authService.getCurrentUser();

            Optional<com.example.backend.dto.character.CharacterResponse> character =
                    characterService.getCurrentCharacterOptional();

            if (character.isEmpty()) {
                return false;
            }

            com.example.backend.dto.character.CharacterResponse charResp = character.get();
            if (charResp.getCharHealth() <= 0 || charResp.getCharSanity() <= 0) {
                return false;
            }

            Optional<Now> activeGame = nowRepository.findByCharacterId(charResp.getCharId());
            return activeGame.isEmpty();

        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkHasActiveGame() {
        try {
            authService.getCurrentUser();
            Optional<com.example.backend.dto.character.CharacterResponse> character =
                    characterService.getCurrentCharacterOptional();

            if (character.isEmpty()) {
                return false;
            }

            Optional<Now> activeGame = nowRepository.findByCharacterId(character.get().getCharId());
            return activeGame.isPresent();

        } catch (Exception e) {
            return false;
        }
    }

    @CacheEvict(value = "stories", key = "#stationName + '_' + #lineNumber")
    @Transactional
    public Story createStory(String title, String stationName, Integer lineNumber, Integer length) {
        Station station = stationRepository.findByStaNameAndStaLine(stationName, lineNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Station", "name and line", stationName + " " + lineNumber));

        Story story = Story.builder()
                .station(station)
                .stoTitle(title)
                .stoLength(length)
                .build();

        Story savedStory = storyRepository.save(story);

        return savedStory;
    }

    @Transactional
    public void deleteStory(Long storyId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException("Story", "id", storyId));

        List<Now> activeGames = nowRepository.findCharactersInStory(storyId);
        if (!activeGames.isEmpty()) {
            throw new IllegalStateException("진행 중인 게임이 있는 스토리는 삭제할 수 없습니다.");
        }

        storyRepository.delete(story);
        log.info("스토리 삭제: storyId={}, title={}", storyId, story.getStoTitle());
    }

    @Transactional(readOnly = true)
    public List<StoryResponse> getStoriesWithStatistics() {
        List<Story> stories = storyRepository.findAll();

        return stories.stream()
                .map(story -> {
                    StoryResponse response = entityDtoMapper.toStoryResponse(story);

                    List<Now> currentPlayers = nowRepository.findCharactersInStory(story.getStoId());

                    return response;
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Station> getStationsWithoutStories() {
        List<Station> allStations = stationRepository.findAll();
        List<Station> stationsWithStories = stationRepository.findStationsWithStories();

        return allStations.stream()
                .filter(station -> !stationsWithStories.contains(station))
                .collect(Collectors.toList());
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CharacterStoryStatistics {
        private Long characterId;
        private Long totalClears;
        private Long totalPlays;
        private Double clearRate;
    }
}
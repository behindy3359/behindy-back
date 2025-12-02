package com.example.backend.service;

import com.example.backend.dto.character.CharacterCreateRequest;
import com.example.backend.dto.character.CharacterGameStatusResponse;
import com.example.backend.dto.character.CharacterResponse;
import com.example.backend.dto.character.VisitedStationResponse;
import com.example.backend.entity.Character;
import com.example.backend.entity.User;
import com.example.backend.entity.Now;
import com.example.backend.entity.Page;
import com.example.backend.entity.Story;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.CharacterRepository;
import com.example.backend.repository.LogERepository;
import com.example.backend.repository.NowRepository;
import com.example.backend.repository.StoryRepository;
import com.example.backend.service.mapper.EntityDtoMapper;
import com.example.backend.util.HtmlSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CharacterService {

    private final CharacterRepository characterRepository;
    private final AuthService authService;
    private final HtmlSanitizer htmlSanitizer;
    private final EntityDtoMapper entityDtoMapper;
    private final NowRepository nowRepository;
    private final StoryRepository storyRepository;
    private final LogERepository logERepository;

    @Transactional
    public CharacterResponse createCharacter(CharacterCreateRequest request) {
        User currentUser = authService.getCurrentUser();

        boolean hasExistingCharacter = characterRepository.existsByUserAndDeletedAtIsNull(currentUser);

        if (hasExistingCharacter) {
            throw new IllegalStateException("이미 살아있는 캐릭터가 있습니다. 기존 캐릭터가 사망해야 새 캐릭터를 생성할 수 있습니다.");
        }

        String sanitizedName = htmlSanitizer.sanitize(request.getCharName());

        boolean nameExists = characterRepository.existsByCharNameAndDeletedAtIsNull(sanitizedName);

        if (nameExists) {
            throw new IllegalArgumentException("이미 사용 중인 캐릭터 이름입니다.");
        }

        Character character = Character.builder()
                .user(currentUser)
                .charName(sanitizedName)
                .charHealth(100)
                .charSanity(100)
                .build();

        Character savedCharacter = characterRepository.save(character);

        CharacterResponse response = entityDtoMapper.toCharacterResponse(savedCharacter);

        return response;
    }

    @Transactional(readOnly = true)
    public CharacterResponse getCurrentCharacter() {
        User currentUser = authService.getCurrentUser();

        Optional<Character> characterOpt = characterRepository.findByUserAndDeletedAtIsNull(currentUser);

        if (characterOpt.isEmpty()) {
            throw new ResourceNotFoundException("Character", "user", currentUser.getUserId());
        }

        Character character = characterOpt.get();

        return entityDtoMapper.toCharacterResponse(character);
    }

    @Transactional(readOnly = true)
    public Optional<CharacterResponse> getCurrentCharacterOptional() {
        User currentUser = authService.getCurrentUser();

        Optional<Character> characterOpt = characterRepository.findByUserAndDeletedAtIsNull(currentUser);

        if (characterOpt.isEmpty()) {
            return Optional.empty();
        }

        Character character = characterOpt.get();

        return Optional.of(entityDtoMapper.toCharacterResponse(character));
    }

    @Transactional(readOnly = true)
    public CharacterGameStatusResponse getCharacterGameStatus() {
        User currentUser = authService.getCurrentUser();
        Character character = characterRepository.findByUserAndDeletedAtIsNull(currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Character", "user", currentUser.getUserId()));

        boolean isAlive = character.getCharHealth() > 0 && character.getCharSanity() > 0;
        boolean isDying = isAlive && (character.getCharHealth() <= 20 || character.getCharSanity() <= 20);
        String statusMessage = getCharacterStatusMessage(character);

        Optional<Now> activeGame = nowRepository.findByCharacterIdWithPage(character.getCharId());
        boolean hasActiveGame = activeGame.isPresent();

        Long currentStoryId = null;
        String currentStoryTitle = null;
        Long currentPageNumber = null;
        LocalDateTime gameStartTime = null;

        if (hasActiveGame) {
            Now gameSession = activeGame.get();
            Page currentPage = gameSession.getPage();
            currentPageNumber = currentPage.getPageNumber();
            gameStartTime = gameSession.getCreatedAt();

            Story story = storyRepository.findById(currentPage.getStoId()).orElse(null);
            if (story != null) {
                currentStoryId = story.getStoId();
                currentStoryTitle = story.getStoTitle();
            }
        }

        Long totalClears = logERepository.countCompletionsByCharacter(character.getCharId());
        Long totalPlays = logERepository.countTotalPlaysByCharacter(character.getCharId());
        Double clearRate = totalPlays > 0 ? (double) totalClears / totalPlays * 100 : 0.0;

        boolean canEnterNewGame = isAlive && !hasActiveGame && character.getCharHealth() > 0 && character.getCharSanity() > 0;
        String cannotEnterReason = null;

        if (!isAlive) {
            cannotEnterReason = "캐릭터가 사망한 상태입니다.";
        } else if (hasActiveGame) {
            cannotEnterReason = "이미 진행 중인 게임이 있습니다.";
        } else if (character.getCharHealth() <= 0 || character.getCharSanity() <= 0) {
            cannotEnterReason = "캐릭터 상태가 게임을 진행하기에 적합하지 않습니다.";
        }

        return CharacterGameStatusResponse.builder()
                .charId(character.getCharId())
                .charName(character.getCharName())
                .charHealth(character.getCharHealth())
                .charSanity(character.getCharSanity())
                .isAlive(isAlive)
                .isDying(isDying)
                .statusMessage(statusMessage)
                .hasActiveGame(hasActiveGame)
                .currentStoryId(currentStoryId)
                .currentStoryTitle(currentStoryTitle)
                .currentPageNumber(currentPageNumber)
                .gameStartTime(gameStartTime)
                .totalClears(totalClears)
                .totalPlays(totalPlays)
                .clearRate(clearRate)
                .canEnterNewGame(canEnterNewGame)
                .cannotEnterReason(cannotEnterReason)
                .build();
    }

    @Transactional(readOnly = true)
    public List<CharacterResponse> getCharacterHistory() {
        User currentUser = authService.getCurrentUser();

        List<Character> characters = characterRepository.findByUserOrderByCreatedAtDesc(currentUser);

        List<CharacterResponse> responses = characters.stream()
                .map(character -> entityDtoMapper.toCharacterResponse(character))
                .collect(Collectors.toList());

        return responses;
    }

    @Transactional
    public void killCharacter(Long charId) {
        User currentUser = authService.getCurrentUser();

        Character character = characterRepository.findAliveCharacterById(charId)
                .orElseThrow(() -> new ResourceNotFoundException("Character", "id", charId));

        if (!character.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new AccessDeniedException("캐릭터를 삭제할 권한이 없습니다.");
        }

        character.delete();
        characterRepository.save(character);

        cleanupGameProgress(character);
    }

    @Transactional
    public CharacterResponse updateCharacterStats(Long charId, Integer healthChange, Integer sanityChange) {
        Character character = characterRepository.findAliveCharacterById(charId)
                .orElseThrow(() -> new ResourceNotFoundException("Character", "id", charId));

        if (healthChange != null) {
            int newHealth = Math.max(0, character.getCharHealth() + healthChange);
            character.setCharHealth(newHealth);
        }

        if (sanityChange != null) {
            int newSanity = Math.max(0, character.getCharSanity() + sanityChange);
            character.setCharSanity(newSanity);
        }

        checkAndProcessDeath(character);

        Character savedCharacter = characterRepository.save(character);

        CharacterResponse response = entityDtoMapper.toCharacterResponse(savedCharacter);

        return response;
    }

    @Transactional
    public void checkAndProcessDeath(Character character) {
        boolean isDying = character.getCharHealth() <= 0 || character.getCharSanity() <= 0;

        if (isDying) {
            character.delete();
            characterRepository.save(character);
            cleanupGameProgress(character);
        }
    }

    private String getCharacterStatusMessage(Character character) {
        if (character.isDeleted()) {
            return "사망";
        }

        if (character.getCharHealth() <= 0 || character.getCharSanity() <= 0) {
            return "위험 - 즉시 치료 필요";
        }

        if (character.getCharHealth() <= 20 || character.getCharSanity() <= 20) {
            return "주의 - 상태가 좋지 않음";
        }

        if (character.getCharHealth() >= 80 && character.getCharSanity() >= 80) {
            return "건강";
        }

        return "보통";
    }

    private void cleanupGameProgress(Character character) {
        try {
            nowRepository.deleteByCharacter(character);
        } catch (Exception e) {
            log.error("게임 진행 데이터 정리 실패: charId={}, error={}", character.getCharId(), e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public List<VisitedStationResponse> getVisitedStations() {
        User currentUser = authService.getCurrentUser();
        Character character = characterRepository.findByUserAndDeletedAtIsNull(currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Character", "user", currentUser.getUserId()));

        List<Object[]> results = logERepository.findVisitedStationsByCharacter(character.getCharId());

        List<VisitedStationResponse> responses = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        for (Object[] row : results) {
            String stationName = (String) row[0];
            Integer stationLine = (Integer) row[1];
            Long clearCount = (Long) row[2];
            Long totalPlayCount = (Long) row[3];
            LocalDateTime lastVisitedAt = (LocalDateTime) row[4];

            Double clearRate = totalPlayCount > 0 ? (double) clearCount / totalPlayCount * 100 : 0.0;

            String visitBadge = determineVisitBadge(clearCount);

            VisitedStationResponse response = VisitedStationResponse.builder()
                    .stationName(stationName)
                    .stationLine(stationLine)
                    .visitCount(clearCount)
                    .totalPlayCount(totalPlayCount)
                    .clearRate(Math.round(clearRate * 10) / 10.0)
                    .lastVisitedAt(lastVisitedAt.format(formatter))
                    .visitBadge(visitBadge)
                    .build();

            responses.add(response);
        }

        return responses;
    }

    private String determineVisitBadge(Long clearCount) {
        if (clearCount >= 10) {
            return "PLATINUM";
        } else if (clearCount >= 5) {
            return "GOLD";
        } else if (clearCount >= 2) {
            return "SILVER";
        } else {
            return "BRONZE";
        }
    }
}
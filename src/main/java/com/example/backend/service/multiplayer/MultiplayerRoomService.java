package com.example.backend.service.multiplayer;

import com.example.backend.dto.multiplayer.*;
import com.example.backend.entity.*;
import com.example.backend.entity.Station;
import com.example.backend.entity.multiplayer.*;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.*;
import com.example.backend.repository.multiplayer.*;
import com.example.backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MultiplayerRoomService {

    private final MultiplayerRoomRepository roomRepository;
    private final RoomParticipantRepository participantRepository;
    private final StationRepository stationRepository;
    private final CharacterRepository characterRepository;
    private final UserStoryStatsRepository statsRepository;
    private final AuthService authService;

    @Transactional
    public RoomResponse createRoom(RoomCreateRequest request) {
        User currentUser = authService.getCurrentUser();

        Station station = resolveStation(request.getStationId(), request.getStationName(), request.getLineNumber());

        com.example.backend.entity.Character character = characterRepository.findById(request.getCharacterId())
                .orElseThrow(() -> new ResourceNotFoundException("캐릭터를 찾을 수 없습니다"));

        if (!character.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new AccessDeniedException("본인의 캐릭터만 사용할 수 있습니다");
        }

        if (character.isDeleted()) {
            throw new IllegalStateException("사망한 캐릭터는 사용할 수 없습니다");
        }

        List<RoomParticipant> activeParticipations = participantRepository
                .findActiveParticipantsByUserId(currentUser.getUserId());
        if (!activeParticipations.isEmpty()) {
            throw new IllegalStateException("이미 다른 방에 참가 중입니다");
        }

        MultiplayerRoom room = MultiplayerRoom.builder()
                .station(station)
                .roomName(request.getRoomName())
                .maxPlayers(3)
                .currentPhase(0)
                .status(RoomStatus.WAITING)
                .owner(currentUser)
                .build();

        room = roomRepository.save(room);

        RoomParticipant participant = RoomParticipant.builder()
                .room(room)
                .user(currentUser)
                .character(character)
                .isActive(true)
                .hp(character.getCharHealth())
                .sanity(character.getCharSanity())
                .build();

        participantRepository.save(participant);

        // Pessimistic Lock으로 동시성 충돌 방지
        UserStoryStats stats = getOrCreateUserStats(currentUser);
        stats.incrementParticipations();
        statsRepository.save(stats);

        log.info("Room created: {} by user: {}", room.getRoomId(), currentUser.getUserId());

        return toRoomResponse(room);
    }

    @Transactional
    public RoomResponse joinRoom(Long roomId, RoomJoinRequest request) {
        User currentUser = authService.getCurrentUser();

        MultiplayerRoom room = roomRepository.findByIdWithParticipants(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("방을 찾을 수 없습니다"));

        if (room.getStatus() != RoomStatus.WAITING) {
            throw new IllegalStateException("대기 중인 방에만 참가할 수 있습니다");
        }

        if (room.isFull()) {
            throw new IllegalStateException("방이 가득 찼습니다");
        }

        com.example.backend.entity.Character character = characterRepository.findById(request.getCharacterId())
                .orElseThrow(() -> new ResourceNotFoundException("캐릭터를 찾을 수 없습니다"));

        if (!character.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new AccessDeniedException("본인의 캐릭터만 사용할 수 있습니다");
        }

        if (character.isDeleted()) {
            throw new IllegalStateException("사망한 캐릭터는 사용할 수 없습니다");
        }

        boolean alreadyJoined = participantRepository
                .existsByRoomIdAndUserId(roomId, currentUser.getUserId());
        if (alreadyJoined) {
            throw new IllegalStateException("이미 이 방에 참가했습니다");
        }

        List<RoomParticipant> activeParticipations = participantRepository
                .findActiveParticipantsByUserId(currentUser.getUserId());
        if (!activeParticipations.isEmpty()) {
            throw new IllegalStateException("이미 다른 방에 참가 중입니다");
        }

        RoomParticipant participant = RoomParticipant.builder()
                .room(room)
                .user(currentUser)
                .character(character)
                .isActive(true)
                .hp(character.getCharHealth())
                .sanity(character.getCharSanity())
                .build();

        participantRepository.save(participant);

        // Pessimistic Lock으로 동시성 충돌 방지
        UserStoryStats stats = getOrCreateUserStats(currentUser);
        stats.incrementParticipations();
        statsRepository.save(stats);

        log.info("User {} joined room {}", currentUser.getUserId(), roomId);

        return toRoomResponse(room);
    }

    @Transactional
    public void leaveRoom(Long roomId) {
        User currentUser = authService.getCurrentUser();

        RoomParticipant participant = participantRepository
                .findByRoomIdAndUserId(roomId, currentUser.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("방 참가 정보를 찾을 수 없습니다"));

        participant.leave();
        participantRepository.save(participant);

        MultiplayerRoom room = participant.getRoom();
        long activeCount = participantRepository.countActiveParticipantsByRoomId(roomId);

        if (activeCount == 0) {
            room.finish();
            roomRepository.save(room);
            log.info("Room {} finished - all participants left", roomId);
        } else if (room.getOwner().getUserId().equals(currentUser.getUserId())) {
            List<RoomParticipant> activeParticipants = participantRepository
                    .findActiveParticipantsByRoomId(roomId);
            if (!activeParticipants.isEmpty()) {
                room.setOwner(activeParticipants.get(0).getUser());
                roomRepository.save(room);
                log.info("Room {} owner transferred to user {}",
                    roomId, activeParticipants.get(0).getUser().getUserId());
            }
        }

        log.info("User {} left room {}", currentUser.getUserId(), roomId);
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> getRoomsByStation(Long stationId, String stationName, Integer lineNumber) {
        Station station = resolveStation(stationId, stationName, lineNumber);
        List<RoomStatus> activeStatuses = List.of(RoomStatus.WAITING, RoomStatus.PLAYING);
        List<MultiplayerRoom> rooms = roomRepository
                .findByStationIdAndStatusIn(station.getStaId(), activeStatuses);

        return rooms.stream()
                .map(this::toRoomResponse)
                .collect(Collectors.toList());
    }

    private Station resolveStation(Long stationId, String stationName, Integer lineNumber) {
        if (stationId != null) {
            return stationRepository.findById(stationId)
                    .orElseGet(() -> {
                        if (StringUtils.hasText(stationName) && lineNumber != null) {
                            return stationRepository.findByStaNameAndStaLine(stationName, lineNumber)
                                    .orElseThrow(() -> new ResourceNotFoundException("역을 찾을 수 없습니다"));
                        }
                        throw new ResourceNotFoundException("역을 찾을 수 없습니다");
                    });
        }

        if (StringUtils.hasText(stationName) && lineNumber != null) {
            return stationRepository.findByStaNameAndStaLine(stationName, lineNumber)
                    .orElseThrow(() -> new ResourceNotFoundException("역을 찾을 수 없습니다"));
        }

        throw new IllegalArgumentException("역 정보가 필요합니다");
    }

    @Transactional(readOnly = true)
    public RoomResponse getRoomDetail(Long roomId) {
        MultiplayerRoom room = roomRepository.findByIdWithParticipants(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("방을 찾을 수 없습니다"));

        return toRoomResponse(room);
    }

    private RoomResponse toRoomResponse(MultiplayerRoom room) {
        List<RoomParticipant> activeParticipants = participantRepository
                .findActiveParticipantsWithCharacter(room.getRoomId());

        List<ParticipantResponse> participantResponses = activeParticipants.stream()
                .map(p -> ParticipantResponse.builder()
                        .participantId(p.getParticipantId())
                        .userId(p.getUser().getUserId())
                        .characterId(p.getCharacter().getCharId())
                        .characterName(p.getCharacter().getCharName())
                        .hp(p.getHp())
                        .sanity(p.getSanity())
                        .isActive(p.getIsActive())
                        .joinedAt(p.getJoinedAt())
                        .build())
                .collect(Collectors.toList());

        String ownerCharacterName = activeParticipants.stream()
                .filter(p -> p.getUser().getUserId().equals(room.getOwner().getUserId()))
                .findFirst()
                .map(p -> p.getCharacter().getCharName())
                .orElse(null);

        return RoomResponse.builder()
                .roomId(room.getRoomId())
                .roomName(room.getRoomName())
                .stationId(room.getStation().getStaId())
                .stationName(room.getStation().getStaName())
                .currentPlayers(room.getCurrentPlayerCount())
                .maxPlayers(room.getMaxPlayers())
                .currentPhase(room.getCurrentPhase())
                .status(room.getStatus().name())
                .ownerId(room.getOwner().getUserId())
                .ownerCharacterName(ownerCharacterName)
                .participants(participantResponses)
                .createdAt(room.getCreatedAt())
                .build();
    }

    /**
     * UserStoryStats를 조회하거나 생성합니다.
     * 동시성 충돌 시 재시도합니다 (최대 3회).
     */
    private UserStoryStats getOrCreateUserStats(User user) {
        int maxRetries = 3;
        int attempt = 0;

        while (attempt < maxRetries) {
            try {
                return statsRepository.findByUserIdForUpdate(user.getUserId())
                        .orElseGet(() -> {
                            UserStoryStats newStats = UserStoryStats.builder()
                                    .userId(user.getUserId())
                                    .user(user)
                                    .build();
                            return statsRepository.save(newStats);
                        });
            } catch (org.springframework.orm.ObjectOptimisticLockingFailureException |
                     org.springframework.dao.DataIntegrityViolationException e) {
                attempt++;
                if (attempt >= maxRetries) {
                    log.error("Failed to get or create UserStoryStats after {} attempts for user {}",
                            maxRetries, user.getUserId(), e);
                    throw e;
                }
                log.warn("Concurrent modification detected for UserStoryStats (user: {}), retrying... (attempt {}/{})",
                        user.getUserId(), attempt, maxRetries);
                try {
                    Thread.sleep(50 * attempt); // Exponential backoff: 50ms, 100ms, 150ms
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Thread interrupted during retry", ie);
                }
            }
        }
        throw new IllegalStateException("Unreachable code");
    }
}

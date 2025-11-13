package com.example.backend.service.multiplayer;

import com.example.backend.dto.multiplayer.*;
import com.example.backend.entity.*;
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

        Station station = stationRepository.findById(request.getStationId())
                .orElseThrow(() -> new ResourceNotFoundException("역을 찾을 수 없습니다"));

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

        UserStoryStats stats = statsRepository.findByUserId(currentUser.getUserId())
                .orElse(UserStoryStats.builder()
                        .userId(currentUser.getUserId())
                        .user(currentUser)
                        .build());
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

        UserStoryStats stats = statsRepository.findByUserId(currentUser.getUserId())
                .orElse(UserStoryStats.builder()
                        .userId(currentUser.getUserId())
                        .user(currentUser)
                        .build());
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
    public List<RoomResponse> getRoomsByStation(Long stationId) {
        List<RoomStatus> activeStatuses = List.of(RoomStatus.WAITING, RoomStatus.PLAYING);
        List<MultiplayerRoom> rooms = roomRepository
                .findByStationIdAndStatusIn(stationId, activeStatuses);

        return rooms.stream()
                .map(this::toRoomResponse)
                .collect(Collectors.toList());
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
}

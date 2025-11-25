package com.example.backend.service.multiplayer;

import com.example.backend.dto.multiplayer.*;
import com.example.backend.entity.multiplayer.*;
import com.example.backend.repository.multiplayer.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmIntegrationService {

    @Qualifier("llmWebClient")
    private final WebClient llmWebClient;

    private final MultiplayerRoomRepository roomRepository;
    private final RoomParticipantRepository participantRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final MultiplayerStoryStateRepository storyStateRepository;
    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${ai.server.timeout:60000}")
    private int aiServerTimeout;

    @Async
    @Transactional
    public CompletableFuture<Void> generateNextPhase(Long roomId) {
        try {
            log.info("=== 멀티플레이어 LLM 생성 시작: Room {} ===", roomId);

            MultiplayerRoom room = roomRepository.findByIdWithParticipants(roomId)
                    .orElseThrow(() -> new IllegalArgumentException("방을 찾을 수 없습니다"));

            if (room.getIsLlmProcessing()) {
                log.warn("이미 LLM 처리 중: Room {}", roomId);
                return CompletableFuture.completedFuture(null);
            }

            room.setIsLlmProcessing(true);
            roomRepository.save(room);

            List<RoomParticipant> activeParticipants = participantRepository
                    .findActiveParticipantsByRoomId(roomId);

            if (activeParticipants.isEmpty()) {
                log.warn("활성 참여자가 없습니다: Room {}", roomId);
                room.setIsLlmProcessing(false);
                roomRepository.save(room);
                return CompletableFuture.completedFuture(null);
            }

            sendAiThinkingMessage(roomId);

            boolean isIntro = room.getCurrentPhase() == 0;
            List<ChatMessage> messageStack = isIntro ? Collections.emptyList() :
                getMessageStack(roomId, room.getCurrentPhase());

            LlmStoryRequest request = buildLlmRequest(room, activeParticipants, messageStack, isIntro);

            return callLlmServer(request)
                    .toFuture()
                    .thenAccept(response -> {
                        handleLlmResponse(room, activeParticipants, response);
                    })
                    .exceptionally(e -> {
                        log.error("LLM 생성 실패: Room {}", roomId, e);
                        handleLlmFailure(room);
                        return null;
                    });

        } catch (Exception e) {
            log.error("generateNextPhase 실패: Room {}", roomId, e);
            return CompletableFuture.failedFuture(e);
        }
    }

    private List<ChatMessage> getMessageStack(Long roomId, Integer currentPhase) {
        List<ChatMessage> messages = chatMessageRepository
                .findByRoomIdAndMessageTypeAndPhaseOrderByCreatedAtAsc(
                    roomId,
                    MessageType.USER,
                    currentPhase
                );

        if (messages.size() > 20) {
            messages = messages.subList(messages.size() - 20, messages.size());
        }

        log.info("대화 스택 조회: Room {} Phase {} - {}개 메시지",
            roomId, currentPhase, messages.size());

        return messages;
    }

    private LlmStoryRequest buildLlmRequest(MultiplayerRoom room,
                                            List<RoomParticipant> participants,
                                            List<ChatMessage> messages,
                                            boolean isIntro) {
        List<MessageContext> messageStack = messages.stream()
                .map(msg -> MessageContext.builder()
                        .username(msg.getUser().getUsername())
                        .content(msg.getContent())
                        .build())
                .collect(Collectors.toList());

        List<ParticipantContext> participantContexts = participants.stream()
                .map(p -> ParticipantContext.builder()
                        .characterName(p.getCharacter().getCharName())
                        .hp(p.getHp())
                        .sanity(p.getSanity())
                        .build())
                .collect(Collectors.toList());

        return LlmStoryRequest.builder()
                .roomId(room.getRoomId())
                .phase(room.getCurrentPhase())
                .stationName(room.getStation().getStaName())
                .storyOutline(room.getStoryOutline())
                .participants(participantContexts)
                .messageStack(messageStack)
                .isIntro(isIntro)
                .build();
    }

    private Mono<LlmStoryResponse> callLlmServer(LlmStoryRequest request) {
        log.info("LLM Server 호출: Room {} Phase {} isIntro={}",
            request.getRoomId(), request.getPhase(), request.getIsIntro());

        return llmWebClient.post()
                .uri("/api/multiplayer/generate-story")
                .header("X-Internal-API-Key", "behindy-internal-2025-secret-key")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(LlmStoryResponse.class)
                .timeout(Duration.ofMillis(aiServerTimeout))
                .doOnSuccess(response -> log.info("LLM Server 응답 성공: Phase {}", response.getPhase()))
                .doOnError(e -> log.error("LLM Server 호출 실패", e));
    }

    @Transactional
    protected void handleLlmResponse(MultiplayerRoom room,
                                     List<RoomParticipant> participants,
                                     LlmStoryResponse response) {
        try {
            saveStoryState(room, response);
            updateParticipantStates(participants, response.getEffects());

            if (response.getStoryOutline() != null && !response.getStoryOutline().isEmpty()) {
                room.setStoryOutline(response.getStoryOutline());
            }

            room.setCurrentPhase(response.getPhase());

            if (response.getIsEnding()) {
                room.finish();
                log.info("스토리 종료: Room {}", room.getRoomId());
            }

            room.setIsLlmProcessing(false);
            roomRepository.save(room);

            broadcastLlmResponse(room.getRoomId(), response);
            broadcastParticipantUpdates(room.getRoomId(), participants);

        } catch (Exception e) {
            log.error("LLM 응답 처리 실패: Room {}", room.getRoomId(), e);
            handleLlmFailure(room);
        }
    }

    @Transactional
    protected void handleLlmFailure(MultiplayerRoom room) {
        try {
            room.setIsLlmProcessing(false);
            roomRepository.save(room);
            broadcastErrorMessage(room.getRoomId(), "AI 응답 생성에 실패했습니다. 다시 시도해주세요.");
        } catch (Exception e) {
            log.error("LLM 실패 처리 중 에러: Room {}", room.getRoomId(), e);
        }
    }

    @Transactional
    protected void saveStoryState(MultiplayerRoom room, LlmStoryResponse response) {
        MultiplayerStoryState state = MultiplayerStoryState.builder()
                .room(room)
                .phase(response.getPhase())
                .llmResponse(response.getStoryText())
                .summary(room.getStoryOutline())
                .context(new HashMap<>())
                .build();

        storyStateRepository.save(state);
        log.info("스토리 상태 저장 완료: Room {} Phase {}", room.getRoomId(), response.getPhase());
    }

    @Transactional
    protected void updateParticipantStates(List<RoomParticipant> participants,
                                           List<CharacterEffect> effects) {
        if (effects == null || effects.isEmpty()) {
            log.info("HP/Sanity 변화 없음");
            return;
        }

        Map<String, CharacterEffect> effectMap = effects.stream()
                .collect(Collectors.toMap(
                        CharacterEffect::getCharacterName,
                        e -> e,
                        (e1, e2) -> e1
                ));

        for (RoomParticipant participant : participants) {
            String characterName = participant.getCharacter().getCharName();
            CharacterEffect effect = effectMap.get(characterName);
            if (effect != null) {
                if (effect.getHpChange() != 0) {
                    int newHp = Math.max(0, Math.min(100, participant.getHp() + effect.getHpChange()));
                    participant.setHp(newHp);
                }
                if (effect.getSanityChange() != 0) {
                    int newSanity = Math.max(0, Math.min(100, participant.getSanity() + effect.getSanityChange()));
                    participant.setSanity(newSanity);
                }

                participantRepository.save(participant);

                log.info("참여자 상태 업데이트: {} HP:{}{} Sanity:{}{}",
                        characterName,
                        participant.getHp(),
                        effect.getHpChange() > 0 ? "+" + effect.getHpChange() : effect.getHpChange(),
                        participant.getSanity(),
                        effect.getSanityChange() > 0 ? "+" + effect.getSanityChange() : effect.getSanityChange()
                );

                if (participant.getHp() <= 0) {
                    handleParticipantDeath(participant);
                }
            }
        }
    }

    @Transactional
    protected void handleParticipantDeath(RoomParticipant participant) {
        participant.leave();
        participantRepository.save(participant);

        String deathMessage = String.format("%s님이 사망하여 방에서 퇴장합니다.",
            participant.getCharacter().getCharName());
        broadcastSystemMessage(participant.getRoom().getRoomId(), deathMessage);

        log.info("참여자 사망 처리: Room {} Character {}",
            participant.getRoom().getRoomId(),
            participant.getCharacter().getCharName());
    }

    private void sendAiThinkingMessage(Long roomId) {
        ChatMessageResponse thinkingMsg = chatMessageService.sendSystemMessage(
                roomId,
                "AI가 생각 중...",
                Map.of("type", "llm_thinking")
        );
        messagingTemplate.convertAndSend("/topic/room/" + roomId, thinkingMsg);
    }

    private void broadcastLlmResponse(Long roomId, LlmStoryResponse response) {
        ChatMessageResponse storyMessage = chatMessageService.sendLlmMessage(
                roomId,
                response.getStoryText(),
                response.getPhase()
        );

        messagingTemplate.convertAndSend("/topic/room/" + roomId, storyMessage);
        log.info("LLM 응답 브로드캐스트 완료: Room {} Phase {}", roomId, response.getPhase());
    }

    private void broadcastParticipantUpdates(Long roomId, List<RoomParticipant> participants) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("type", "participant_update");
        updates.put("participants", participants.stream()
            .map(p -> Map.of(
                "participantId", p.getParticipantId(),
                "characterName", p.getCharacter().getCharName(),
                "hp", p.getHp(),
                "sanity", p.getSanity(),
                "isActive", p.isActive()
            ))
            .collect(Collectors.toList()));

        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/participants", updates);
    }

    private void broadcastSystemMessage(Long roomId, String message) {
        ChatMessageResponse systemMsg = chatMessageService.sendSystemMessage(
                roomId,
                message,
                Map.of("type", "info")
        );
        messagingTemplate.convertAndSend("/topic/room/" + roomId, systemMsg);
    }

    private void broadcastErrorMessage(Long roomId, String errorMessage) {
        try {
            ChatMessageResponse errorMsg = chatMessageService.sendSystemMessage(
                    roomId,
                    errorMessage,
                    Map.of("type", "error")
            );
            messagingTemplate.convertAndSend("/topic/room/" + roomId, errorMsg);
        } catch (Exception e) {
            log.error("에러 메시지 브로드캐스트 실패: Room {}", roomId, e);
        }
    }
}

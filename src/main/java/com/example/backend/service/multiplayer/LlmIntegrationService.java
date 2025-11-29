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
                return CompletableFuture.completedFuture(null);
            }

            room.setIsLlmProcessing(true);
            roomRepository.save(room);

            List<RoomParticipant> activeParticipants = participantRepository
                    .findActiveParticipantsByRoomId(roomId);

            if (activeParticipants.isEmpty()) {
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

        return messages;
    }

    private List<StoryHistoryItem> getStoryHistory(Long roomId, Integer currentPhase) {
        int startPhase = Math.max(1, currentPhase - 4);

        List<MultiplayerStoryState> storyStates = storyStateRepository
                .findByRoom_RoomIdAndPhaseBetweenOrderByPhaseAsc(roomId, startPhase, currentPhase - 1);

        return storyStates.stream()
                .filter(state -> state.getSummary() != null && !state.getSummary().isEmpty())
                .map(state -> StoryHistoryItem.builder()
                        .phase(state.getPhase())
                        .summary(state.getSummary())
                        .build())
                .collect(Collectors.toList());
    }

    private LlmStoryRequest buildLlmRequest(MultiplayerRoom room,
                                            List<RoomParticipant> participants,
                                            List<ChatMessage> messages,
                                            boolean isIntro) {
        Map<Long, String> userIdToCharacterName = participants.stream()
                .collect(Collectors.toMap(
                        p -> p.getUser().getUserId(),
                        p -> p.getCharacter().getCharName()
                ));

        List<MessageContext> messageStack = messages.stream()
                .map(msg -> {
                    String characterName = userIdToCharacterName.get(msg.getUser().getUserId());
                    return MessageContext.builder()
                            .characterName(characterName != null ? characterName : msg.getUser().getUserName())
                            .content(msg.getContent())
                            .build();
                })
                .collect(Collectors.toList());

        List<ParticipantContext> participantContexts = participants.stream()
                .map(p -> ParticipantContext.builder()
                        .characterName(p.getCharacter().getCharName())
                        .hp(p.getHp())
                        .sanity(p.getSanity())
                        .build())
                .collect(Collectors.toList());

        List<StoryHistoryItem> storyHistory = isIntro ? Collections.emptyList() :
                getStoryHistory(room.getRoomId(), room.getCurrentPhase());

        return LlmStoryRequest.builder()
                .roomId(room.getRoomId())
                .phase(room.getCurrentPhase())
                .stationName(room.getStation().getStaName())
                .storyOutline(room.getStoryOutline())
                .participants(participantContexts)
                .messageStack(messageStack)
                .storyHistory(storyHistory)
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

                if (response.getEndingSummary() != null) {
                    broadcastEndingMessage(room.getRoomId(), response.getEndingSummary());
                }
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
        String combinedStory = formatStoryContent(response.getStory());

        Map<String, Object> context = new HashMap<>();
        if (response.getIsEnding() && response.getEndingSummary() != null) {
            context.put("ending_summary", response.getEndingSummary());
        }

        MultiplayerStoryState state = MultiplayerStoryState.builder()
                .room(room)
                .phase(response.getPhase())
                .llmResponse(combinedStory)
                .summary(response.getPhaseSummary())
                .context(context)
                .build();

        storyStateRepository.save(state);
    }

    private String formatStoryContent(LlmStoryResponse.StoryContent story) {
        if (story == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        if (story.getCurrentSituation() != null) {
            sb.append(story.getCurrentSituation()).append("\n\n");
        }
        if (story.getSpecialEvent() != null) {
            sb.append(story.getSpecialEvent()).append("\n\n");
        }
        if (story.getHint() != null) {
            sb.append(story.getHint());
        }
        return sb.toString().trim();
    }

    @Transactional
    protected void updateParticipantStates(List<RoomParticipant> participants,
                                           List<CharacterEffect> effects) {
        if (effects == null || effects.isEmpty()) {
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
                boolean hasHpChange = effect.getHpChange() != null && effect.getHpChange() != 0;
                boolean hasSanityChange = effect.getSanityChange() != null && effect.getSanityChange() != 0;

                if (hasHpChange) {
                    int newHp = Math.max(0, Math.min(100, participant.getHp() + effect.getHpChange()));
                    participant.setHp(newHp);
                }
                if (hasSanityChange) {
                    int newSanity = Math.max(0, Math.min(100, participant.getSanity() + effect.getSanityChange()));
                    participant.setSanity(newSanity);
                }

                if (hasHpChange || hasSanityChange) {
                    participantRepository.save(participant);

                    sendStatChangeMessages(participant.getRoom().getRoomId(), characterName, effect);
                }

                if (participant.getHp() <= 0 || participant.getSanity() <= 0) {
                    handleParticipantDeath(participant);
                }
            }
        }
    }

    private void sendStatChangeMessages(Long roomId, String characterName, CharacterEffect effect) {
        if (effect.getHpChange() != null && effect.getHpChange() != 0) {
            String sign = effect.getHpChange() > 0 ? "+" : "";
            String message = String.format("%s에게 %s%d의 체력 영향", characterName, sign, effect.getHpChange());
            ChatMessageResponse sysMsg = chatMessageService.sendSystemMessage(
                    roomId, message, Map.of("type", "stat_change")
            );
            messagingTemplate.convertAndSend("/topic/room/" + roomId, sysMsg);
        }

        if (effect.getSanityChange() != null && effect.getSanityChange() != 0) {
            String sign = effect.getSanityChange() > 0 ? "+" : "";
            String message = String.format("%s에게 %s%d의 정신력 영향", characterName, sign, effect.getSanityChange());
            ChatMessageResponse sysMsg = chatMessageService.sendSystemMessage(
                    roomId, message, Map.of("type", "stat_change")
            );
            messagingTemplate.convertAndSend("/topic/room/" + roomId, sysMsg);
        }
    }

    @Transactional
    protected void handleParticipantDeath(RoomParticipant participant) {
        participant.leave();
        participantRepository.save(participant);

        String deathMessage = String.format("%s님이 사망하여 방에서 퇴장합니다.",
            participant.getCharacter().getCharName());
        broadcastSystemMessage(participant.getRoom().getRoomId(), deathMessage);
    }

    private void sendAiThinkingMessage(Long roomId) {
        ChatMessageResponse thinkingMsg = chatMessageService.sendSystemMessage(
                roomId,
                "어둠 속에서 웅성거리는 소리가 들립니다...",
                Map.of("type", "llm_thinking")
        );
        messagingTemplate.convertAndSend("/topic/room/" + roomId, thinkingMsg);
    }

    private void broadcastLlmResponse(Long roomId, LlmStoryResponse response) {
        String combinedStory = formatStoryContent(response.getStory());

        ChatMessageResponse storyMessage = chatMessageService.sendLlmMessage(
                roomId,
                combinedStory,
                response.getPhase()
        );

        messagingTemplate.convertAndSend("/topic/room/" + roomId, storyMessage);
    }

    private void broadcastParticipantUpdates(Long roomId, List<RoomParticipant> participants) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("type", "participant_update");
        updates.put("participants", participants.stream()
            .map(p -> {
                Map<String, Object> participantMap = new HashMap<>();
                participantMap.put("participantId", p.getParticipantId());
                participantMap.put("userId", p.getUser().getUserId());
                participantMap.put("characterName", p.getCharacter().getCharName());
                participantMap.put("hp", p.getHp());
                participantMap.put("sanity", p.getSanity());
                participantMap.put("isActive", p.isActive());
                return participantMap;
            })
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

    private void broadcastEndingMessage(Long roomId, String endingSummary) {
        try {
            ChatMessageResponse endingMsg = chatMessageService.sendSystemMessage(
                    roomId,
                    endingSummary,
                    Map.of("type", "ending", "ending_summary", endingSummary)
            );
            messagingTemplate.convertAndSend("/topic/room/" + roomId, endingMsg);
        } catch (Exception e) {
            log.error("엔딩 메시지 브로드캐스트 실패: Room {}", roomId, e);
        }
    }
}

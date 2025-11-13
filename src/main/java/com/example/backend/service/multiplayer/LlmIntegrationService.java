package com.example.backend.service.multiplayer;

import com.example.backend.dto.multiplayer.ChatMessageResponse;
import com.example.backend.entity.multiplayer.ChatMessage;
import com.example.backend.entity.multiplayer.MultiplayerRoom;
import com.example.backend.entity.multiplayer.MultiplayerStoryState;
import com.example.backend.entity.multiplayer.RoomParticipant;
import com.example.backend.repository.multiplayer.ChatMessageRepository;
import com.example.backend.repository.multiplayer.MultiplayerRoomRepository;
import com.example.backend.repository.multiplayer.MultiplayerStoryStateRepository;
import com.example.backend.repository.multiplayer.RoomParticipantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.data.domain.PageRequest;
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

    @Value("${ai.server.timeout:30000}")
    private int aiServerTimeout;

    @Async
    @Transactional
    public CompletableFuture<Void> generateNextPhase(Long roomId) {
        try {
            log.info("=== 멀티플레이어 LLM 생성 시작: Room {} ===", roomId);

            MultiplayerRoom room = roomRepository.findByIdWithParticipants(roomId)
                    .orElseThrow(() -> new IllegalArgumentException("방을 찾을 수 없습니다"));

            List<RoomParticipant> activeParticipants = participantRepository
                    .findActiveParticipantsByRoomId(roomId);

            if (activeParticipants.isEmpty()) {
                log.warn("활성 참여자가 없습니다: Room {}", roomId);
                return CompletableFuture.completedFuture(null);
            }

            List<ChatMessage> recentMessages = chatMessageRepository
                    .findByRoomIdOrderByCreatedAtDesc(roomId, PageRequest.of(0, 100));

            MultiplayerStoryRequest request = buildLlmRequest(room, activeParticipants, recentMessages);

            return callLlmServer(request)
                    .toFuture()
                    .thenAccept(response -> {
                        saveStoryState(room, response);
                        updateParticipantStates(activeParticipants, response.getParticipantUpdates());
                        broadcastLlmResponse(roomId, response);
                    })
                    .exceptionally(e -> {
                        log.error("LLM 생성 실패: Room {}", roomId, e);
                        broadcastErrorMessage(roomId, "AI 응답 생성에 실패했습니다");
                        return null;
                    });

        } catch (Exception e) {
            log.error("generateNextPhase 실패: Room {}", roomId, e);
            return CompletableFuture.failedFuture(e);
        }
    }

    private MultiplayerStoryRequest buildLlmRequest(MultiplayerRoom room,
                                                     List<RoomParticipant> participants,
                                                     List<ChatMessage> messages) {
        MultiplayerStoryRequest request = new MultiplayerStoryRequest();
        request.setStationId(room.getStation().getStaId().intValue());
        request.setStationName(room.getStation().getStaName());
        request.setLineNumber(room.getStation().getStaLine());
        request.setCurrentPhase(room.getCurrentPhase());

        MultiplayerStoryState latestState = storyStateRepository
                .findLatestByRoomId(room.getRoomId())
                .orElse(null);

        if (latestState != null) {
            request.setSummary(latestState.getSummary());
        }

        List<ChatHistoryItem> historyItems = messages.stream()
                .map(msg -> {
                    ChatHistoryItem item = new ChatHistoryItem();
                    String characterName = msg.getMetadata() != null ?
                            (String) msg.getMetadata().get("characterName") : "Unknown";
                    item.setCharacterName(characterName);
                    item.setContent(msg.getContent());
                    return item;
                })
                .collect(Collectors.toList());

        request.setRecentMessages(historyItems);

        List<ParticipantInfo> participantInfos = participants.stream()
                .map(p -> {
                    ParticipantInfo info = new ParticipantInfo();
                    info.setCharacterName(p.getCharacter().getCharName());
                    info.setHp(p.getHp());
                    info.setSanity(p.getSanity());
                    return info;
                })
                .collect(Collectors.toList());

        request.setParticipants(participantInfos);

        return request;
    }

    private Mono<MultiplayerStoryResponse> callLlmServer(MultiplayerStoryRequest request) {
        log.info("LLM Server 호출: {} Phase {}", request.getStationName(), request.getCurrentPhase());

        return llmWebClient.post()
                .uri("/llm/multiplayer/next-phase")
                .header("X-Internal-API-Key", "behindy-internal-2025-secret-key")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(MultiplayerStoryResponse.class)
                .timeout(Duration.ofMillis(aiServerTimeout))
                .doOnSuccess(response -> log.info("LLM Server 응답 성공"))
                .doOnError(e -> log.error("LLM Server 호출 실패", e));
    }

    @Transactional
    protected void saveStoryState(MultiplayerRoom room, MultiplayerStoryResponse response) {
        MultiplayerStoryState state = MultiplayerStoryState.builder()
                .room(room)
                .phase(room.getCurrentPhase())
                .llmResponse(response.getContent())
                .summary(response.getSummary())
                .context(new HashMap<>())
                .build();

        storyStateRepository.save(state);
        log.info("스토리 상태 저장 완료: Room {} Phase {}", room.getRoomId(), room.getCurrentPhase());
    }

    @Transactional
    protected void updateParticipantStates(List<RoomParticipant> participants,
                                           List<ParticipantUpdate> updates) {
        Map<String, ParticipantUpdate> updateMap = updates.stream()
                .collect(Collectors.toMap(
                        ParticipantUpdate::getCharacterName,
                        u -> u,
                        (u1, u2) -> u1
                ));

        for (RoomParticipant participant : participants) {
            String characterName = participant.getCharacter().getCharName();
            ParticipantUpdate update = updateMap.get(characterName);
            if (update != null) {
                if (update.getHpChange() != 0) {
                    int newHp = Math.max(0, Math.min(100, participant.getHp() + update.getHpChange()));
                    participant.setHp(newHp);
                }
                if (update.getSanityChange() != 0) {
                    int newSanity = Math.max(0, Math.min(100, participant.getSanity() + update.getSanityChange()));
                    participant.setSanity(newSanity);
                }

                participantRepository.save(participant);

                log.info("참여자 상태 업데이트: {} HP:{}{} Sanity:{}{}",
                        characterName,
                        participant.getHp(),
                        update.getHpChange() > 0 ? "+" + update.getHpChange() : update.getHpChange(),
                        participant.getSanity(),
                        update.getSanityChange() > 0 ? "+" + update.getSanityChange() : update.getSanityChange()
                );
            }
        }
    }

    private void broadcastLlmResponse(Long roomId, MultiplayerStoryResponse response) {
        MultiplayerRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("방을 찾을 수 없습니다"));

        ChatMessageResponse phaseMessage = chatMessageService.sendPhaseMessage(
                roomId,
                response.getContent(),
                room.getCurrentPhase()
        );

        messagingTemplate.convertAndSend("/topic/room/" + roomId, phaseMessage);
        log.info("LLM 응답 브로드캐스트 완료: Room {}", roomId);
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

    public static class MultiplayerStoryRequest {
        private int stationId;
        private String stationName;
        private int lineNumber;
        private int currentPhase;
        private String summary;
        private List<ChatHistoryItem> recentMessages;
        private List<ParticipantInfo> participants;

        public int getStationId() { return stationId; }
        public void setStationId(int stationId) { this.stationId = stationId; }

        public String getStationName() { return stationName; }
        public void setStationName(String stationName) { this.stationName = stationName; }

        public int getLineNumber() { return lineNumber; }
        public void setLineNumber(int lineNumber) { this.lineNumber = lineNumber; }

        public int getCurrentPhase() { return currentPhase; }
        public void setCurrentPhase(int currentPhase) { this.currentPhase = currentPhase; }

        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }

        public List<ChatHistoryItem> getRecentMessages() { return recentMessages; }
        public void setRecentMessages(List<ChatHistoryItem> recentMessages) { this.recentMessages = recentMessages; }

        public List<ParticipantInfo> getParticipants() { return participants; }
        public void setParticipants(List<ParticipantInfo> participants) { this.participants = participants; }
    }

    public static class ChatHistoryItem {
        private String characterName;
        private String content;

        public String getCharacterName() { return characterName; }
        public void setCharacterName(String characterName) { this.characterName = characterName; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }

    public static class ParticipantInfo {
        private String characterName;
        private int hp;
        private int sanity;

        public String getCharacterName() { return characterName; }
        public void setCharacterName(String characterName) { this.characterName = characterName; }

        public int getHp() { return hp; }
        public void setHp(int hp) { this.hp = hp; }

        public int getSanity() { return sanity; }
        public void setSanity(int sanity) { this.sanity = sanity; }
    }

    public static class MultiplayerStoryResponse {
        private String content;
        private String summary;
        private List<ParticipantUpdate> participantUpdates;

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }

        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }

        public List<ParticipantUpdate> getParticipantUpdates() { return participantUpdates; }
        public void setParticipantUpdates(List<ParticipantUpdate> participantUpdates) { this.participantUpdates = participantUpdates; }
    }

    public static class ParticipantUpdate {
        private String characterName;
        private int hpChange;
        private int sanityChange;

        public String getCharacterName() { return characterName; }
        public void setCharacterName(String characterName) { this.characterName = characterName; }

        public int getHpChange() { return hpChange; }
        public void setHpChange(int hpChange) { this.hpChange = hpChange; }

        public int getSanityChange() { return sanityChange; }
        public void setSanityChange(int sanityChange) { this.sanityChange = sanityChange; }
    }
}

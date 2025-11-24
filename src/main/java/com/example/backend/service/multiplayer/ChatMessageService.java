package com.example.backend.service.multiplayer;

import com.example.backend.dto.multiplayer.ChatMessageRequest;
import com.example.backend.dto.multiplayer.ChatMessageResponse;
import com.example.backend.dto.multiplayer.RoomVoteResponse;
import com.example.backend.entity.User;
import com.example.backend.entity.multiplayer.*;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.multiplayer.*;
import com.example.backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageRepository messageRepository;
    private final MultiplayerRoomRepository roomRepository;
    private final RoomParticipantRepository participantRepository;
    private final MessageSanitizer messageSanitizer;
    private final RateLimiter rateLimiter;
    private final AuthService authService;

    @Transactional
    public ChatMessageResponse sendMessage(Long roomId, ChatMessageRequest request) {
        User currentUser = authService.getCurrentUser();

        if (!rateLimiter.allowMessage(currentUser.getUserId())) {
            long cooldown = rateLimiter.getRemainingCooldown(currentUser.getUserId());
            throw new IllegalStateException(cooldown + "ms 후에 다시 시도해주세요");
        }

        MultiplayerRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("방을 찾을 수 없습니다"));

        boolean isParticipant = participantRepository
                .existsByRoomIdAndUserId(roomId, currentUser.getUserId());
        if (!isParticipant) {
            throw new IllegalStateException("방 참가자만 메시지를 보낼 수 있습니다");
        }

        String sanitizedContent = messageSanitizer.sanitize(request.getContent());

        ChatMessage message = ChatMessage.builder()
                .room(room)
                .user(currentUser)
                .messageType(MessageType.USER)
                .content(sanitizedContent)
                .build();

        message = messageRepository.save(message);

        log.debug("User {} sent message in room {}", currentUser.getUserId(), roomId);

        return toMessageResponse(message);
    }

    @Transactional
    public ChatMessageResponse sendSystemMessage(Long roomId, String content, Map<String, Object> metadata) {
        MultiplayerRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("방을 찾을 수 없습니다"));

        ChatMessage message = ChatMessage.builder()
                .room(room)
                .user(null)
                .messageType(MessageType.SYSTEM)
                .content(content)
                .metadata(metadata)
                .build();

        message = messageRepository.save(message);

        log.debug("System message sent in room {}: {}", roomId, content);

        return toMessageResponse(message);
    }

    @Transactional
    public ChatMessageResponse sendLlmMessage(Long roomId, String content, Integer phase) {
        MultiplayerRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("방을 찾을 수 없습니다"));

        ChatMessage message = ChatMessage.builder()
                .room(room)
                .user(null)
                .messageType(MessageType.LLM)
                .content(content)
                .metadata(Map.of("phase", phase))
                .build();

        message = messageRepository.save(message);

        log.info("LLM message sent in room {} for phase {}", roomId, phase);

        return toMessageResponse(message);
    }

    @Transactional
    public ChatMessageResponse sendPhaseMessage(Long roomId, String content, Integer phase) {
        MultiplayerRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("방을 찾을 수 없습니다"));

        ChatMessage message = ChatMessage.builder()
                .room(room)
                .user(null)
                .messageType(MessageType.PHASE)
                .content(content)
                .metadata(Map.of("phase", phase))
                .build();

        message = messageRepository.save(message);

        log.debug("Phase message sent in room {}: phase {}", roomId, phase);

        return toMessageResponse(message);
    }

    @Transactional
    public ChatMessageResponse sendVoteMessage(Long roomId, RoomVoteResponse voteResponse, String eventType) {
        MultiplayerRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("방을 찾을 수 없습니다"));

        Map<String, Object> metadata = new java.util.HashMap<>();
        metadata.put("type", eventType);
        metadata.put("voteId", voteResponse.getVoteId());
        metadata.put("targetUserId", voteResponse.getTargetUserId());
        metadata.put("targetUsername", voteResponse.getTargetUsername());
        metadata.put("initiatedByUserId", voteResponse.getInitiatedByUserId());
        metadata.put("initiatedByUsername", voteResponse.getInitiatedByUsername());
        metadata.put("status", voteResponse.getStatus());
        metadata.put("yesCount", voteResponse.getYesCount());
        metadata.put("noCount", voteResponse.getNoCount());
        metadata.put("requiredVotes", voteResponse.getRequiredVotes());
        if (voteResponse.getExpiresAt() != null) {
            metadata.put("expiresAt", voteResponse.getExpiresAt().toString());
        }

        ChatMessage message = ChatMessage.builder()
                .room(room)
                .user(null)
                .messageType(MessageType.VOTE)
                .content("투표 상태 업데이트")
                .metadata(metadata)
                .build();

        message = messageRepository.save(message);

        log.debug("Vote message sent in room {}: {}", roomId, metadata);

        return toMessageResponse(message);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getMessages(Long roomId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<ChatMessage> messages = messageRepository
                .findByRoomIdOrderByCreatedAtDesc(roomId, pageable);

        Collections.reverse(messages);

        return messages.stream()
                .map(this::toMessageResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getRecentUserMessages(Long roomId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<MessageType> userTypes = List.of(MessageType.USER);
        List<ChatMessage> messages = messageRepository
                .findRecentUserMessages(roomId, userTypes, pageable);

        return messages.stream()
                .map(this::toMessageResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getMessagesAfter(Long roomId, Long lastMessageId, int limit) {
        if (lastMessageId == null || lastMessageId < 0) {
            return getMessages(roomId, limit);
        }

        Pageable pageable = PageRequest.of(0, limit);
        List<ChatMessage> messages = messageRepository
                .findAfterMessageId(roomId, lastMessageId, pageable);

        return messages.stream()
                .map(this::toMessageResponse)
                .collect(Collectors.toList());
    }

    private ChatMessageResponse toMessageResponse(ChatMessage message) {
        String characterName = null;
        Long userId = null;
        String username = null;

        if (message.getUser() != null) {
            userId = message.getUser().getUserId();
            username = message.getUser().getUserName();
            RoomParticipant participant = participantRepository
                    .findByRoomIdAndUserId(message.getRoom().getRoomId(), userId)
                    .orElse(null);
            if (participant != null && participant.getCharacter() != null) {
                characterName = participant.getCharacter().getCharName();
            }
        }

        return ChatMessageResponse.builder()
                .messageId(message.getMessageId())
                .roomId(message.getRoom().getRoomId())
                .messageType(message.getMessageType().name())
                .content(message.getContent())
                .userId(userId)
                .username(username)
                .characterName(characterName)
                .metadata(message.getMetadata())
                .createdAt(message.getCreatedAt())
                .build();
    }
}

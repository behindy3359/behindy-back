package com.example.backend.controller.multiplayer;

import com.example.backend.dto.multiplayer.ChatMessageRequest;
import com.example.backend.dto.multiplayer.ChatMessageResponse;
import com.example.backend.dto.multiplayer.RoomVoteResponse;
import com.example.backend.service.multiplayer.ChatMessageService;
import com.example.backend.service.multiplayer.LlmIntegrationService;
import com.example.backend.service.multiplayer.VoteService;
import com.example.backend.repository.multiplayer.RoomParticipantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatMessageService chatMessageService;
    private final LlmIntegrationService llmIntegrationService;
    private final VoteService voteService;
    private final SimpMessagingTemplate messagingTemplate;
    private final RoomParticipantRepository participantRepository;

    @MessageMapping("/room/{roomId}/chat")
    public void handleChat(
            @DestinationVariable Long roomId,
            @Payload ChatMessageRequest request,
            SimpMessageHeaderAccessor headerAccessor) {

        try {
            Long userId = getUserIdFromSession(headerAccessor);

            ChatMessageResponse response = chatMessageService.sendMessage(roomId, request, userId);

            messagingTemplate.convertAndSend("/topic/room/" + roomId, response);
        } catch (Exception e) {
            log.error("Error handling chat message in room {}: {}", roomId, e.getMessage(), e);
            sendErrorToUser(headerAccessor, e.getMessage());
        }
    }

    @MessageMapping("/room/{roomId}/action")
    public void handleAction(
            @DestinationVariable Long roomId,
            SimpMessageHeaderAccessor headerAccessor) {

        try {
            Long userId = getUserIdFromSession(headerAccessor);

            Long voteId = voteService.startActionVote(roomId, userId);
            RoomVoteResponse voteState = voteService.getVoteState(voteId);

            if (voteState.getStatus().equals("PASSED")) {
                ChatMessageResponse passedMessage = chatMessageService.sendSystemMessage(
                        roomId,
                        "행동하기 투표가 자동 승인되었습니다",
                        Map.of(
                            "type", "vote_auto_passed",
                            "voteId", voteId
                        )
                );
                messagingTemplate.convertAndSend("/topic/room/" + roomId, passedMessage);

                llmIntegrationService.generateNextPhase(roomId)
                        .thenAccept(llmResult -> log.info("LLM 응답 처리 완료: Room {}", roomId))
                        .exceptionally(ex -> {
                            log.error("LLM 응답 처리 실패: Room {}", roomId, ex);
                            sendErrorToRoom(roomId, "AI 응답 생성에 실패했습니다");
                            return null;
                        });
            } else {
                ChatMessageResponse voteMessage = chatMessageService.sendSystemMessage(
                        roomId,
                        "행동하기 투표가 시작되었습니다",
                        Map.of(
                            "type", "vote_start",
                            "voteId", voteId
                        )
                );
                messagingTemplate.convertAndSend("/topic/room/" + roomId, voteMessage);
            }
        } catch (Exception e) {
            log.error("Error handling action in room {}: {}", roomId, e.getMessage());
            sendErrorToUser(headerAccessor, e.getMessage());
        }
    }

    @MessageMapping("/room/{roomId}/vote/kick")
    public void startKickVote(
            @DestinationVariable Long roomId,
            @Payload Map<String, Long> payload,
            SimpMessageHeaderAccessor headerAccessor) {

        try {
            Long userId = getUserIdFromSession(headerAccessor);
            Long targetUserId = payload.get("targetUserId");

            Long voteId = voteService.startKickVote(roomId, targetUserId, userId);

            ChatMessageResponse voteMessage = chatMessageService.sendSystemMessage(
                    roomId,
                    "추방 투표가 시작되었습니다",
                    Map.of(
                        "type", "vote_start",
                        "voteId", voteId,
                        "targetUserId", targetUserId
                    )
            );

            messagingTemplate.convertAndSend("/topic/room/" + roomId, voteMessage);
        } catch (Exception e) {
            log.error("Error starting kick vote in room {}: {}", roomId, e.getMessage());
            sendErrorToUser(headerAccessor, e.getMessage());
        }
    }

    @MessageMapping("/room/{roomId}/vote/{voteId}/ballot")
    public void submitBallot(
            @DestinationVariable Long roomId,
            @DestinationVariable Long voteId,
            @Payload Map<String, Boolean> payload,
            SimpMessageHeaderAccessor headerAccessor) {

        try {
            Long userId = getUserIdFromSession(headerAccessor);
            Boolean vote = payload.get("vote");

            VoteService.VoteResult result = voteService.submitBallot(voteId, vote, userId);

            if (result.getStatus() != com.example.backend.entity.multiplayer.VoteStatus.PENDING) {
                RoomVoteResponse voteState = voteService.getVoteState(voteId);
                String messageContent;
                if (voteState.getVoteType().equals("KICK")) {
                    messageContent = result.getStatus() ==
                        com.example.backend.entity.multiplayer.VoteStatus.PASSED ?
                            "추방 투표가 가결되었습니다" : "추방 투표가 부결되었습니다";
                } else {
                    messageContent = result.getStatus() ==
                        com.example.backend.entity.multiplayer.VoteStatus.PASSED ?
                            "행동하기 투표가 가결되었습니다" : "행동하기 투표가 부결되었습니다";
                }

                ChatMessageResponse voteResultMessage = chatMessageService.sendSystemMessage(
                        roomId,
                        messageContent,
                        Map.of(
                            "type", "vote_end",
                            "voteId", voteId,
                            "result", result.getStatus().name()
                        )
                );

                messagingTemplate.convertAndSend("/topic/room/" + roomId, voteResultMessage);

                if (voteState.getVoteType().equals("ACTION") &&
                    result.getStatus() == com.example.backend.entity.multiplayer.VoteStatus.PASSED) {

                    llmIntegrationService.generateNextPhase(roomId)
                            .thenAccept(llmResult -> log.info("LLM 응답 처리 완료: Room {}", roomId))
                            .exceptionally(ex -> {
                                log.error("LLM 응답 처리 실패: Room {}", roomId, ex);
                                sendErrorToRoom(roomId, "AI 응답 생성에 실패했습니다");
                                return null;
                            });
                }
            }
        } catch (Exception e) {
            log.error("Error submitting ballot in room {}: {}", roomId, e.getMessage());
            sendErrorToUser(headerAccessor, e.getMessage());
        }
    }

    @MessageMapping("/room/{roomId}/sync")
    public void syncMessages(
            @DestinationVariable Long roomId,
            @Payload Map<String, Long> payload,
            SimpMessageHeaderAccessor headerAccessor) {

        try {
            Long userId = getUserIdFromSession(headerAccessor);
            boolean isParticipant = participantRepository.existsByRoomIdAndUserId(roomId, userId);

            if (!isParticipant) {
                throw new IllegalStateException("방 참가자만 메시지 동기화를 요청할 수 있습니다");
            }

            Long lastMessageId = payload.get("lastMessageId");
            List<ChatMessageResponse> missed = chatMessageService.getMessagesAfter(roomId, lastMessageId, 100);

            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/sync",
                    missed
            );
        } catch (Exception e) {
            log.error("Error syncing messages in room {}: {}", roomId, e.getMessage());
            sendErrorToUser(headerAccessor, e.getMessage());
        }
    }

    private Long getUserIdFromSession(SimpMessageHeaderAccessor headerAccessor) {
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        if (sessionAttributes == null) {
            throw new IllegalStateException("Session attributes not found");
        }

        Object userIdObj = sessionAttributes.get("userId");
        if (userIdObj == null) {
            throw new IllegalStateException("User ID not found in session");
        }

        return (Long) userIdObj;
    }

    private void sendErrorToUser(SimpMessageHeaderAccessor headerAccessor, String errorMessage) {
        try {
            Long userId = getUserIdFromSession(headerAccessor);
            Map<String, String> error = Map.of(
                "type", "error",
                "message", errorMessage
            );
            messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/errors",
                error
            );
        } catch (Exception e) {
            log.error("Failed to send error to user: {}", e.getMessage());
        }
    }

    private void sendErrorToRoom(Long roomId, String errorMessage) {
        try {
            ChatMessageResponse errorMsg = chatMessageService.sendSystemMessage(
                    roomId,
                    errorMessage,
                    Map.of("type", "error")
            );
            messagingTemplate.convertAndSend("/topic/room/" + roomId, errorMsg);
        } catch (Exception e) {
            log.error("Failed to send error to room {}: {}", roomId, e.getMessage());
        }
    }
}

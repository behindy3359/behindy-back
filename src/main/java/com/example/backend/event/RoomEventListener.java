package com.example.backend.event;

import com.example.backend.service.multiplayer.LlmIntegrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoomEventListener {

    private final LlmIntegrationService llmIntegrationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRoomCreated(RoomCreatedEvent event) {
        Long roomId = event.getRoomId();
        log.info("방 생성 완료, 인트로 스토리 생성 시작: Room {}", roomId);

        llmIntegrationService.generateNextPhase(roomId)
                .thenAccept(result -> log.info("인트로 스토리 생성 완료: Room {}", roomId))
                .exceptionally(ex -> {
                    log.error("인트로 스토리 생성 실패: Room {}", roomId, ex);
                    return null;
                });
    }
}

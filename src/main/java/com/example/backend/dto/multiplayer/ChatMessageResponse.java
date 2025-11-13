package com.example.backend.dto.multiplayer;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "채팅 메시지 응답")
public class ChatMessageResponse {

    @Schema(description = "메시지 ID", example = "1")
    private Long messageId;

    @Schema(description = "메시지 타입", example = "USER")
    private String messageType;

    @Schema(description = "메시지 내용", example = "앞으로 가보자!")
    private String content;

    @Schema(description = "사용자 ID", example = "10")
    private Long userId;

    @Schema(description = "캐릭터 이름", example = "모험가")
    private String characterName;

    @Schema(description = "추가 메타데이터")
    private Map<String, Object> metadata;

    @Schema(description = "생성 시간", example = "2025-11-10T10:00:00")
    private LocalDateTime createdAt;
}

package com.example.backend.dto.multiplayer;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "채팅 메시지 전송 요청")
public class ChatMessageRequest {

    @NotBlank(message = "메시지 내용은 필수입니다")
    @Size(max = 100, message = "메시지는 100자 이하여야 합니다")
    @Schema(description = "메시지 내용", example = "앞으로 가보자!", required = true)
    private String content;
}

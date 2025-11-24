package com.example.backend.dto.multiplayer;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "방 상세 정보 응답 (메시지/투표 포함)")
public class RoomDetailResponse {

    @Schema(description = "방 정보")
    private RoomResponse room;

    @Schema(description = "최근 메시지 목록")
    private List<ChatMessageResponse> messages;

    @Schema(description = "진행 중인 투표")
    private RoomVoteResponse activeVote;
}

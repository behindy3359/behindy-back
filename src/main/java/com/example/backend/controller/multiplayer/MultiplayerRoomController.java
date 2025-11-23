package com.example.backend.controller.multiplayer;

import com.example.backend.dto.multiplayer.RoomCreateRequest;
import com.example.backend.dto.multiplayer.RoomJoinRequest;
import com.example.backend.dto.multiplayer.RoomResponse;
import com.example.backend.service.multiplayer.MultiplayerRoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.StringUtils;

import java.util.List;

@Tag(name = "멀티플레이어 방 API", description = "멀티플레이어 방 생성, 참가, 조회 API")
@Slf4j
@RestController
@RequestMapping("/api/multiplayer/rooms")
@RequiredArgsConstructor
public class MultiplayerRoomController {

    private final MultiplayerRoomService roomService;

    @Operation(summary = "방 생성", description = "새로운 멀티플레이어 방을 생성합니다")
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RoomResponse> createRoom(@Valid @RequestBody RoomCreateRequest request) {
        RoomResponse response = roomService.createRoom(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "방 참가", description = "기존 방에 참가합니다")
    @PostMapping("/{roomId}/join")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RoomResponse> joinRoom(
            @PathVariable Long roomId,
            @Valid @RequestBody RoomJoinRequest request) {
        RoomResponse response = roomService.joinRoom(roomId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "방 퇴장", description = "현재 참가 중인 방에서 퇴장합니다")
    @PostMapping("/{roomId}/leave")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> leaveRoom(@PathVariable Long roomId) {
        roomService.leaveRoom(roomId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "역별 방 목록 조회", description = "특정 역의 활성 방 목록을 조회합니다")
    @GetMapping
    public ResponseEntity<List<RoomResponse>> getRoomsByStation(
            @RequestParam(required = false) Long stationId,
            @RequestParam(required = false) String stationName,
            @RequestParam(required = false) Integer lineNumber) {
        if (stationId == null && (!StringUtils.hasText(stationName) || lineNumber == null)) {
            return ResponseEntity.badRequest().build();
        }
        List<RoomResponse> rooms = roomService.getRoomsByStation(stationId, stationName, lineNumber);
        return ResponseEntity.ok(rooms);
    }

    @Operation(summary = "방 상세 조회", description = "방의 상세 정보를 조회합니다")
    @GetMapping("/{roomId}")
    public ResponseEntity<RoomResponse> getRoomDetail(@PathVariable Long roomId) {
        RoomResponse room = roomService.getRoomDetail(roomId);
        return ResponseEntity.ok(room);
    }
}

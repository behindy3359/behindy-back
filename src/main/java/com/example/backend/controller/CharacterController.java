package com.example.backend.controller;

import com.example.backend.dto.auth.ApiResponse;
import com.example.backend.dto.character.CharacterCreateRequest;
import com.example.backend.dto.character.CharacterResponse;
import com.example.backend.dto.character.VisitedStationResponse;
import com.example.backend.service.CharacterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Tag(name = "캐릭터 API", description = "게임 캐릭터 생성, 조회, 상태 관리 관련 API")
@Slf4j
@RestController
@RequestMapping("/api/characters")
@RequiredArgsConstructor
public class CharacterController {

    private final CharacterService characterService;

    @Operation(summary = "캐릭터 생성", description = "게임을 플레이할 새로운 캐릭터를 생성합니다. 캐릭터 이름과 초기 스탯이 설정됩니다. 인증된 사용자만 사용할 수 있습니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "캐릭터 생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "이미 살아있는 캐릭터가 존재함")
    })
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CharacterResponse> createCharacter(@Valid @RequestBody CharacterCreateRequest request) {
        CharacterResponse response = characterService.createCharacter(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "현재 살아있는 캐릭터 조회", description = "현재 로그인한 사용자의 활성화된(살아있는) 캐릭터 정보를 조회합니다. 인증된 사용자만 사용할 수 있습니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "캐릭터 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "살아있는 캐릭터 없음")
    })
    @GetMapping("/current")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CharacterResponse> getCurrentCharacter() {
        CharacterResponse response = characterService.getCurrentCharacter();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "캐릭터 존재 여부 확인", description = "게임 진입 전 살아있는 캐릭터가 있는지 확인합니다. 게임을 시작하기 전 캐릭터 생성 여부를 체크하는 용도로 사용됩니다. 인증된 사용자만 사용할 수 있습니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "확인 완료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @GetMapping("/exists")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> checkCharacterExists() {
        Optional<CharacterResponse> character = characterService.getCurrentCharacterOptional();
        boolean exists = character.isPresent();

        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message(exists ? "살아있는 캐릭터가 있습니다." : "캐릭터가 없습니다.")
                .data(character.orElse(null))
                .build());
    }

    @Operation(summary = "캐릭터 히스토리 조회", description = "현재 사용자가 생성한 모든 캐릭터의 히스토리를 조회합니다. 사망한 캐릭터를 포함한 전체 캐릭터 목록을 반환합니다. 인증된 사용자만 사용할 수 있습니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "히스토리 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @GetMapping("/history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CharacterResponse>> getCharacterHistory() {
        List<CharacterResponse> characters = characterService.getCharacterHistory();
        return ResponseEntity.ok(characters);
    }

    @Operation(summary = "캐릭터 사망 처리", description = "캐릭터를 사망 상태로 변경합니다. 체력이나 정신력이 0이 되었을 때 호출되며, 사망한 캐릭터는 더 이상 게임을 플레이할 수 없습니다. 인증된 사용자만 사용할 수 있습니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "캐릭터 사망 처리 완료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인의 캐릭터가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "캐릭터를 찾을 수 없음")
    })
    @DeleteMapping("/{charId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> killCharacter(
            @Parameter(description = "사망 처리할 캐릭터 ID", required = true) @PathVariable Long charId) {
        characterService.killCharacter(charId);
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("캐릭터가 사망 처리되었습니다.")
                .build());
    }

    @Operation(summary = "캐릭터 스탯 업데이트", description = "게임 진행 중 캐릭터의 체력(health)과 정신력(sanity) 스탯을 변경합니다. 선택지의 결과로 스탯이 증가하거나 감소할 수 있습니다. 인증된 사용자만 사용할 수 있습니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "스탯 업데이트 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인의 캐릭터가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "캐릭터를 찾을 수 없음")
    })
    @PatchMapping("/{charId}/stats")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CharacterResponse> updateCharacterStats(
            @Parameter(description = "스탯을 업데이트할 캐릭터 ID", required = true) @PathVariable Long charId,
            @Parameter(description = "체력 변화량 (양수: 증가, 음수: 감소)", required = false) @RequestParam(required = false) Integer healthChange,
            @Parameter(description = "정신력 변화량 (양수: 증가, 음수: 감소)", required = false) @RequestParam(required = false) Integer sanityChange) {
        CharacterResponse response = characterService.updateCharacterStats(charId, healthChange, sanityChange);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "캐릭터가 방문한 역 통계 조회",
               description = "현재 캐릭터가 클리어한 역들의 방문 통계를 조회합니다. 방문 횟수, 클리어율, 뱃지 등급 정보를 포함합니다. 인증된 사용자만 사용할 수 있습니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "방문한 역 통계 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "캐릭터를 찾을 수 없음")
    })
    @GetMapping("/visited-stations")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<VisitedStationResponse>> getVisitedStations() {
        List<VisitedStationResponse> responses = characterService.getVisitedStations();
        return ResponseEntity.ok(responses);
    }
}
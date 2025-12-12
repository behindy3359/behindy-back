package com.example.backend.controller;

import com.example.backend.dto.auth.ApiResponse;
import com.example.backend.dto.game.*;
import com.example.backend.service.GameService;
import com.example.backend.service.DemoAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.example.backend.dto.game.GameEnterResponse;

import java.util.List;

@Tag(name = "게임 API", description = "게임 플레이, 선택지 진행, 게임 상태 관리 관련 API")
@Slf4j
@RestController
@RequestMapping("/api/game")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;
    private final DemoAccountService demoAccountService;

    @Operation(summary = "게임 진입 자격 확인", description = "현재 사용자가 게임을 시작할 수 있는지 확인합니다. 살아있는 캐릭터 존재 여부, 진행 중인 게임 유무 등을 체크합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "자격 확인 완료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @GetMapping("/eligibility")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GameEligibilityResponse> checkGameEligibility() {
        GameEligibilityResponse response = gameService.checkGameEligibility();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "현재 게임 상태 조회", description = "진행 중인 게임의 현재 상태를 조회합니다. 현재 노드, 캐릭터 스탯, 사용 가능한 선택지 등을 반환합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "게임 상태 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "진행 중인 게임 없음")
    })
    @GetMapping("/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GameStateResponse> getCurrentGameState() {
        GameStateResponse response = gameService.getCurrentGameState();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "게임 시작", description = "특정 스토리를 선택하여 새로운 게임을 시작합니다. 게임 세션이 생성되고 첫 번째 노드로 이동합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "게임 시작 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "이미 진행 중인 게임이 있음")
    })
    @PostMapping("/start/{storyId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GameStartResponse> startGame(
            @Parameter(description = "시작할 스토리 ID", required = true) @PathVariable Long storyId) {
        GameStartResponse response = gameService.startGame(storyId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "역 기반 게임 진입 (핵심 기능)",
            description = """
                **사용자의 실제 위치(지하철역)를 기반으로 게임에 진입합니다.**

                이 프로젝트의 가장 핵심적인 기능으로, 실시간 지하철 위치와 게임을 연동합니다.

                **동작 과정:**
                1. 프론트엔드에서 사용자의 위치(역 이름, 노선 번호) 전송
                2. 서울시 실시간 지하철 위치 API로 현재 위치 검증
                3. 해당 역에 연결된 스토리 조회
                4. 기존 게임 세션이 있으면 이어서 진행, 없으면 새로 시작
                5. 캐릭터 상태와 현재 노드 정보 반환

                **특징:**
                - 실시간 지하철 API 연동 (30초 주기로 캐싱)
                - 자동 세션 관리 (재개 또는 신규 시작)
                - 역별 스토리 자동 매칭

                **예시:**
                - 강남역 2호선: `POST /api/game/enter/station/강남/line/2`
                - 홍대입구역 2호선: `POST /api/game/enter/station/홍대입구/line/2`
                """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "게임 진입 성공 - 새 게임 시작 또는 기존 게임 재개",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = GameEnterResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "게임 진입 성공",
                                    value = """
                                        {
                                          "gameSessionId": 42,
                                          "storyId": 1,
                                          "storyTitle": "강남역의 비밀",
                                          "currentNode": {
                                            "nodeId": 100,
                                            "nodeNumber": 1,
                                            "content": "당신은 강남역 플랫폼에 서있습니다. 수많은 사람들이 오가는 가운데, 이상한 기운을 느낍니다...",
                                            "options": [
                                              {
                                                "optionId": 1001,
                                                "content": "계단을 내려가 탐색한다",
                                                "effect": "health",
                                                "amount": -10
                                              },
                                              {
                                                "optionId": 1002,
                                                "content": "주변 사람들에게 물어본다",
                                                "effect": "sanity",
                                                "amount": -5
                                              }
                                            ]
                                          },
                                          "character": {
                                            "charId": 5,
                                            "charName": "모험가",
                                            "charHealth": 100,
                                            "charSanity": 100,
                                            "isAlive": true
                                          },
                                          "isNewGame": true,
                                          "message": "강남역의 새로운 이야기가 시작됩니다!"
                                        }
                                        """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 필요 - 로그인이 필요합니다"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "해당 역의 스토리 없음 - 아직 스토리가 개발되지 않은 역입니다",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    value = """
                                        {
                                          "success": false,
                                          "message": "홍대입구역에는 아직 스토리가 준비되지 않았습니다.",
                                          "data": null
                                        }
                                        """
                            )
                    )
            )
    })
    @PostMapping("/enter/station/{stationName}/line/{lineNumber}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GameEnterResponse> enterGameByStation(
            @Parameter(description = "현재 위치한 지하철역 이름 (예: 강남)", required = true) @PathVariable String stationName,
            @Parameter(description = "현재 위치한 노선 번호 (예: 2)", required = true) @PathVariable Integer lineNumber) {
        GameEnterResponse response = gameService.enterGameByStation(stationName, lineNumber);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "게임 재개", description = "이전에 중단했던 게임을 이어서 진행합니다. 마지막으로 저장된 지점부터 다시 시작합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "게임 재개 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "재개할 게임 없음")
    })
    @PostMapping("/resume")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GameResumeResponse> resumeGame() {
        GameResumeResponse response = gameService.resumeGame();
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "선택지 선택",
            description = """
                **게임 진행 중 제시된 선택지 중 하나를 선택합니다.**

                선택에 따라 다음 노드로 이동하고 캐릭터 스탯이 변경될 수 있습니다.

                **처리 과정:**
                1. 선택지 유효성 검증
                2. 캐릭터 스탯 변화 적용 (체력 또는 정신력)
                3. 다음 노드로 이동
                4. 게임 종료 조건 확인 (체력/정신력 0 이하, 엔딩 도달)
                5. 결과 반환

                **스탯 효과:**
                - `health`: 체력 변화 (-10 ~ +10)
                - `sanity`: 정신력 변화 (-10 ~ +10)

                **게임 오버 조건:**
                - 체력이 0 이하로 떨어지면 사망
                - 정신력이 0 이하로 떨어지면 광기
                - 특정 노드에 도달하면 엔딩 (좋은 엔딩/나쁜 엔딩)
                """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "선택 처리 성공 - 다음 노드로 이동",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ChoiceResultResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "일반 진행",
                                    value = """
                                        {
                                          "success": true,
                                          "chosenOption": {
                                            "optionId": 1001,
                                            "content": "계단을 내려가 탐색한다"
                                          },
                                          "statChange": {
                                            "type": "health",
                                            "amount": -10,
                                            "beforeValue": 100,
                                            "afterValue": 90,
                                            "message": "체력이 10 감소했습니다."
                                          },
                                          "nextNode": {
                                            "nodeId": 101,
                                            "nodeNumber": 2,
                                            "content": "어두운 계단을 내려가자 차가운 공기가 느껴집니다. 멀리서 이상한 소리가 들립니다...",
                                            "options": [
                                              {
                                                "optionId": 1003,
                                                "content": "소리가 나는 곳으로 간다",
                                                "effect": "sanity",
                                                "amount": -15
                                              },
                                              {
                                                "optionId": 1004,
                                                "content": "다시 올라간다",
                                                "effect": "health",
                                                "amount": -5
                                              }
                                            ]
                                          },
                                          "character": {
                                            "charId": 5,
                                            "charName": "모험가",
                                            "charHealth": 90,
                                            "charSanity": 100,
                                            "isAlive": true,
                                            "statusMessage": "건강"
                                          },
                                          "isGameOver": false,
                                          "isEnding": false
                                        }
                                        """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "게임 오버 - 캐릭터 사망",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "캐릭터 사망",
                                    value = """
                                        {
                                          "success": true,
                                          "chosenOption": {
                                            "optionId": 1005,
                                            "content": "정면으로 돌진한다"
                                          },
                                          "statChange": {
                                            "type": "health",
                                            "amount": -100,
                                            "beforeValue": 20,
                                            "afterValue": 0,
                                            "message": "치명상을 입었습니다!"
                                          },
                                          "character": {
                                            "charId": 5,
                                            "charName": "모험가",
                                            "charHealth": 0,
                                            "charSanity": 45,
                                            "isAlive": false,
                                            "statusMessage": "사망"
                                          },
                                          "isGameOver": true,
                                          "isEnding": false,
                                          "endingMessage": "당신은 강남역의 비밀을 밝히지 못하고 쓰러졌습니다...",
                                          "playTime": 1245
                                        }
                                        """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 필요"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "선택지를 찾을 수 없거나 진행 중인 게임이 없음"
            )
    })
    @PostMapping("/choice/{optionId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ChoiceResultResponse> makeChoice(
            @Parameter(description = "선택할 선택지 ID", required = true) @PathVariable Long optionId) {
        ChoiceResultResponse response = gameService.makeChoice(optionId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "게임 포기", description = "진행 중인 게임을 포기하고 종료합니다. 게임 세션이 종료되고 결과가 기록됩니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "게임 포기 처리 완료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "진행 중인 게임 없음")
    })
    @PostMapping("/quit")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GameQuitResponse> quitGame() {
        GameQuitResponse response = gameService.quitGame();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "[관리자] 진행 중인 모든 게임 세션 조회", description = "시스템에서 현재 진행 중인 모든 게임 세션을 조회합니다. 관리자 권한이 필요합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "세션 목록 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 필요")
    })
    @GetMapping("/admin/sessions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ActiveGameSessionResponse>> getAllActiveGameSessions() {
        List<ActiveGameSessionResponse> sessions = gameService.getAllActiveGameSessions();
        return ResponseEntity.ok(sessions);
    }

    @Operation(summary = "[관리자] 특정 스토리의 진행 통계 조회", description = "특정 스토리의 플레이 통계를 조회합니다. 총 플레이 수, 완료율, 평균 플레이 시간 등의 정보를 제공합니다. 관리자 권한이 필요합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "통계 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "스토리를 찾을 수 없음")
    })
    @GetMapping("/admin/stories/{storyId}/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StoryStatisticsResponse> getStoryStatistics(
            @Parameter(description = "통계를 조회할 스토리 ID", required = true) @PathVariable Long storyId) {
        StoryStatisticsResponse statistics = gameService.getStoryStatistics(storyId);
        return ResponseEntity.ok(statistics);
    }

    @Operation(summary = "[관리자] 오래된 게임 세션 정리", description = "지정된 기간 동안 활동이 없는 오래된 게임 세션을 정리합니다. 시스템 유지보수용 API입니다. 관리자 권한이 필요합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "세션 정리 완료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 필요")
    })
    @DeleteMapping("/admin/cleanup")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> cleanupOldGameSessions(
            @Parameter(description = "정리할 세션의 기준 일수 (기본값: 7일)", required = false) @RequestParam(defaultValue = "7") int daysOld) {
        int cleanedCount = gameService.cleanupOldGameSessions(daysOld);
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message(String.format("%d개의 오래된 게임 세션이 정리되었습니다.", cleanedCount))
                .data(cleanedCount)
                .build());
    }

    @Operation(summary = "[관리자] 데모 계정 세션 해제", description = "모든 데모 계정의 로그인 세션(Redis Refresh Token)을 삭제하여 다른 사용자가 로그인할 수 있게 합니다. 실제 데이터(캐릭터, 게시글, 댓글 등)는 유지됩니다. 배포 후 자동 호출용 API입니다. 관리자 권한이 필요합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "데모 계정 세션 해제 완료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 필요")
    })
    @PostMapping("/admin/demo-accounts/release")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> releaseDemoAccounts() {
        demoAccountService.releaseAllDemoAccounts();
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("데모 계정 세션 해제가 완료되었습니다. 이제 다른 사용자가 로그인할 수 있습니다.")
                .build());
    }
}
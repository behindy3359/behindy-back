package com.example.backend.controller;

import com.example.backend.dto.game.StoryListResponse;
import com.example.backend.dto.game.StoryResponse;
import com.example.backend.service.StoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "스토리 API", description = "게임 스토리 조회 관련 API")
@RestController
@RequestMapping("/api/stories")
@RequiredArgsConstructor
public class StoryController {

    private final StoryService storyService;

    @Operation(summary = "전체 스토리 목록 조회", description = "등록된 모든 스토리를 조회합니다.")
    @GetMapping
    public ResponseEntity<List<StoryResponse>> getAllStories() {
        List<StoryResponse> stories = storyService.getAllStories();
        return ResponseEntity.ok(stories);
    }

    @Operation(summary = "노선별 스토리 조회", description = "특정 지하철 노선의 스토리를 조회합니다.")
    @GetMapping("/line/{lineNumber}")
    public ResponseEntity<List<StoryResponse>> getStoriesByLine(
            @Parameter(description = "지하철 노선 번호 (예: 2)", required = true) @PathVariable Integer lineNumber) {
        List<StoryResponse> stories = storyService.getStoriesByLine(lineNumber);
        return ResponseEntity.ok(stories);
    }

    @Operation(summary = "특정 역의 스토리 조회", description = "특정 지하철역의 모든 스토리를 조회합니다.")
    @GetMapping("/station/{stationName}")
    public ResponseEntity<StoryListResponse> getStoriesByStation(
            @Parameter(description = "지하철역 이름 (예: 강남)", required = true) @PathVariable String stationName) {
        StoryListResponse response = storyService.getStoriesByStation(stationName);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "특정 역 + 노선의 스토리 조회", description = "특정 지하철역과 노선 번호를 모두 지정하여 해당하는 스토리를 조회합니다.")
    @GetMapping("/station/{stationName}/line/{lineNumber}")
    public ResponseEntity<StoryListResponse> getStoriesByStationAndLine(
            @Parameter(description = "지하철역 이름 (예: 강남)", required = true) @PathVariable String stationName,
            @Parameter(description = "지하철 노선 번호 (예: 2)", required = true) @PathVariable Integer lineNumber) {
        StoryListResponse response = storyService.getStoriesByStationAndLine(stationName, lineNumber);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "단일 스토리 상세 조회", description = "스토리 ID를 사용하여 특정 스토리의 상세 정보를 조회합니다.")
    @GetMapping("/{storyId}")
    public ResponseEntity<StoryResponse> getStoryById(
            @Parameter(description = "조회할 스토리 ID", required = true) @PathVariable Long storyId) {
        StoryResponse story = storyService.getStoryById(storyId);
        return ResponseEntity.ok(story);
    }

    @Operation(summary = "랜덤 스토리 추천", description = "전체 스토리 중에서 무작위로 선택된 스토리들을 조회합니다.")
    @GetMapping("/random")
    public ResponseEntity<List<StoryResponse>> getRandomStories(
            @Parameter(description = "추천받을 스토리 개수 (기본값: 3)", required = false) @RequestParam(defaultValue = "3") Integer count) {
        List<StoryResponse> stories = storyService.getRandomStories(count);
        return ResponseEntity.ok(stories);
    }

    @Operation(summary = "특정 노선의 랜덤 스토리 추천", description = "특정 지하철 노선의 스토리 중에서 무작위로 선택된 스토리들을 조회합니다.")
    @GetMapping("/random/line/{lineNumber}")
    public ResponseEntity<List<StoryResponse>> getRandomStoriesByLine(
            @Parameter(description = "지하철 노선 번호 (예: 2)", required = true) @PathVariable Integer lineNumber,
            @Parameter(description = "추천받을 스토리 개수 (기본값: 3)", required = false) @RequestParam(defaultValue = "3") Integer count) {
        List<StoryResponse> stories = storyService.getRandomStoriesByLine(lineNumber, count);
        return ResponseEntity.ok(stories);
    }

    @Operation(summary = "난이도별 스토리 조회", description = "특정 난이도의 스토리들을 조회합니다. 난이도는 EASY, MEDIUM, HARD 중 하나를 사용합니다.")
    @GetMapping("/difficulty/{difficulty}")
    public ResponseEntity<List<StoryResponse>> getStoriesByDifficulty(
            @Parameter(description = "난이도 (EASY, MEDIUM, HARD)", required = true) @PathVariable String difficulty) {
        List<StoryResponse> stories = storyService.getStoriesByDifficulty(difficulty);
        return ResponseEntity.ok(stories);
    }
}
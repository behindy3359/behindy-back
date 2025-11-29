package com.example.backend.service.multiplayer;

import com.example.backend.dto.multiplayer.UserStatsResponse;
import com.example.backend.entity.User;
import com.example.backend.entity.multiplayer.UserStoryStats;
import com.example.backend.repository.multiplayer.UserStoryStatsRepository;
import com.example.backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserStatsService {

    private final UserStoryStatsRepository statsRepository;
    private final AuthService authService;

    @Transactional(readOnly = true)
    public UserStatsResponse getMyStats() {
        User currentUser = authService.getCurrentUser();

        UserStoryStats stats = statsRepository.findByUserId(currentUser.getUserId())
                .orElse(UserStoryStats.builder()
                        .userId(currentUser.getUserId())
                        .user(currentUser)
                        .totalParticipations(0)
                        .totalCompletions(0)
                        .totalDeaths(0)
                        .totalKicks(0)
                        .build());

        return toStatsResponse(stats);
    }

    @Transactional(readOnly = true)
    public UserStatsResponse getUserStats(Long userId) {
        UserStoryStats stats = statsRepository.findByUserId(userId)
                .orElse(UserStoryStats.builder()
                        .userId(userId)
                        .totalParticipations(0)
                        .totalCompletions(0)
                        .totalDeaths(0)
                        .totalKicks(0)
                        .build());

        return toStatsResponse(stats);
    }

    @Transactional
    public void incrementCompletion(Long userId) {
        UserStoryStats stats = statsRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("통계를 찾을 수 없습니다"));

        stats.incrementCompletions();
        statsRepository.save(stats);
    }

    @Transactional
    public void incrementDeath(Long userId) {
        UserStoryStats stats = statsRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("통계를 찾을 수 없습니다"));

        stats.incrementDeaths();
        statsRepository.save(stats);
    }

    @Transactional
    public void incrementKick(Long userId) {
        UserStoryStats stats = statsRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("통계를 찾을 수 없습니다"));

        stats.incrementKicks();
        statsRepository.save(stats);
    }

    private UserStatsResponse toStatsResponse(UserStoryStats stats) {
        return UserStatsResponse.builder()
                .userId(stats.getUserId())
                .totalParticipations(stats.getTotalParticipations())
                .totalCompletions(stats.getTotalCompletions())
                .totalDeaths(stats.getTotalDeaths())
                .totalKicks(stats.getTotalKicks())
                .completionRate(stats.getCompletionRate())
                .build();
    }
}

package com.example.backend.service;

import com.example.backend.config.DemoAccountConfig;
import com.example.backend.entity.User;
import com.example.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class DemoAccountService {

    private final DemoAccountConfig demoAccountConfig;
    private final UserRepository userRepository;
    private final RedisService redisService;

    @Transactional
    public void releaseAllDemoAccounts() {
        List<DemoAccountConfig.DemoAccount> demoAccounts = demoAccountConfig.getAccounts();

        if (demoAccounts == null || demoAccounts.isEmpty()) {
            log.warn("설정된 데모 계정이 없습니다");
            return;
        }

        int totalReleased = 0;

        for (DemoAccountConfig.DemoAccount demoAccount : demoAccounts) {
            try {
                int released = releaseDemoAccount(demoAccount.getEmail());
                totalReleased += released;
                log.info("[RELEASED] Demo account: {} ({} tokens)", demoAccount.getEmail(), released);
            } catch (Exception e) {
                log.error("[FAILED] Demo account release failed: {} - {}", demoAccount.getEmail(), e.getMessage(), e);
            }
        }

        log.info("[COMPLETED] Total demo accounts released: {} tokens", totalReleased);
    }

    @Transactional
    public int releaseDemoAccount(String email) {
        User user = userRepository.findByUserEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("데모 계정을 찾을 수 없습니다: " + email));

        int tokenCount = 0;

        try {
            Set<String> tokens = redisService.getAllRefreshTokensForUser(String.valueOf(user.getUserId()));
            tokenCount = tokens.size();
            redisService.deleteAllRefreshTokensForUser(String.valueOf(user.getUserId()));
            log.debug("Redis tokens deleted: {}", tokenCount);
        } catch (Exception e) {
            log.warn("Redis 토큰 삭제 중 오류: {}", e.getMessage());
        }

        return tokenCount;
    }

    public List<DemoAccountConfig.DemoAccount> getDemoAccounts() {
        return demoAccountConfig.getAccounts();
    }

    public boolean isDemoAccount(String email) {
        return demoAccountConfig.getAccounts().stream()
                .anyMatch(account -> account.getEmail().equals(email));
    }
}

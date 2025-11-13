package com.example.backend.service.multiplayer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimiter {

    private static final long MESSAGE_INTERVAL_MS = 2000;
    private final RedisTemplate<String, String> redisTemplate;

    public boolean allowMessage(Long userId) {
        String key = "user:" + userId + ":last_message";

        String lastTimeStr = redisTemplate.opsForValue().get(key);
        long now = System.currentTimeMillis();

        if (lastTimeStr != null) {
            long lastTime = Long.parseLong(lastTimeStr);
            if (now - lastTime < MESSAGE_INTERVAL_MS) {
                log.debug("Rate limit exceeded for user {}", userId);
                return false;
            }
        }

        redisTemplate.opsForValue().set(
            key,
            String.valueOf(now),
            MESSAGE_INTERVAL_MS,
            TimeUnit.MILLISECONDS
        );

        return true;
    }

    public long getRemainingCooldown(Long userId) {
        String key = "user:" + userId + ":last_message";
        String lastTimeStr = redisTemplate.opsForValue().get(key);

        if (lastTimeStr == null) {
            return 0;
        }

        long lastTime = Long.parseLong(lastTimeStr);
        long now = System.currentTimeMillis();
        long elapsed = now - lastTime;

        return Math.max(0, MESSAGE_INTERVAL_MS - elapsed);
    }
}

package com.example.backend.security.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final RedisTemplate<String, String> redisTemplate;

    public void saveRefreshToken(String userId, String refreshToken, long expirationMs) {
        redisTemplate.opsForValue().set("RT:" + userId, refreshToken, expirationMs, TimeUnit.MILLISECONDS);
    }

    public boolean isRefreshTokenValid(String userId, String refreshToken) {
        String stored = redisTemplate.opsForValue().get("RT:" + userId);
        return refreshToken.equals(stored);
    }

    public void deleteRefreshToken(String userId) {
        redisTemplate.delete("RT:" + userId);
    }
}

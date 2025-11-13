package com.example.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, String> redisTemplate;

    public void setWithExpiration(String key, String value, long expirationMs) {
        try {
            redisTemplate.opsForValue().set(key, value, expirationMs, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.error("Redis 저장 실패: key={}, error={}", key, e.getMessage());
            throw new RuntimeException("Redis 저장 중 오류가 발생했습니다.", e);
        }
    }

    public String get(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("Redis 조회 실패: key={}, error={}", key, e.getMessage());
            return null;
        }
    }

    public void delete(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.error("Redis 삭제 실패: key={}, error={}", key, e.getMessage());
        }
    }

    public boolean hasKey(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("Redis 키 존재 확인 실패: key={}, error={}", key, e.getMessage());
            return false;
        }
    }

    public long getExpire(String key) {
        try {
            return redisTemplate.getExpire(key, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Redis TTL 조회 실패: key={}, error={}", key, e.getMessage());
            return -1;
        }
    }

    public void saveRefreshToken(Long userId, String token, long expirationMs) {
        String key = "RT:" + userId;
        setWithExpiration(key, token, expirationMs);
    }

    public boolean validateRefreshToken(Long userId, String token) {
        String key = "RT:" + userId;
        String stored = get(key);
        return token.equals(stored);
    }

    public void deleteRefreshToken(Long userId) {
        String key = "RT:" + userId;
        delete(key);
    }

    public boolean isRefreshTokenValid(String userId, String jti) {
        String key = "RT:" + userId + ":" + jti;
        return hasKey(key);
    }

    public void deleteRefreshToken(String userId, String jti) {
        String key = "RT:" + userId + ":" + jti;
        delete(key);
    }

    public java.util.Set<String> getAllRefreshTokensForUser(String userId) {
        try {
            String pattern = "RT:" + userId + ":*";
            var keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                log.debug("사용자 {}의 활성 Refresh Token: {} 개", userId, keys.size());
                return keys;
            }
            return java.util.Collections.emptySet();
        } catch (Exception e) {
            log.error("사용자 {}의 Refresh Token 조회 실패: {}", userId, e.getMessage());
            return java.util.Collections.emptySet();
        }
    }

    public void deleteAllRefreshTokensForUser(String userId) {
        try {
            String pattern = "RT:" + userId + ":*";
            var keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("사용자 {}의 모든 Refresh Token 삭제: {} 개", userId, keys.size());
            }
        } catch (Exception e) {
            log.error("사용자 {}의 Refresh Token 일괄 삭제 실패: {}", userId, e.getMessage());
        }
    }

    public void cleanupExpiredTokens() {
        try {
            String pattern = "RT:*";
            var keys = redisTemplate.keys(pattern);
            if (keys != null) {
                int cleanedCount = 0;
                for (String key : keys) {
                    if (getExpire(key) <= 0) {
                        delete(key);
                        cleanedCount++;
                    }
                }
                log.info("만료된 Refresh Token 정리 완료: {} 개", cleanedCount);
            }
        } catch (Exception e) {
            log.error("만료된 토큰 정리 실패: {}", e.getMessage());
        }
    }

    public void cacheMetroData(String key, String data, long ttlMinutes) {
        setWithExpiration("METRO:" + key, data, ttlMinutes * 60 * 1000);
    }

    public String getMetroData(String key) {
        return get("METRO:" + key);
    }
}
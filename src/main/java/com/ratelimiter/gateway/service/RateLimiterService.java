package com.ratelimiter.gateway.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Service
public class RateLimiterService {

    private final StringRedisTemplate redisTemplate;

    public RateLimiterService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isAllowed(String clientId, int limit, int windowSeconds) {

        long currentEpochSeconds = Instant.now().getEpochSecond();
        long windowStart = currentEpochSeconds / windowSeconds * windowSeconds;

        String key = "rate:" + clientId + ":" + windowStart;

        Long count = redisTemplate.opsForValue().increment(key);

        if (count != null && count == 1) {
            redisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);
        }

        return count != null && count <= limit;
    }
}

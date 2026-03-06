package com.ratelimiter.gateway.service;

import com.ratelimiter.gateway.model.RateLimitAlgorithm;
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

    public boolean isAllowed(String clientId,
                             String endpoint,
                             int limit,
                             int windowSeconds,
                             RateLimitAlgorithm algorithm) {

        System.out.println("Client: " + clientId +
                " Endpoint: " + endpoint +
                " Algorithm: " + algorithm);

        if (algorithm == RateLimitAlgorithm.FIXED_WINDOW) {
            return isAllowedFixedWindow(clientId, endpoint, limit, windowSeconds);
        }

        return isAllowedTokenBucket(clientId, endpoint, limit, windowSeconds);
    }

    public boolean isAllowedTokenBucket(String clientId,
                                        String endpoint,
                                        int capacity,
                                        int windowSeconds) {

        String keyPrefix = clientId + ":" + endpoint;

        String tokensKey = "bucket:tokens:" + keyPrefix;
        String timestampKey = "bucket:timestamp:" + keyPrefix;

        long now = Instant.now().getEpochSecond();

        String lastRefillStr = redisTemplate.opsForValue().get(timestampKey);
        String tokensStr = redisTemplate.opsForValue().get(tokensKey);

        long lastRefill = lastRefillStr == null ? now : Long.parseLong(lastRefillStr);
        int tokens = tokensStr == null ? capacity : Integer.parseInt(tokensStr);

        long elapsedTime = now - lastRefill;

        int refillRate = capacity / windowSeconds;
        int refillTokens = (int) (elapsedTime * refillRate);

        tokens = Math.min(capacity, tokens + refillTokens);

        if (tokens <= 0) {
            return false;
        }

        tokens--;

        redisTemplate.opsForValue().set(tokensKey, String.valueOf(tokens));
        redisTemplate.opsForValue().set(timestampKey, String.valueOf(now));

        redisTemplate.expire(tokensKey, windowSeconds * 2L, TimeUnit.SECONDS);
        redisTemplate.expire(timestampKey, windowSeconds * 2L, TimeUnit.SECONDS);

        return true;
    }

    public boolean isAllowedFixedWindow(String clientId,
                                        String endpoint,
                                        int limit,
                                        int windowSeconds) {

        String key = "fixed_window:" + clientId + ":" + endpoint;

        Long count = redisTemplate.opsForValue().increment(key);

        System.out.println("Fixed window key: " + key + " count=" + count);

        if (count == null) {
            return false;
        }

        if (count == 1) {
            redisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);
        }

        return count <= limit;
    }
}
package com.ratelimiter.gateway.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;
import java.util.Map;

@Service
public class MetricsService {

    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong allowedRequests = new AtomicLong(0);
    private final AtomicLong rateLimitedRequests = new AtomicLong(0);

    // Per-client metrics storage
    private final ConcurrentHashMap<String, ClientMetrics> clientMetricsMap = new ConcurrentHashMap<>();

    public void incrementTotalRequests(String apiKey) {

        totalRequests.incrementAndGet();

        clientMetricsMap
                .computeIfAbsent(apiKey, k -> new ClientMetrics())
                .incrementTotalRequests();
    }

    public void incrementAllowedRequests(String apiKey) {

        allowedRequests.incrementAndGet();

        clientMetricsMap
                .computeIfAbsent(apiKey, k -> new ClientMetrics())
                .incrementAllowedRequests();
    }

    public void incrementRateLimitedRequests(String apiKey) {

        rateLimitedRequests.incrementAndGet();

        clientMetricsMap
                .computeIfAbsent(apiKey, k -> new ClientMetrics())
                .incrementRateLimitedRequests();
    }

    public Map<String, Long> getMetrics() {

        Map<String, Long> metrics = new HashMap<>();

        metrics.put("totalRequests", totalRequests.get());
        metrics.put("allowedRequests", allowedRequests.get());
        metrics.put("rateLimitedRequests", rateLimitedRequests.get());

        return metrics;
    }

    public Map<String, Long> getClientMetrics(String apiKey) {

        ClientMetrics clientMetrics = clientMetricsMap.get(apiKey);

        if (clientMetrics == null) {
            return null;
        }

        Map<String, Long> metrics = new HashMap<>();

        metrics.put("totalRequests", clientMetrics.getTotalRequests());
        metrics.put("allowedRequests", clientMetrics.getAllowedRequests());
        metrics.put("rateLimitedRequests", clientMetrics.getRateLimitedRequests());

        return metrics;
    }

    // Inner class to store client-specific counters
    static class ClientMetrics {

        private final AtomicLong totalRequests = new AtomicLong(0);
        private final AtomicLong allowedRequests = new AtomicLong(0);
        private final AtomicLong rateLimitedRequests = new AtomicLong(0);

        public void incrementTotalRequests() {
            totalRequests.incrementAndGet();
        }

        public void incrementAllowedRequests() {
            allowedRequests.incrementAndGet();
        }

        public void incrementRateLimitedRequests() {
            rateLimitedRequests.incrementAndGet();
        }

        public long getTotalRequests() {
            return totalRequests.get();
        }

        public long getAllowedRequests() {
            return allowedRequests.get();
        }

        public long getRateLimitedRequests() {
            return rateLimitedRequests.get();
        }
    }
}
package com.ratelimiter.gateway.controller;

import com.ratelimiter.gateway.service.MetricsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/metrics")
public class MetricsController {

    private final MetricsService metricsService;

    public MetricsController(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    // Global gateway metrics
    @GetMapping
    public Map<String, Long> getGlobalMetrics() {
        return metricsService.getMetrics();
    }

    // Metrics for a specific API key
    @GetMapping("/client/{apiKey}")
    public ResponseEntity<?> getClientMetrics(@PathVariable String apiKey) {

        Map<String, Long> metrics = metricsService.getClientMetrics(apiKey);

        if (metrics == null) {
            return ResponseEntity
                    .status(404)
                    .body("No metrics found for this API key");
        }

        return ResponseEntity.ok(metrics);
    }
}
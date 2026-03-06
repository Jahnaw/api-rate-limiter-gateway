package com.ratelimiter.gateway.filter;

import com.ratelimiter.gateway.model.Client;
import com.ratelimiter.gateway.service.ClientCacheService;
import com.ratelimiter.gateway.service.RateLimiterService;
import com.ratelimiter.gateway.service.MetricsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-KEY";

    private final ClientCacheService clientCacheService;
    private final RateLimiterService rateLimiterService;
    private final MetricsService metricsService;

    public ApiKeyAuthFilter(ClientCacheService clientCacheService,
                            RateLimiterService rateLimiterService,
                            MetricsService metricsService) {
        this.clientCacheService = clientCacheService;
        this.rateLimiterService = rateLimiterService;
        this.metricsService = metricsService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().equals("/clients/register") ||
                request.getRequestURI().equals("/metrics");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String apiKey = request.getHeader(API_KEY_HEADER);

        if (apiKey == null || apiKey.isBlank()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Missing API Key");
            return;
        }

        // track total requests for this API key
        metricsService.incrementTotalRequests(apiKey);

        Client client = clientCacheService.getClientByApiKey(apiKey);

        if (client == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Invalid API Key");
            return;
        }

        // Extract endpoint
        String requestUri = request.getRequestURI();
        String endpoint = requestUri.replaceFirst("/proxy/" + client.getName(), "");

        boolean allowed = rateLimiterService.isAllowed(
                client.getId().toString(),
                endpoint,
                client.getRateLimit(),
                client.getWindowSeconds(),
                client.getAlgorithm()
        );

        if (!allowed) {

            metricsService.incrementRateLimitedRequests(apiKey);

            response.setStatus(429);
            response.getWriter().write("Rate limit exceeded");
            return;
        }

        metricsService.incrementAllowedRequests(apiKey);

        filterChain.doFilter(request, response);
    }
}
package com.ratelimiter.gateway.filter;

import com.ratelimiter.gateway.model.Client;
import com.ratelimiter.gateway.repository.ClientRepository;
import com.ratelimiter.gateway.service.RateLimiterService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-KEY";

    private final ClientRepository clientRepository;
    private final RateLimiterService rateLimiterService;

    public ApiKeyAuthFilter(ClientRepository clientRepository,
                            RateLimiterService rateLimiterService) {
        this.clientRepository = clientRepository;
        this.rateLimiterService = rateLimiterService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().equals("/clients/register");
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

        Optional<Client> clientOpt = clientRepository.findByApiKey(apiKey);

        if (clientOpt.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Invalid API Key");
            return;
        }

        Client client = clientOpt.get();

        boolean allowed = rateLimiterService.isAllowed(
                client.getId().toString(),
                client.getRateLimit(),
                client.getWindowSeconds()
        );

        if (!allowed) {
            response.setStatus(429);
            response.getWriter().write("Rate limit exceeded");
            return;
        }

        // Passed auth + rate limit
        filterChain.doFilter(request, response);
    }
}

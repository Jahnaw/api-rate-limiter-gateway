package com.ratelimiter.gateway.controller;

import com.ratelimiter.gateway.model.Client;
import com.ratelimiter.gateway.repository.ClientRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Enumeration;
import java.util.Optional;

@RestController
public class ProxyController {

    private final ClientRepository clientRepository;
    private final RestTemplate restTemplate;

    public ProxyController(ClientRepository clientRepository,
                           RestTemplate restTemplate) {
        this.clientRepository = clientRepository;
        this.restTemplate = restTemplate;
    }

    @RequestMapping("/proxy/**")
    public ResponseEntity<byte[]> proxy(HttpServletRequest request) throws Exception {

        String apiKey = request.getHeader("X-API-KEY");
        if (apiKey == null || apiKey.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Client client = clientRepository.findByApiKey(apiKey)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        // Strip gateway prefix
        String requestUri = request.getRequestURI();
        String forwardPath = requestUri.replaceFirst(
                "/proxy/" + client.getName(),
                ""
        );

        String targetUrl = client.getBackendBaseUrl() + forwardPath;

        HttpHeaders headers = new HttpHeaders();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            if (!headerName.equalsIgnoreCase("host")) {
                headers.add(headerName, request.getHeader(headerName));
            }
        }

        byte[] body = request.getInputStream().readAllBytes();
        HttpMethod method = HttpMethod.valueOf(request.getMethod());

        HttpEntity<byte[]> entity = new HttpEntity<>(body, headers);

        ResponseEntity<byte[]> response = restTemplate.exchange(
                targetUrl,
                method,
                entity,
                byte[].class
        );

        return ResponseEntity
                .status(response.getStatusCode())
                .headers(response.getHeaders())
                .body(response.getBody());
    }
}

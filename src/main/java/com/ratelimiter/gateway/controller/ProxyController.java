package com.ratelimiter.gateway.controller;

import com.ratelimiter.gateway.model.Client;
import com.ratelimiter.gateway.repository.ClientRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

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

    @RequestMapping("/**")
    public ResponseEntity<byte[]> proxy(HttpServletRequest request) {

        // Skip client registration
        if (request.getRequestURI().equals("/clients/register")) {
            return ResponseEntity.notFound().build();
        }

        String apiKey = request.getHeader("X-API-KEY");
        Optional<Client> clientOpt = clientRepository.findByApiKey(apiKey);

        if (clientOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Client client = clientOpt.get();

        String targetUrl = client.getBackendBaseUrl() + request.getRequestURI();

        HttpHeaders headers = new HttpHeaders();

        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headers.add(headerName, request.getHeader(headerName));
        }

        HttpMethod method = HttpMethod.valueOf(request.getMethod());
        HttpEntity<byte[]> entity = new HttpEntity<>(null, headers);

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

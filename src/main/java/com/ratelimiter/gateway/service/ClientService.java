package com.ratelimiter.gateway.service;

import com.ratelimiter.gateway.dto.ClientRegistrationRequest;
import com.ratelimiter.gateway.model.Client;
import com.ratelimiter.gateway.model.RateLimitAlgorithm;
import com.ratelimiter.gateway.repository.ClientRepository;
import org.springframework.stereotype.Service;

@Service
public class ClientService {

    private final ClientRepository clientRepository;
    private final ApiKeyGenerator apiKeyGenerator;

    public ClientService(ClientRepository clientRepository,
                         ApiKeyGenerator apiKeyGenerator) {
        this.clientRepository = clientRepository;
        this.apiKeyGenerator = apiKeyGenerator;
    }

    public Client register(ClientRegistrationRequest request) {

        Client client = new Client();

        client.setName(request.getName());
        client.setBackendBaseUrl(request.getBackendBaseUrl());
        client.setRateLimit(request.getRateLimit());
        client.setWindowSeconds(request.getWindowSeconds());
        client.setApiKey(apiKeyGenerator.generate());
        client.setActive(true);

        // Set algorithm (default = TOKEN_BUCKET)
        if (request.getAlgorithm() != null) {
            client.setAlgorithm(request.getAlgorithm());
        } else {
            client.setAlgorithm(RateLimitAlgorithm.TOKEN_BUCKET);
        }

        return clientRepository.save(client);
    }
}
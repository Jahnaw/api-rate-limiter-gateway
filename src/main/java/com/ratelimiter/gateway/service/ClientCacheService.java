package com.ratelimiter.gateway.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ratelimiter.gateway.model.Client;
import com.ratelimiter.gateway.repository.ClientRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class ClientCacheService {

    private static final String CACHE_PREFIX = "client:apikey:";
    private static final long CACHE_TTL_MINUTES = 10;

    private final StringRedisTemplate redisTemplate;
    private final ClientRepository clientRepository;
    private final ObjectMapper objectMapper;

    public ClientCacheService(
            StringRedisTemplate redisTemplate,
            ClientRepository clientRepository,
            ObjectMapper objectMapper
    ) {
        this.redisTemplate = redisTemplate;
        this.clientRepository = clientRepository;
        this.objectMapper = objectMapper;
    }

    public Client getClientByApiKey(String apiKey) {

        String redisKey = CACHE_PREFIX + apiKey;

        // 1️⃣ Check Redis
        String cachedValue = redisTemplate.opsForValue().get(redisKey);

        if (cachedValue != null) {
            try {
                return objectMapper.readValue(cachedValue, Client.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Error parsing cached client JSON", e);
            }
        }

        // 2️⃣ Cache miss → query DB
        Optional<Client> clientOpt = clientRepository.findByApiKey(apiKey);

        if (clientOpt.isEmpty()) {
            return null;
        }

        Client client = clientOpt.get();

        try {

            // 3️⃣ Convert client → JSON
            String json = objectMapper.writeValueAsString(client);

            // 4️⃣ Store in Redis with TTL
            redisTemplate.opsForValue()
                    .set(redisKey, json, CACHE_TTL_MINUTES, TimeUnit.MINUTES);

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing client", e);
        }

        return client;
    }
}
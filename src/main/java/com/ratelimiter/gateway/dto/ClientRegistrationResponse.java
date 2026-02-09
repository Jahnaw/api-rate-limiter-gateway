package com.ratelimiter.gateway.dto;

import java.util.UUID;

public class ClientRegistrationResponse {

    private UUID clientId;
    private String apiKey;

    public ClientRegistrationResponse(UUID clientId, String apiKey) {
        this.clientId = clientId;
        this.apiKey = apiKey;
    }

    public UUID getClientId() {
        return clientId;
    }

    public String getApiKey() {
        return apiKey;
    }
}

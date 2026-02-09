package com.ratelimiter.gateway.controller;

import com.ratelimiter.gateway.dto.ClientRegistrationRequest;
import com.ratelimiter.gateway.dto.ClientRegistrationResponse;
import com.ratelimiter.gateway.model.Client;
import com.ratelimiter.gateway.service.ClientService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/clients")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ClientRegistrationResponse register(
            @RequestBody ClientRegistrationRequest request
    ) {
        Client client = clientService.register(request);
        return new ClientRegistrationResponse(
                client.getId(),
                client.getApiKey()
        );
    }
}

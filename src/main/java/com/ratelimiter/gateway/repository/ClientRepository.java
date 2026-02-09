package com.ratelimiter.gateway.repository;

import com.ratelimiter.gateway.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ClientRepository extends JpaRepository<Client, UUID> {

    Optional<Client> findByApiKey(String apiKey);
}

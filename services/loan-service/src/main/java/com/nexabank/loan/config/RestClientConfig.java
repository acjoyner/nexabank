package com.nexabank.loan.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * RestClient Configuration.
 *
 * RestClient is the modern Spring Boot 3.2+ HTTP client — it replaces RestTemplate.
 * It uses a fluent builder API and supports both synchronous and reactive usage.
 *
 * The base URL points to the Python AI layer service.
 * In Docker, this resolves to the "ai-layer" container via Docker DNS.
 */
@Configuration
public class RestClientConfig {

    @Value("${ai-layer.url:http://localhost:8000}")
    private String aiLayerUrl;

    @Bean
    public RestClient restClient() {
        return RestClient.builder()
                .baseUrl(aiLayerUrl)
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Accept", "application/json")
                .build();
    }
}

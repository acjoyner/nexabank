package com.nexabank.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * NexaBank API Gateway — Single entry point for all client requests.
 *
 * Responsibilities:
 * - JWT authentication (validates token, rejects unauthenticated requests)
 * - Request routing (load-balanced via Eureka service names)
 * - Rate limiting (prevent abuse)
 * - CORS (allows Angular frontend at localhost:4200)
 * - Correlation ID injection (for distributed tracing)
 *
 * Routes are defined in config-server: config/api-gateway.yml
 * See docs/learning/02-spring-cloud-eureka-gateway.md
 */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}

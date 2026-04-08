package com.nexabank.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Global request logging filter — runs on EVERY request through the gateway.
 *
 * Assigns a correlation ID (UUID) to each request, injected as X-Correlation-Id header.
 * Downstream services should propagate this header in their logs so that a
 * single user action can be traced across multiple microservices.
 *
 * This is a critical pattern for onshore-offshore debugging — offshore teams
 * can share a correlation ID in tickets to identify the exact request chain.
 */
@Component
@Slf4j
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String correlationId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();

        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header("X-Correlation-Id", correlationId)
                .build();

        log.info("[{}] --> {} {}",
                correlationId,
                exchange.getRequest().getMethod(),
                exchange.getRequest().getURI().getPath());

        return chain.filter(exchange.mutate().request(mutatedRequest).build())
                .then(Mono.fromRunnable(() -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("[{}] <-- {} {}ms",
                            correlationId,
                            exchange.getResponse().getStatusCode(),
                            duration);
                }));
    }

    @Override
    public int getOrder() {
        // Run before JwtAuthFilter so correlation ID is set first
        return Ordered.HIGHEST_PRECEDENCE;
    }
}

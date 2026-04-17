package com.nexabank.gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RequestLoggingFilterTest {

    private RequestLoggingFilter loggingFilter;
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        loggingFilter = new RequestLoggingFilter();
        chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());
    }

    @Test
    void injectsCorrelationIdHeader() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/accounts")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        loggingFilter.filter(exchange, chain).block();

        verify(chain).filter(argThat(ex ->
                ex.getRequest().getHeaders().containsKey("X-Correlation-Id")
        ));
    }

    @Test
    void correlationIdIsUuid() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/transactions")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        loggingFilter.filter(exchange, chain).block();

        verify(chain).filter(argThat(ex -> {
            String correlationId = ex.getRequest().getHeaders().getFirst("X-Correlation-Id");
            assertThat(correlationId).matches(
                    "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
            );
            return true;
        }));
    }

    @Test
    void hasHighestPrecedenceOrder() {
        assertThat(loggingFilter.getOrder()).isEqualTo(Integer.MIN_VALUE);
    }

    @Test
    void chainIsAlwaysCalled() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/health").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        loggingFilter.filter(exchange, chain).block();

        verify(chain, times(1)).filter(any());
    }
}

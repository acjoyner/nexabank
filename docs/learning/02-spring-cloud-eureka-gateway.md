# Spring Cloud — Eureka, Config Server, API Gateway

> **Paste into Ollama/Open WebUI** for AI-assisted learning on this topic.

## The Three Infrastructure Services

### 1. Eureka Server (Service Discovery)
**Port:** 8761 | **File:** `infrastructure/eureka-server/`

Think of Eureka as a phone book for microservices. Instead of hardcoding `http://account-service:8081`, services register their name and address with Eureka. Other services look up the name to get the address.

- All services have `@EnableDiscoveryClient` — they register at startup
- Feign clients use `name = "account-service"` which Eureka resolves
- Dashboard: `http://localhost:8761` shows all registered services

### 2. Config Server (Centralized Configuration)
**Port:** 8888 | **File:** `infrastructure/config-server/`

Instead of each service having its own `application.properties`, all configuration lives in `infrastructure/config-server/src/main/resources/config/`.

Services bootstrap with:
```yaml
spring:
  config:
    import: "configserver:http://localhost:8888"
```

Then fetch `http://config-server:8888/account-service/default` which returns the contents of `config/account-service.yml`.

**Why this matters:** Change a database URL in one place (`account-service.yml`) — no need to rebuild/redeploy the service JAR.

### 3. API Gateway
**Port:** 8080 | **File:** `infrastructure/api-gateway/`

The single entry point for all client traffic. Responsibilities:
1. **JWT Authentication** — `JwtAuthFilter` validates Bearer tokens
2. **Routing** — routes `/api/accounts/**` to account-service, etc.
3. **Rate Limiting** — prevents API abuse
4. **CORS** — allows Angular at `localhost:4200`
5. **Correlation IDs** — `RequestLoggingFilter` assigns UUID per request

**Important:** API Gateway uses **Spring Cloud Gateway** which is reactive (WebFlux). It does NOT use Spring MVC. All filters are reactive (`GatewayFilter`, `GlobalFilter`).

## Route Configuration
**File:** `infrastructure/config-server/src/main/resources/config/api-gateway.yml`

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: auth-routes
          uri: lb://account-service    # lb:// = load-balanced via Eureka
          predicates:
            - Path=/api/auth/**        # matches these paths
          # No JwtAuthFilter — public endpoint

        - id: account-routes
          uri: lb://account-service
          predicates:
            - Path=/api/accounts/**
          filters:
            - JwtAuthFilter            # JWT required
```

`lb://account-service` — the `lb://` prefix tells Spring Cloud to use load balancing. It looks up "account-service" in Eureka, gets the actual IP, and routes there.

## Startup Order
Services must start in this order (handled by Docker Compose `depends_on`):
1. Eureka Server
2. Config Server (needs Eureka to register)
3. API Gateway (needs Config Server for routes)
4. Business services (need all three above)

## Interview Talking Points
- **Why Config Server?** Externalized configuration is a 12-Factor App principle. It allows environment-specific config (dev/staging/prod) without changing code.
- **Why API Gateway?** Single entry point simplifies security (one place to validate JWT), CORS configuration, and rate limiting instead of duplicating in every service.
- **What is service mesh vs. API Gateway?** API Gateway handles external traffic (north-south). Service mesh (Istio, Linkerd) handles internal service-to-service traffic (east-west). This app uses API Gateway for simplicity.
- **How does the gateway forward identity to services?** After JWT validation, it sets `X-User-Email` and `X-User-Roles` headers on the forwarded request. Downstream services read these headers instead of parsing the JWT again.

## Questions to Ask Your AI
- "What is the difference between Eureka and Consul for service discovery?"
- "How does Spring Cloud Config Server handle different environments (dev/prod)?"
- "What is the difference between Spring MVC and Spring WebFlux?"
- "How does load balancing work with the lb:// prefix in gateway routes?"
- "What happens if the Config Server is down when a service starts?"

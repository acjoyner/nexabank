# JWT + Spring Security in NexaBank

> **Paste into Ollama/Open WebUI** for AI-assisted learning on this topic.

## How JWT Works in This Architecture

```
1. User registers/logs in → account-service issues JWT
2. Angular stores JWT in localStorage
3. JWT interceptor attaches "Authorization: Bearer {token}" to every request
4. API Gateway JwtAuthFilter validates JWT
5. Gateway extracts claims and forwards:
   - X-User-Email: john@example.com
   - X-User-Roles: ROLE_CUSTOMER
   - X-User-Id: 42
6. Downstream services trust these headers — no JWT re-validation needed
```

## JWT Structure
A JWT has 3 parts separated by dots: `header.payload.signature`

Payload claims in this app:
```json
{
  "sub": "john@example.com",
  "userId": "42",
  "roles": "ROLE_CUSTOMER",
  "iat": 1702000000,
  "exp": 1702086400
}
```

## Key Code

### JWT Issuance (account-service)
**File:** `services/account-service/src/main/java/com/nexabank/account/service/JwtService.java`

Uses JJWT 0.12.x API — modern builder style:
```java
Jwts.builder()
    .subject(email)
    .claim("userId", userId.toString())
    .claim("roles", "ROLE_CUSTOMER")
    .issuedAt(Date.from(now))
    .expiration(Date.from(now.plusMillis(86400000)))  // 24 hours
    .signWith(Keys.hmacShaKeyFor(secret.getBytes()))
    .compact();
```

**Important:** The secret must be at least 256 bits (32 characters) for HMAC-SHA256. Shorter secrets cause JJWT to throw `WeakKeyException`.

### JWT Validation (api-gateway)
**File:** `infrastructure/api-gateway/src/main/java/com/nexabank/gateway/filter/JwtAuthFilter.java`

```java
Claims claims = Jwts.parser()
    .verifyWith(key)
    .build()
    .parseSignedClaims(token)
    .getPayload();
```

If the token is expired, tampered with, or uses a different secret, JJWT throws `JwtException` and the filter returns 401.

### Security Config (account-service)
**File:** `services/account-service/src/main/java/com/nexabank/account/config/SecurityConfig.java`

```java
.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
```

`STATELESS` — no HTTP session created. Every request must carry the JWT. This is the correct pattern for microservices.

### Angular JWT Interceptor
**File:** `frontend/nexabank-ui/src/app/core/interceptors/jwt.interceptor.ts`

```typescript
export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const token = localStorage.getItem('nexabank_token');
  if (token) {
    req = req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
  }
  return next(req);
};
```

Functional interceptor (Angular 17+) — no class needed. The `HttpInterceptorFn` type is the modern approach.

## Security Considerations
- **Don't store JWT in localStorage in production** — use HttpOnly cookies to prevent XSS. localStorage is used here for demo simplicity.
- **JWT expiry** is set to 24 hours. Production systems use shorter-lived access tokens (15 min) + refresh tokens.
- **The JWT secret** should be injected via environment variable (`${JWT_SECRET}`) — never hardcoded in properties files.

## Interview Talking Points
- **Stateless vs. stateful authentication?** JWT is stateless — the server doesn't store sessions. Any service instance can validate a JWT independently.
- **How does the gateway forward identity?** Gateway extracts claims from JWT and forwards as HTTP headers (`X-User-Email`, `X-User-Roles`). Downstream services trust these without re-parsing the JWT.
- **What happens when the token expires?** The `errorInterceptor` in Angular catches 401 responses, clears localStorage, and redirects to login.
- **HMAC vs. RSA signing?** HMAC (symmetric) uses the same secret to sign and verify — simpler but the secret must be shared. RSA (asymmetric) uses private key to sign, public key to verify — better for multi-service architectures where you don't want all services to have the signing secret.

## Questions to Ask Your AI
- "What is the difference between authentication and authorization?"
- "How would you implement refresh tokens in this architecture?"
- "Why is SessionCreationPolicy.STATELESS important for microservices?"
- "What is the vulnerability of storing JWT in localStorage?"
- "How would I add role-based access (admin vs. customer) to the gateway filter?"

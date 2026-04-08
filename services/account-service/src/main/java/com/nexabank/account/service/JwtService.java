package com.nexabank.account.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * JWT Service — Token generation and validation.
 *
 * Uses JJWT 0.12.x API (modern builder-style).
 * The secret must be at least 256 bits (32 chars) for HMAC-SHA256.
 *
 * Token contains:
 * - subject: customer email
 * - userId: customer DB ID
 * - roles: ROLE_CUSTOMER (future: ROLE_ADMIN)
 * - iat / exp: issued-at / expiration
 *
 * See docs/learning/07-jwt-spring-security.md
 */
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration-ms:86400000}")
    private long expirationMs;

    public String generateToken(Long userId, String email) {
        SecretKey key = getSigningKey();
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(email)
                .claim("userId", userId.toString())
                .claim("roles", "ROLE_CUSTOMER")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(key)
                .compact();
    }

    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Instant getExpirationInstant() {
        return Instant.now().plusMillis(expirationMs);
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
}

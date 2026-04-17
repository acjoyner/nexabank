package com.nexabank.account.service;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret",
                "test-secret-at-least-256-bits-long-for-testing-purposes-only!!");
        ReflectionTestUtils.setField(jwtService, "expirationMs", 3_600_000L);
    }

    @Test
    void generateToken_returnsNonBlankJwt() {
        String token = jwtService.generateToken(1L, "alice@nexabank.com");
        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    void extractClaims_subjectMatchesEmail() {
        String token = jwtService.generateToken(42L, "bob@nexabank.com");
        Claims claims = jwtService.extractClaims(token);
        assertThat(claims.getSubject()).isEqualTo("bob@nexabank.com");
    }

    @Test
    void extractClaims_userIdMatchesInput() {
        String token = jwtService.generateToken(99L, "carol@nexabank.com");
        Claims claims = jwtService.extractClaims(token);
        assertThat(claims.get("userId", Long.class)).isEqualTo(99L);
    }

    @Test
    void getExpirationInstant_isFutureDate() {
        Instant expiry = jwtService.getExpirationInstant();
        assertThat(expiry).isAfter(Instant.now());
    }

    @Test
    void extractClaims_onTamperedToken_throwsException() {
        assertThatThrownBy(() -> jwtService.extractClaims("bad.token.here"))
                .isInstanceOf(Exception.class);
    }

    @Test
    void twoTokensForSameUser_areNotEqual() {
        String t1 = jwtService.generateToken(1L, "dave@nexabank.com");
        String t2 = jwtService.generateToken(1L, "dave@nexabank.com");
        assertThat(t1).isNotEqualTo(t2);
    }
}

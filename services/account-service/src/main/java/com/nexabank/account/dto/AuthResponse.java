package com.nexabank.account.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class AuthResponse {
    private String token;
    private String tokenType;
    private Instant expiresAt;
    private Long customerId;
    private String email;
    private String fullName;
}

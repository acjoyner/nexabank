package com.nexabank.account.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexabank.account.dto.AuthResponse;
import com.nexabank.account.dto.LoginRequest;
import com.nexabank.account.dto.RegisterRequest;
import com.nexabank.account.exception.DuplicateAccountException;
import com.nexabank.account.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean  private AuthService authService;

    @Test
    @WithMockUser
    void register_validRequest_returns200WithToken() throws Exception {
        AuthResponse response = AuthResponse.builder()
                .token("jwt-token").tokenType("Bearer")
                .expiresAt(Instant.now().plusSeconds(3600))
                .customerId(1L).email("alice@nexabank.com").fullName("Alice Smith")
                .build();
        when(authService.register(any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegisterRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.email").value("alice@nexabank.com"));
    }

    @Test
    @WithMockUser
    void register_duplicateEmail_returns409() throws Exception {
        when(authService.register(any()))
                .thenThrow(new DuplicateAccountException("Email already registered"));

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegisterRequest())))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser
    void register_blankEmail_returns400() throws Exception {
        RegisterRequest req = validRegisterRequest();
        req.setEmail("");

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void login_validCredentials_returns200WithToken() throws Exception {
        AuthResponse response = AuthResponse.builder()
                .token("login-jwt").tokenType("Bearer")
                .expiresAt(Instant.now().plusSeconds(3600))
                .customerId(2L).email("bob@nexabank.com").fullName("Bob Jones")
                .build();
        when(authService.login(any())).thenReturn(response);

        LoginRequest req = new LoginRequest();
        req.setEmail("bob@nexabank.com");
        req.setPassword("Password1!");

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("login-jwt"));
    }

    @Test
    @WithMockUser
    void login_badCredentials_returns401() throws Exception {
        when(authService.login(any()))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        LoginRequest req = new LoginRequest();
        req.setEmail("bad@nexabank.com");
        req.setPassword("wrongpass");

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    private RegisterRequest validRegisterRequest() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("alice@nexabank.com");
        req.setPassword("Password1!");
        req.setFirstName("Alice");
        req.setLastName("Smith");
        req.setPhone("555-0100");
        return req;
    }
}

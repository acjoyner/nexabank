package com.nexabank.account.service;

import com.nexabank.account.dto.AuthResponse;
import com.nexabank.account.dto.LoginRequest;
import com.nexabank.account.dto.RegisterRequest;
import com.nexabank.account.exception.DuplicateAccountException;
import com.nexabank.account.model.*;
import com.nexabank.account.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private CustomerRepository customerRepository;
    @Mock private AccountNumberFactory accountNumberFactory;
    @Mock private JwtService jwtService;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;

    private PasswordEncoder passwordEncoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        authService = new AuthService(customerRepository, accountNumberFactory,
                jwtService, passwordEncoder, kafkaTemplate);
    }

    @Test
    void register_createsCustomerAndReturnsJwt() {
        RegisterRequest req = registerRequest("alice@nexabank.com", "Password1!");
        when(customerRepository.existsByEmail("alice@nexabank.com")).thenReturn(false);
        when(accountNumberFactory.generate(AccountType.CHECKING)).thenReturn("CHK-0000001001");
        when(customerRepository.save(any())).thenAnswer(inv -> {
            Customer c = inv.getArgument(0);
            c.setId(1L);
            if (c.getAccounts() == null) c.setAccounts(new ArrayList<>());
            if (c.getAccounts().isEmpty()) {
                Account a = Account.builder().id(100L).accountNumber("CHK-0000001001")
                        .accountType(AccountType.CHECKING).status(AccountStatus.ACTIVE).build();
                c.getAccounts().add(a);
            }
            return c;
        });
        when(jwtService.generateToken(anyLong(), anyString())).thenReturn("mock-jwt-token");
        when(jwtService.getExpirationInstant()).thenReturn(Instant.now().plusSeconds(3600));

        AuthResponse response = authService.register(req);

        assertThat(response.getToken()).isEqualTo("mock-jwt-token");
        assertThat(response.getEmail()).isEqualTo("alice@nexabank.com");
        verify(kafkaTemplate).send(eq("nexabank.account.created"), eq("alice@nexabank.com"), any());
    }

    @Test
    void register_duplicateEmail_throwsDuplicateAccountException() {
        when(customerRepository.existsByEmail("dup@nexabank.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest("dup@nexabank.com", "Pass1!")))
                .isInstanceOf(DuplicateAccountException.class)
                .hasMessageContaining("dup@nexabank.com");
    }

    @Test
    void register_passwordIsHashed() {
        RegisterRequest req = registerRequest("bob@nexabank.com", "ClearTextPass!");
        when(customerRepository.existsByEmail(anyString())).thenReturn(false);
        when(accountNumberFactory.generate(any())).thenReturn("CHK-0000001002");
        when(customerRepository.save(any())).thenAnswer(inv -> {
            Customer c = inv.getArgument(0);
            c.setId(2L);
            if (c.getAccounts() == null) c.setAccounts(new ArrayList<>());
            Account a = Account.builder().id(101L).accountNumber("CHK-0000001002")
                    .accountType(AccountType.CHECKING).status(AccountStatus.ACTIVE).build();
            c.getAccounts().add(a);
            return c;
        });
        when(jwtService.generateToken(anyLong(), anyString())).thenReturn("token");
        when(jwtService.getExpirationInstant()).thenReturn(Instant.now().plusSeconds(3600));

        authService.register(req);

        ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository, atLeastOnce()).save(captor.capture());
        Customer saved = captor.getAllValues().get(0);
        assertThat(saved.getPasswordHash()).isNotEqualTo("ClearTextPass!");
        assertThat(passwordEncoder.matches("ClearTextPass!", saved.getPasswordHash())).isTrue();
    }

    @Test
    void login_validCredentials_returnsJwt() {
        String encoded = passwordEncoder.encode("mySecret1!");
        Customer customer = Customer.builder()
                .id(5L).email("carol@nexabank.com")
                .passwordHash(encoded).firstName("Carol").lastName("Jones")
                .build();
        when(customerRepository.findByEmail("carol@nexabank.com")).thenReturn(Optional.of(customer));
        when(jwtService.generateToken(5L, "carol@nexabank.com")).thenReturn("carol-jwt");
        when(jwtService.getExpirationInstant()).thenReturn(Instant.now().plusSeconds(3600));

        LoginRequest req = new LoginRequest();
        req.setEmail("carol@nexabank.com");
        req.setPassword("mySecret1!");

        AuthResponse response = authService.login(req);

        assertThat(response.getToken()).isEqualTo("carol-jwt");
        assertThat(response.getCustomerId()).isEqualTo(5L);
    }

    @Test
    void login_unknownEmail_throwsBadCredentials() {
        when(customerRepository.findByEmail("unknown@nexabank.com")).thenReturn(Optional.empty());

        LoginRequest req = new LoginRequest();
        req.setEmail("unknown@nexabank.com");
        req.setPassword("anything");

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_wrongPassword_throwsBadCredentials() {
        Customer customer = Customer.builder()
                .id(6L).email("dave@nexabank.com")
                .passwordHash(passwordEncoder.encode("correctPass!"))
                .firstName("Dave").lastName("Lee").build();
        when(customerRepository.findByEmail("dave@nexabank.com")).thenReturn(Optional.of(customer));

        LoginRequest req = new LoginRequest();
        req.setEmail("dave@nexabank.com");
        req.setPassword("wrongPass!");

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadCredentialsException.class);
    }

    private RegisterRequest registerRequest(String email, String password) {
        RegisterRequest req = new RegisterRequest();
        req.setEmail(email);
        req.setPassword(password);
        req.setFirstName("Test");
        req.setLastName("User");
        req.setPhone("555-1234");
        return req;
    }
}

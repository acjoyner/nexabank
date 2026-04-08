package com.nexabank.account.controller;

import com.nexabank.account.dto.AccountRequest;
import com.nexabank.account.dto.AccountResponse;
import com.nexabank.account.dto.BalanceResponse;
import com.nexabank.account.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

import java.util.List;

/**
 * Account Controller — protected endpoints (JWT required, validated at gateway).
 *
 * User identity is injected by the gateway as X-User-Id and X-User-Email headers.
 * This controller trusts those headers — it does NOT re-validate the JWT.
 */
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Tag(name = "Accounts", description = "Bank account management")
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "List all accounts for a customer")
    public ResponseEntity<List<AccountResponse>> getAccounts(@PathVariable Long customerId) {
        return ResponseEntity.ok(accountService.getAccountsByCustomer(customerId));
    }

    @GetMapping("/{accountId}")
    @Operation(summary = "Get account details")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable Long accountId) {
        return ResponseEntity.ok(accountService.getAccount(accountId));
    }

    @GetMapping("/{accountId}/balance")
    @Operation(summary = "Get account balance — also called by transaction-service via Feign")
    public ResponseEntity<BalanceResponse> getBalance(@PathVariable Long accountId) {
        return ResponseEntity.ok(accountService.getBalance(accountId));
    }

    /**
     * Internal endpoint — called by transaction-service via Feign (not through gateway).
     * Updates the persisted balance after a deposit, withdrawal, or transfer.
     */
    @PutMapping("/{accountId}/balance")
    @Operation(summary = "Update account balance — called internally by transaction-service")
    public ResponseEntity<BalanceResponse> updateBalance(
            @PathVariable Long accountId,
            @RequestBody Map<String, BigDecimal> body) {
        BigDecimal newBalance = body.get("balance");
        return ResponseEntity.ok(accountService.updateBalance(accountId, newBalance));
    }

    @PostMapping("/customer/{customerId}")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Open a new bank account for a customer")
    public ResponseEntity<AccountResponse> openAccount(
            @PathVariable Long customerId,
            @Valid @RequestBody AccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(accountService.openAccount(customerId, request));
    }
}

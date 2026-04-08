package com.nexabank.transaction.controller;

import com.nexabank.transaction.dto.*;
import com.nexabank.transaction.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Transaction Controller.
 *
 * The X-User-Email header is injected by the API Gateway's JwtAuthFilter
 * and forwarded here — used as the `createdBy` audit field on transactions.
 */
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Money movement operations")
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/deposit")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Deposit funds into an account")
    public ResponseEntity<TransactionResponse> deposit(
            @Valid @RequestBody DepositRequest request,
            @RequestHeader(value = "X-User-Email", defaultValue = "system") String userEmail) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.deposit(request, userEmail));
    }

    @PostMapping("/withdrawal")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Withdraw funds from an account")
    public ResponseEntity<TransactionResponse> withdrawal(
            @Valid @RequestBody WithdrawalRequest request,
            @RequestHeader(value = "X-User-Email", defaultValue = "system") String userEmail) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.withdrawal(request, userEmail));
    }

    @PostMapping("/transfer")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Transfer funds between accounts")
    public ResponseEntity<TransactionResponse> transfer(
            @Valid @RequestBody TransferRequest request,
            @RequestHeader(value = "X-User-Email", defaultValue = "system") String userEmail) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.transfer(request, userEmail));
    }

    @GetMapping("/account/{accountId}")
    @Operation(summary = "Get transaction history for an account")
    public ResponseEntity<List<TransactionResponse>> getTransactions(
            @PathVariable Long accountId) {
        return ResponseEntity.ok(transactionService.getTransactionsByAccount(accountId));
    }
}

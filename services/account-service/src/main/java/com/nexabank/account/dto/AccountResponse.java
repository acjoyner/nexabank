package com.nexabank.account.dto;

import com.nexabank.account.model.AccountStatus;
import com.nexabank.account.model.AccountType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class AccountResponse {
    private Long id;
    private String accountNumber;
    private AccountType accountType;
    private BigDecimal balance;
    private AccountStatus status;
    private BigDecimal interestRate;
    private Instant openedAt;
    private Long customerId;
    private String customerEmail;
}

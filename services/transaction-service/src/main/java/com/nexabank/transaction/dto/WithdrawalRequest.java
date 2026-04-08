package com.nexabank.transaction.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class WithdrawalRequest {

    @NotNull
    private Long accountId;

    @NotNull
    @DecimalMin(value = "0.01", message = "Withdrawal amount must be at least $0.01")
    private BigDecimal amount;

    private String description;
}

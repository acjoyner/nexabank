package com.nexabank.transaction.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransferRequest {

    @NotNull
    private Long sourceAccountId;

    @NotNull
    private Long destAccountId;

    @NotNull
    @DecimalMin(value = "0.01", message = "Transfer amount must be at least $0.01")
    private BigDecimal amount;

    private String description;
}

package com.nexabank.loan.dto;

import com.nexabank.loan.model.LoanType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class LoanApplicationRequest {

    @NotNull
    private Long customerId;

    @NotNull
    private Long accountId;

    @NotNull
    private LoanType loanType;

    @NotNull
    @DecimalMin(value = "1000.00", message = "Minimum loan amount is $1,000")
    @DecimalMax(value = "1000000.00", message = "Maximum loan amount is $1,000,000")
    private BigDecimal requestedAmount;

    @NotNull
    @Min(value = 12, message = "Minimum term is 12 months")
    @Max(value = 360, message = "Maximum term is 360 months (30 years)")
    private Integer termMonths;

    @NotNull
    @DecimalMin(value = "10000.00", message = "Minimum annual income is $10,000")
    private BigDecimal annualIncome;

    @Min(value = 300, message = "Credit score must be at least 300")
    @Max(value = 850, message = "Credit score cannot exceed 850")
    private Integer creditScore;
}

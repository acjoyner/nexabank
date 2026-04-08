package com.nexabank.account.dto;

import com.nexabank.account.model.AccountStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class BalanceResponse {
    private Long accountId;
    private String accountNumber;
    private BigDecimal balance;
    private AccountStatus status;

    public static BalanceResponse unavailable() {
        return BalanceResponse.builder()
                .balance(BigDecimal.ZERO)
                .status(AccountStatus.FROZEN)
                .build();
    }
}

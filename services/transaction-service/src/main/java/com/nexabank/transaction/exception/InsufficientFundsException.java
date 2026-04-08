package com.nexabank.transaction.exception;

import java.math.BigDecimal;

public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(Long accountId, BigDecimal available, BigDecimal requested) {
        super(String.format(
            "Insufficient funds in account %d: available $%.2f, requested $%.2f",
            accountId, available, requested
        ));
    }
}

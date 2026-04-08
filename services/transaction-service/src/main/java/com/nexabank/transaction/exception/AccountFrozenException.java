package com.nexabank.transaction.exception;

public class AccountFrozenException extends RuntimeException {

    public AccountFrozenException(Long accountId) {
        super("Account " + accountId + " is frozen and cannot process transactions");
    }
}

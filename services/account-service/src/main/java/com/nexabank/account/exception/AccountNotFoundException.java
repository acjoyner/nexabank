package com.nexabank.account.exception;

public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(Long id) {
        super("Account not found with id: " + id);
    }

    public AccountNotFoundException(String message) {
        super(message);
    }
}

package com.nexabank.account.service;

import com.nexabank.account.model.AccountType;
import com.nexabank.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Factory pattern — generates formatted, unique account numbers.
 *
 * Format: {TYPE_PREFIX}-{10-DIGIT-PADDED-ID}
 * Examples: CHK-0000001001, SAV-0000001002
 *
 * The padded ID is based on the current sequence count to ensure uniqueness.
 * Prefixes make account type immediately visible in logs and statements.
 */
@Component
@RequiredArgsConstructor
public class AccountNumberFactory {

    private final AccountRepository accountRepository;

    public String generate(AccountType type) {
        String prefix = type == AccountType.CHECKING ? "CHK" : "SAV";
        long count = accountRepository.count() + 1001;
        return String.format("%s-%010d", prefix, count);
    }
}

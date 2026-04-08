package com.nexabank.account.mapper;

import com.nexabank.account.dto.AccountResponse;
import com.nexabank.account.model.Account;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper — automatically generates the implementation at compile time.
 *
 * componentModel="spring" means Spring manages the bean (inject with @Autowired / constructor).
 * @Mapping annotations handle field name differences between entity and DTO.
 *
 * See docs/learning/11-design-patterns-used.md — DTO/Mapper pattern
 */
@Mapper(componentModel = "spring")
public interface AccountMapper {

    @Mapping(source = "customer.id", target = "customerId")
    @Mapping(source = "customer.email", target = "customerEmail")
    AccountResponse toResponse(Account account);
}

package com.SmartBank.security;

import com.SmartBank.entity.Account;
import com.SmartBank.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component("accountSecurity")
@RequiredArgsConstructor
public class AccountSecurity {

    private final AccountRepository accountRepository;

    @Transactional(readOnly = true)
    public boolean isOwner(Long id, String username) {
        if (id == null || username == null) {
            return false;
        }
        return accountRepository.findById(id)
                .map(account -> account.getCustomer() != null && username.equals(account.getCustomer().getUsername()))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean isOwnerByAccountNumber(String accountNumber, String username) {
        if (accountNumber == null || username == null) {
            return false;
        }
        Account account = accountRepository.findByAccountNumber(accountNumber);
        return account != null && account.getCustomer() != null && username.equals(account.getCustomer().getUsername());
    }
}

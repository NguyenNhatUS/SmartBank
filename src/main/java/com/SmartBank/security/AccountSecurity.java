package com.SmartBank.security;

import com.SmartBank.entity.Account;
import com.SmartBank.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component("accountSecurity")
@RequiredArgsConstructor
public class AccountSecurity {

    private final AccountRepository accountRepository;

    @Transactional(readOnly = true)
    public boolean isOwner(Long id, Authentication authentication) {
        if (id == null || !isCustomer(authentication)) {
            return false;
        }
        return accountRepository.findById(id)
                .map(account -> account.getCustomer() != null && authentication.getName().equals(account.getCustomer().getUsername()))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean isOwnerByAccountNumber(String accountNumber, Authentication authentication) {
        if (accountNumber == null || !isCustomer(authentication)) {
            return false;
        }
        Account account = accountRepository.findByAccountNumber(accountNumber);
        return account != null && account.getCustomer() != null && authentication.getName().equals(account.getCustomer().getUsername());
    }

    private boolean isCustomer(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated()
                && authentication.getName() != null
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_CUSTOMER".equals(authority.getAuthority()));
    }
}

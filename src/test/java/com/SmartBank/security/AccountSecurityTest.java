package com.SmartBank.security;

import com.SmartBank.entity.Account;
import com.SmartBank.entity.Customer;
import com.SmartBank.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AccountSecurityTest {
    private final AccountRepository repository = mock(AccountRepository.class);
    private final AccountSecurity security = new AccountSecurity(repository);

    @ParameterizedTest
    @ValueSource(strings = {"EMPLOYEE", "ADMIN", "UNKNOWN"})
    void matchingUsernameDoesNotMakeNonCustomerAnOwner(String role) {
        var authentication = new UsernamePasswordAuthenticationToken("alice", null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        assertThat(security.isOwner(1L, authentication)).isFalse();
        assertThat(security.isOwnerByAccountNumber("100", authentication)).isFalse();
        verifyNoInteractions(repository);
    }

    @Test
    void customerOwnershipRequiresMatchingAccountAndAuthenticatedIdentity() {
        var authentication = new UsernamePasswordAuthenticationToken("alice", null,
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
        Account account = Account.builder().customer(Customer.builder().username("alice").build()).build();
        when(repository.findById(1L)).thenReturn(Optional.of(account));
        when(repository.findByAccountNumber("100")).thenReturn(account);
        assertThat(security.isOwner(1L, authentication)).isTrue();
        assertThat(security.isOwnerByAccountNumber("100", authentication)).isTrue();
        assertThat(security.isOwner(2L, authentication)).isFalse();
        assertThat(security.isOwnerByAccountNumber("200", authentication)).isFalse();
        account.getCustomer().setUsername("bob");
        assertThat(security.isOwner(1L, authentication)).isFalse();
        assertThat(security.isOwnerByAccountNumber("100", authentication)).isFalse();
        assertThat(security.isOwner(1L, null)).isFalse();
        assertThat(security.isOwnerByAccountNumber("100", null)).isFalse();
        authentication.setAuthenticated(false);
        assertThat(security.isOwner(1L, authentication)).isFalse();
        assertThat(security.isOwnerByAccountNumber("100", authentication)).isFalse();
    }
}

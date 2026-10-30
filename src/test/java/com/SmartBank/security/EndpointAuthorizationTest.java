package com.SmartBank.security;

import com.SmartBank.config.SecurityConfig;
import com.SmartBank.controller.v1.*;
import com.SmartBank.entity.*;
import com.SmartBank.entity.enums.*;
import com.SmartBank.exception.GlobalExceptionHandler;
import com.SmartBank.mapper.TransactionMapper;
import com.SmartBank.repository.*;
import com.SmartBank.security.handler.*;
import com.SmartBank.service.*;
import com.SmartBank.service.impl.AuthServiceImpl;
import com.SmartBank.service.impl.TransactionServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Real JWT, filter chain, method security, owner/state checks and transaction/auth services.
// Repositories are mocked: this verifies authorization, not database locking or Redis TTL.
@WebMvcTest(controllers = {AccountController.class, TransactionController.class,
        CustomerController.class, AuthController.class}, properties = {
        "jwt.secret=test-only-signing-key-with-at-least-32-bytes-long",
        "jwt.expiration=900000", "jwt.refresh-expiration=604800000"
})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtTokenProvider.class,
        UserAccessValidator.class, AccountSecurity.class, CustomAuthenticationEntryPoint.class,
        CustomAccessDeniedHandler.class, GlobalExceptionHandler.class,
        TransactionServiceImpl.class, AuthServiceImpl.class})
class EndpointAuthorizationTest {
    @Autowired MockMvc mvc;
    @Autowired JwtTokenProvider jwt;
    @MockBean CustomerRepository customers;
    @MockBean EmployeeRepository employees;
    @MockBean AccountRepository accounts;
    @MockBean TransactionRepository transactions;
    @MockBean RefreshTokenRepository refreshTokens;
    @MockBean TokenBlacklistService blacklist;
    @MockBean CustomUserDetailsService userDetails;
    @MockBean AccountService accountService;
    @MockBean CustomerService customerService;
    @MockBean TransactionMapper transactionMapper;

    @ParameterizedTest
    @CsvSource({"CUSTOMER,alice,200", "CUSTOMER,bob,403", "EMPLOYEE,bob,200", "ADMIN,bob,200"})
    void accountDetailAllowsStaffOrCustomerOwner(String role, String owner, int status) throws Exception {
        String bearer = bearer("alice", role);
        when(accounts.findById(1L)).thenReturn(Optional.of(account(owner, "100", 1L)));
        mvc.perform(get("/api/v1/accounts/1").header("Authorization", bearer))
                .andExpect(status().is(status));
        if (status == 200) verify(accountService).getByID(1L);
        else verifyNoInteractions(accountService);
    }

    @ParameterizedTest
    @CsvSource({"CUSTOMER,alice,200", "CUSTOMER,bob,403", "EMPLOYEE,bob,200", "ADMIN,bob,200"})
    void statementAllowsStaffOrCustomerOwner(String role, String owner, int status) throws Exception {
        String bearer = bearer("alice", role);
        when(accounts.findByAccountNumber("100")).thenReturn(account(owner, "100", 1L));
        when(accounts.existsByAccountNumber("100")).thenReturn(true);
        when(transactions.findBySourceAccount_AccountNumberOrTargetAccount_AccountNumber(eq("100"), eq("100"), any()))
                .thenReturn(org.springframework.data.domain.Page.empty());
        mvc.perform(get("/api/v1/transactions/account/100").header("Authorization", bearer))
                .andExpect(status().is(status));
        if (status == 403) verifyNoInteractions(transactions);
    }

    @ParameterizedTest
    @CsvSource({"deposit,CUSTOMER,alice,201", "withdraw,CUSTOMER,alice,201", "transfer,CUSTOMER,alice,201",
            "deposit,CUSTOMER,bob,403", "withdraw,CUSTOMER,bob,403", "transfer,CUSTOMER,bob,403",
            "deposit,EMPLOYEE,bob,201", "withdraw,EMPLOYEE,bob,201", "transfer,EMPLOYEE,bob,201",
            "deposit,ADMIN,bob,201", "withdraw,ADMIN,bob,201", "transfer,ADMIN,bob,201"})
    void moneyOperationsEnforceOwnershipBeforeAnyWrite(String operation, String role, String owner, int status) throws Exception {
        String bearer = bearer("alice", role);
        Account source = account(owner, "100", 1L);
        Account target = account("bob", "200", 2L);
        when(accounts.findByAccountNumber("100")).thenReturn(source);
        when(accounts.findByAccountNumberWithLock("100")).thenReturn(source);
        when(accounts.findByAccountNumberWithLock("200")).thenReturn(target);
        String body = operation.equals("transfer")
                ? "{\"sourceAccountNumber\":\"100\",\"targetAccountNumber\":\"200\",\"amount\":1000}"
                : "{\"accountNumber\":\"100\",\"amount\":1000}";

        var result = mvc.perform(post("/api/v1/transactions/" + operation)
                .header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().is(status));
        if (status == 403) {
            result.andExpect(jsonPath("code").value(1003));
            verify(accounts, never()).findByAccountNumberWithLock(anyString());
            verify(accounts, never()).save(any());
            verifyNoInteractions(transactions);
            assertThat(source.getBalance()).isEqualByComparingTo("10000");
        } else {
            verify(transactions).save(any(Transaction.class));
            assertThat(source.getBalance()).isEqualByComparingTo(operation.equals("deposit") ? "11000" : "9000");
            if (operation.equals("transfer")) assertThat(target.getBalance()).isEqualByComparingTo("11000");
        }
    }

    @ParameterizedTest
    @CsvSource({"CUSTOMER,200", "EMPLOYEE,403", "ADMIN,403"})
    void myAccountsRemainsCustomerOnly(String role, int status) throws Exception {
        mvc.perform(get("/api/v1/accounts/my").header("Authorization", bearer("alice", role)))
                .andExpect(status().is(status));
    }

    @ParameterizedTest
    @CsvSource({"CUSTOMER,403,403", "EMPLOYEE,200,403", "ADMIN,200,200"})
    void listPoliciesRemainStaffForAccountsAndAdminForCustomers(String role, int accountStatus, int customerStatus) throws Exception {
        String token = bearer("alice", role);
        mvc.perform(get("/api/v1/accounts").header("Authorization", token)).andExpect(status().is(accountStatus));
        mvc.perform(get("/api/v1/customers").header("Authorization", token)).andExpect(status().is(customerStatus));
    }

    @Test
    void missingOrMalformedTokenReturns401() throws Exception {
        mvc.perform(get("/api/v1/accounts/1")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/accounts/1").header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("code").value(1105));
        verifyNoInteractions(accountService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"LOCKED", "DISABLED", "EMPLOYEE_DISABLED", "ADMIN_DISABLED"})
    void existingAccessAndRefreshTokensStopWorkingAfterUserIsDisabled(String state) throws Exception {
        String role = state.startsWith("EMPLOYEE") ? "EMPLOYEE" : state.startsWith("ADMIN") ? "ADMIN" : "CUSTOMER";
        String token = bearer("alice", role);
        RefreshToken refresh = refresh("alice", role);
        when(accounts.findById(1L)).thenReturn(Optional.of(account("alice", "100", 1L)));
        mvc.perform(get("/api/v1/accounts/1").header("Authorization", token)).andExpect(status().isOk());
        clearInvocations(accountService);
        if (role.equals("CUSTOMER")) {
            Customer user = customers.findByUsername("alice").orElseThrow();
            if (state.equals("LOCKED")) user.setStatus(CustomerStatus.LOCKED);
            else user.setEnabled(false);
        } else {
            employees.findByUsername("alice").orElseThrow().setEnabled(false);
        }

        mvc.perform(get("/api/v1/accounts/1").header("Authorization", token))
                .andExpect(status().isForbidden()).andExpect(jsonPath("code").value(1104));
        mvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"refresh\"}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("code").value(1104));
        verifyNoInteractions(accountService);
        verify(refreshTokens, never()).save(any());
        assertThat(refresh.isRevoked()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"DELETED", "ROLE_CHANGED", "DUPLICATE"})
    void staleOrAmbiguousIdentityRejectsBothTokenTypes(String state) throws Exception {
        String role = state.equals("ROLE_CHANGED") ? "ADMIN" : "CUSTOMER";
        String token = bearer("alice", role);
        refresh("alice", role);
        if (state.equals("DELETED")) when(customers.findByUsername("alice")).thenReturn(Optional.empty());
        if (state.equals("ROLE_CHANGED")) employees.findByUsername("alice").orElseThrow().setRole(Role.EMPLOYEE);
        if (state.equals("DUPLICATE")) when(employees.findByUsername("alice")).thenReturn(Optional.of(
                Employee.builder().username("alice").role(Role.EMPLOYEE).build()));
        mvc.perform(get("/api/v1/accounts/1").header("Authorization", token))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("code").value(1105));
        mvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"refresh\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("code").value(1105));
        verifyNoInteractions(accountService);
        verify(refreshTokens, never()).save(any());
    }

    @Test
    void databaseUnavailableFailsClosedWith503() throws Exception {
        String token = bearer("alice", "CUSTOMER");
        refresh("alice", "CUSTOMER");
        when(customers.findByUsername("alice")).thenThrow(new DataAccessResourceFailureException("offline"));
        mvc.perform(get("/api/v1/accounts/my").header("Authorization", token))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("code").value(1110));
        mvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"refresh\"}"))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("code").value(1110));
        verifyNoInteractions(accountService);
        verify(refreshTokens, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"CUSTOMER", "EMPLOYEE", "ADMIN"})
    void activeIdentityCanRotateRefreshTokenWithoutAccessToken(String role) throws Exception {
        bearer("alice", role);
        RefreshToken old = refresh("alice", role);
        mvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"refresh\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("data.role").value(role))
                .andExpect(jsonPath("data.accessToken").isNotEmpty())
                .andExpect(jsonPath("data.refreshToken").isNotEmpty());
        assertThat(old.isRevoked()).isTrue();
        verify(refreshTokens, times(2)).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"UNKNOWN", ""})
    void signedTokenWithUnsupportedRoleIsRejected(String role) throws Exception {
        mvc.perform(get("/api/v1/accounts/my")
                        .header("Authorization", "Bearer " + jwt.generateToken("alice", role)))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("code").value(1105));
        verifyNoInteractions(accountService, customers, employees);
    }

    @Test
    void customerCannotUseStaffAccountMutations() throws Exception {
        String token = bearer("alice", "CUSTOMER");
        mvc.perform(patch("/api/v1/accounts/1/freeze").header("Authorization", token))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/v1/accounts/1/close").header("Authorization", token))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/accounts").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(accountService);
    }

    private String bearer(String username, String role) {
        if (role.equals("CUSTOMER")) {
            when(customers.findByUsername(username)).thenReturn(Optional.of(Customer.builder()
                    .username(username).status(CustomerStatus.ACTIVE).build()));
        } else {
            when(employees.findByUsername(username)).thenReturn(Optional.of(Employee.builder()
                    .username(username).role(Role.valueOf(role)).build()));
        }
        return "Bearer " + jwt.generateToken(username, role);
    }

    private RefreshToken refresh(String username, String role) {
        RefreshToken token = RefreshToken.builder().token("refresh").username(username).role(role)
                .expiresAt(LocalDateTime.now().plusDays(1)).build();
        when(refreshTokens.findByToken("refresh")).thenReturn(Optional.of(token));
        return token;
    }

    private Account account(String owner, String number, Long id) {
        return Account.builder().id(id).accountNumber(number).status(AccountStatus.ACTIVE)
                .balance(new BigDecimal("10000"))
                .customer(Customer.builder().username(owner).status(CustomerStatus.ACTIVE).build()).build();
    }
}

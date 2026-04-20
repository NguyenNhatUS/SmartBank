package com.SmartBank.service;


import com.SmartBank.dto.request.AccountCreateRequest;
import com.SmartBank.dto.response.AccountResponse;
import com.SmartBank.exception.ResourceNotFoundException;
import com.SmartBank.mapper.AccountMapper;
import com.SmartBank.entity.Account;
import com.SmartBank.entity.Customer;
import com.SmartBank.entity.enums.AccountStatus;
import com.SmartBank.entity.enums.AccountType;
import com.SmartBank.repository.AccountRepository;
import com.SmartBank.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    private Account account;

    private AccountResponse response;

    private AccountCreateRequest request;

    private Customer customer;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private AccountMapper mapper;

    @InjectMocks
    private AccountService service;

    @BeforeEach
    void setUp() {
        customer = Customer
                .builder()
                .id(1)
                .fullName("Nhat")
                .build();

        account = Account.builder()
                .id(1)
                .accountNumber("01234")
                .type(AccountType.SAVINGS)
                .balance(BigDecimal.valueOf(10000))
                .status(AccountStatus.ACTIVE)
                .customer(customer)
                .build();

        request = AccountCreateRequest
                .builder()
                .customerId(customer.getId())
                .type(AccountType.SAVINGS)
                .build();


        response = AccountResponse
                .builder()
                .id(1)
                .accountNumber("01234")
                .type(String.valueOf(AccountType.SAVINGS))
                .balance(BigDecimal.valueOf(100000))
                .status(String.valueOf(AccountStatus.ACTIVE))
                .customerId(1)
                .customerName("Nhat")
                .build();
    }

    @Test
    public void create_shouldReturnResponse_whenSuccess() {
        when(customerRepository.findById(request.getCustomerId()))
                .thenReturn(Optional.of(customer));

        when(mapper.toEntity(request, customer)).thenReturn(account);

        when(accountRepository.save(account)).thenReturn(account);

        when(mapper.toResponse(account)).thenReturn(response);

        AccountResponse result = service.create(request);


        assertEquals(result.getBalance(), response.getBalance());

        verify(accountRepository, times(1)).save(account);

        verify(mapper, times(1)).toResponse(any(Account.class));
    }

    @Test
    public void create_shouldThrown_whenCustomerNotFound() {
        request.setCustomerId(99);

        when(customerRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found");
    }

    @Test
    public void getById_shouldReturnResponse_whenExists() {
        when(accountRepository.findById(1)).thenReturn(Optional.of(account));

        when(mapper.toResponse(account)).thenReturn(response);

        AccountResponse result = service.getByID(1);

        assertEquals(result.getBalance(), response.getBalance());

        assertEquals(result.getCustomerName(), response.getCustomerName());

        verify(mapper, times(1)).toResponse(account);
    }

    @Test
    public void freeze_shouldFreezeResponse_whenSuccess() {
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));

        when(accountRepository.save(any(Account.class))).thenReturn(account);

        response.setStatus(String.valueOf(AccountStatus.FROZEN));

        when(mapper.toResponse(account)).thenReturn(response);

        AccountResponse result = service.freeze(account.getId());

        assertEquals(String.valueOf(AccountStatus.FROZEN), result.getStatus());

        verify(accountRepository, times(1)).save(account);
    }


    @Test
    public void close_shouldCloseResponse_whenSuccess() {
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));

        when(accountRepository.save(any(Account.class))).thenReturn(account);

        response.setStatus(String.valueOf(AccountStatus.CLOSED));

        when(mapper.toResponse(account)).thenReturn(response);

        AccountResponse result = service.close(account.getId());

        assertEquals(String.valueOf(AccountStatus.CLOSED), result.getStatus());

        verify(accountRepository, times(1)).save(account);

        verify(mapper, times(1)).toResponse(account
        );
    }




}
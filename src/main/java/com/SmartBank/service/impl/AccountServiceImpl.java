package com.SmartBank.service.impl;

import com.SmartBank.dto.request.AccountCreateRequest;
import com.SmartBank.dto.response.AccountResponse;
import com.SmartBank.dto.response.CustomerAccountResponse;
import com.SmartBank.entity.Account;
import com.SmartBank.entity.Customer;
import com.SmartBank.entity.enums.AccountStatus;
import com.SmartBank.exception.AppException;
import com.SmartBank.exception.ErrorCode;
import com.SmartBank.mapper.AccountMapper;
import com.SmartBank.repository.AccountRepository;
import com.SmartBank.repository.CustomerRepository;
import com.SmartBank.service.AccountService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final AccountMapper mapper;
    private final CustomerRepository customerRepository;

    public AccountServiceImpl(AccountRepository accountRepository, AccountMapper mapper, CustomerRepository customerRepository) {
        this.accountRepository = accountRepository;
        this.mapper = mapper;
        this.customerRepository = customerRepository;
    }

    @Override
    @Transactional
    public AccountResponse create(AccountCreateRequest request) {
        Customer customer = customerRepository.findById(request.getCustomerId()).orElseThrow(
                () -> new AppException(ErrorCode.CUSTOMER_NOT_FOUND)
        );

        Account account = mapper.toEntity(request, customer);
        account.setAccountNumber(generateAccountNumber());

        return mapper.toResponse(accountRepository.save(account));
    }

    private String generateAccountNumber() {
        String accountNumber;
        do {
            long number = (long) (Math.random() * 9_000_000_000L) + 1_000_000_000L;
            accountNumber = String.valueOf(number);
        } while (accountRepository.existsByAccountNumber(accountNumber));
        return accountNumber;
    }

    @Override
    @Cacheable(value = "accounts", key = "#id")
    public AccountResponse getByID(Long id) {
        Account account = accountRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.CUSTOMER_NOT_FOUND));
        return mapper.toResponse(account);
    }

    @Override
    @Transactional
    public AccountResponse freeze(Long id) {
        Account account = accountRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.CUSTOMER_NOT_FOUND));
        account.setStatus(AccountStatus.valueOf("FROZEN"));
        return mapper.toResponse(accountRepository.save(account));
    }

    @Override
    @Transactional
    public AccountResponse close(Long id) {
        Account account = accountRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));
        account.setStatus(AccountStatus.valueOf("CLOSED"));
        return mapper.toResponse(accountRepository.save(account));
    }

    @Override
    public List<CustomerAccountResponse> findAllGroupedByCustomer() {
        return customerRepository.findAllCustomersWithAccounts()
                .stream()
                .map(customer -> {
                    List<AccountResponse> accountResponses = customer.getAccountList()
                            .stream()
                            .map(mapper::toResponse)
                            .toList();

                    return CustomerAccountResponse.builder()
                            .customerId(customer.getId())
                            .customerName(customer.getFullName())
                            .customerEmail(customer.getEmail())
                            .accounts(accountResponses)
                            .build();
                })
                .toList();
    }

    @Override
    @Cacheable(value = "accounts")
    public List<AccountResponse> getAccountsByUsername(String username) {
        Customer customer = customerRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.CUSTOMER_NOT_FOUND));

        return customer.getAccountList().stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public AccountResponse createAccountForCustomer(String username, AccountCreateRequest request) {
        Customer customer = customerRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.CUSTOMER_NOT_FOUND));

        Account account = new Account();
        account.setAccountNumber(generateAccountNumber());
        account.setType(request.getType());
        account.setBalance(BigDecimal.ZERO);
        account.setStatus(AccountStatus.ACTIVE);
        account.setCustomer(customer);

        return mapper.toResponse(accountRepository.save(account));
    }
}

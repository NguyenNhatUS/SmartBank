package com.SmartBank.service;

import com.SmartBank.dto.request.AccountCreateRequest;
import com.SmartBank.dto.response.AccountResponse;
import com.SmartBank.dto.response.CustomerAccountResponse;
import com.SmartBank.exception.ResourceNotFoundException;
import com.SmartBank.mapper.AccountMapper;
import com.SmartBank.model.Account;
import com.SmartBank.model.Customer;
import com.SmartBank.model.enums.AccountStatus;
import com.SmartBank.repository.AccountRepository;
import com.SmartBank.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;


@Service
public class AccountService {

    private final AccountRepository accountRepository;

    private final AccountMapper mapper;

    private final CustomerRepository customerRepository;

    public AccountService(AccountRepository accountRepository, AccountMapper mapper, CustomerRepository customerRepository) {
        this.accountRepository = accountRepository;
        this.mapper = mapper;
        this.customerRepository = customerRepository;
    }


    public AccountResponse create(AccountCreateRequest request) {
        Customer customer = customerRepository.findById(request.getCustomerId()).orElseThrow(
                () -> new ResourceNotFoundException("Customer not found")
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

    public AccountResponse getByID(Long id) {
        Account account = accountRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        return mapper.toResponse(account);
    }

    public AccountResponse freeze(Long id) {
        Account account = accountRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        account.setStatus(AccountStatus.valueOf("FROZEN"));

        return mapper.toResponse(accountRepository.save(account));
    }

    public AccountResponse close(Long id) {
        Account account = accountRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        account.setStatus(AccountStatus.valueOf("CLOSED"));

        return mapper.toResponse(accountRepository.save(account));
    }

    public List<CustomerAccountResponse> findAllGroupedByCustomer() {
        // Chỉ tốn đúng 1 câu SQL duy nhất
        return customerRepository.findAllCustomersWithAccounts()
                .stream()
                .map(customer -> {
                    // Map list Account Entity sang Account Response DTO
                    List<AccountResponse> accountResponses = customer.getAccountList()
                            .stream()
                            .map(mapper::toResponse)
                            .toList();

                    // Map Customer Entity sang CustomerAccountResponse
                    return CustomerAccountResponse.builder()
                            .customerId(customer.getId())
                            .customerName(customer.getFullName())
                            .customerEmail(customer.getEmail())
                            .accounts(accountResponses)
                            .build();
                })
                .toList();
    }

    public List<AccountResponse> getAccountsByUsername(String username) {
        Customer customer = customerRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        return customer.getAccountList().stream()
                .map(mapper::toResponse)
                .toList();
    }

    public AccountResponse createAccountForCustomer(String username, AccountCreateRequest request) {
        Customer customer = customerRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        Account account = new Account();
        account.setAccountNumber(generateAccountNumber());
        account.setType(request.getType());
        account.setBalance(BigDecimal.ZERO);
        account.setStatus(AccountStatus.ACTIVE);
        account.setCustomer(customer);

        return mapper.toResponse(accountRepository.save(account));
    }
}

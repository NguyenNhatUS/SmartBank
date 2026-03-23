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
import java.util.List;
import java.util.stream.Collectors;


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
        Customer customer = customerRepository.findById(request.getCustomerId()).orElse(null);

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

    public AccountResponse getByID(Integer id) {
        Account account = accountRepository.findById(id).orElse(null);

        if(account == null) {
            throw new ResourceNotFoundException("Account not found");
        }

        return mapper.toResponse(account);
    }

    public AccountResponse freeze(Integer id) {
        Account account = accountRepository.findById(id).orElse(null);
        if(account == null) {
            throw new ResourceNotFoundException("Account not found");
        }

        account.setStatus(AccountStatus.valueOf("FROZEN"));

        return mapper.toResponse(accountRepository.save(account));
    }

    public AccountResponse close(Integer id) {
        Account account = accountRepository.findById(id).orElse(null);
        if(account == null) {
            throw new ResourceNotFoundException("Account not found");
        }

        account.setStatus(AccountStatus.valueOf("CLOSED"));

        return mapper.toResponse(accountRepository.save(account));
    }

    public List<CustomerAccountResponse> findAllGroupedByCustomer() {
        return accountRepository.findAllByOrderByCustomerIdAsc()
                .stream()
                .collect(Collectors.groupingBy(account -> account.getCustomer()))
                .entrySet()
                .stream()
                .map(entry -> {
                    Customer customer = entry.getKey();
                    List<AccountResponse> accountResponses = entry.getValue()
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
}

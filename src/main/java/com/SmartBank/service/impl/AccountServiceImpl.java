package com.SmartBank.service.impl;

import com.SmartBank.dto.request.AccountCreateRequest;
import com.SmartBank.dto.request.CustomerAccountCreateRequest;
import com.SmartBank.dto.response.AccountResponse;
import com.SmartBank.dto.response.CustomerAccountResponse;
import com.SmartBank.dto.response.PageResponse;
import com.SmartBank.entity.Account;
import com.SmartBank.entity.Customer;
import com.SmartBank.entity.enums.AccountStatus;
import com.SmartBank.exception.AppException;
import com.SmartBank.exception.ErrorCode;
import com.SmartBank.mapper.AccountMapper;
import com.SmartBank.repository.AccountRepository;
import com.SmartBank.repository.CustomerRepository;
import com.SmartBank.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private static final int MAX_PAGE_SIZE = 100;

    private final AccountRepository accountRepository;
    private final AccountMapper mapper;
    private final CustomerRepository customerRepository;

    @Override
    @Transactional
    public AccountResponse create(AccountCreateRequest request) {
        Customer customer = customerRepository.findById(request.getCustomerId()).orElseThrow(
                () -> new AppException(ErrorCode.CUSTOMER_NOT_FOUND));

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
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));
        return mapper.toResponse(account);
    }

    @Override
    @Transactional
    public AccountResponse freeze(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));
        account.setStatus(AccountStatus.valueOf("FROZEN"));
        return mapper.toResponse(accountRepository.save(account));
    }

    @Override
    @Transactional
    public AccountResponse close(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));
        account.setStatus(AccountStatus.valueOf("CLOSED"));
        return mapper.toResponse(accountRepository.save(account));
    }

    @Override
    public PageResponse<CustomerAccountResponse> findAllGroupedByCustomer(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        // Paginate customers first, rather than paginating a collection fetch join.
        Page<Customer> customers = customerRepository.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")));
        List<Long> customerIds = customers.getContent().stream().map(Customer::getId).toList();
        List<Account> accounts = customers.isEmpty() ? List.of()
                : accountRepository.findAllByCustomerIdsWithCustomer(customerIds);
        Map<Long, List<AccountResponse>> accountsByCustomer = accounts.stream()
                .collect(Collectors.groupingBy(account -> account.getCustomer().getId(),
                        Collectors.mapping(mapper::toResponse, Collectors.toList())));

        return PageResponse.from(customers.map(customer -> CustomerAccountResponse.builder()
                .customerId(customer.getId())
                .customerName(customer.getFullName())
                .customerEmail(customer.getEmail())
                .accounts(accountsByCustomer.getOrDefault(customer.getId(), List.of()))
                .build()));
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
    public AccountResponse createAccountForCustomer(String username, CustomerAccountCreateRequest request) {
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

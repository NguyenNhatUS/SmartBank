package com.SmartBank.service.impl;

import com.SmartBank.dto.request.CustomerRequest;
import com.SmartBank.dto.response.AccountResponse;
import com.SmartBank.dto.response.CustomerResponse;
import com.SmartBank.entity.Account;
import com.SmartBank.entity.Customer;
import com.SmartBank.entity.enums.CustomerStatus;
import com.SmartBank.exception.AppException;
import com.SmartBank.exception.ErrorCode;
import com.SmartBank.mapper.AccountMapper;
import com.SmartBank.mapper.CustomerMapper;
import com.SmartBank.repository.CustomerRepository;
import com.SmartBank.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository repository;
    private final CustomerMapper customerMapper;
    private final AccountMapper accountMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public CustomerResponse create(CustomerRequest request) {
        if (repository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        if (repository.existsByPhone(request.getPhone())) {
            throw new AppException(ErrorCode.PHONE_ALREADY_EXISTS);
        }

        if (repository.existsByUsername(request.getUsername())) {
            throw new AppException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }

        Customer customer = customerMapper.toEntity(request);
        customer.setPassword(passwordEncoder.encode(request.getPassword()));
        Customer savedCustomer = repository.save(customer);
        return customerMapper.toResponse(savedCustomer);
    }

    @Override
    @Cacheable(value = "customers")
    public List<CustomerResponse> getAllCustomers() {
        return repository.findAllCustomersWithAccounts()
                .stream()
                .map(customerMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "customers", key = "#id", condition = "#id > 0")
    public CustomerResponse getById(Long id) {
        Customer customer = repository.findById(id).orElse(null);
        if (customer == null) {
            throw new AppException(ErrorCode.CUSTOMER_NOT_FOUND);
        }
        return customerMapper.toResponse(customer);
    }

    @Override
    @CacheEvict(value = "customers", key = "#id")
    @Transactional
    public void deleteById(Long id) {
        Customer customer = repository.findById(id).orElse(null);
        if (customer == null) {
            throw new AppException(ErrorCode.CUSTOMER_NOT_FOUND);
        }
        customer.setStatus(CustomerStatus.LOCKED);
        repository.save(customer);
    }

    @Override
    @CachePut(value = "customers", key = "#id")
    @Transactional
    public CustomerResponse update(Long id, CustomerRequest request) {
        Customer customer = repository.findById(id).orElse(null);
        if (customer == null) {
            throw new AppException(ErrorCode.CUSTOMER_NOT_FOUND);
        }

        customer.setFullName(request.getFullName());
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhone());
        customer.setAddress(request.getAddress());
        customer.setDateOfBirth(request.getDateOfBirth());

        repository.save(customer);

        return customerMapper.toResponse(customer);
    }

    @Override
    @Cacheable(value = "accounts_customers", key = "#customerId")
    public List<AccountResponse> getAccountsByID(Long customerId) {
        Customer customer = repository.findById(customerId).orElse(null);
        if (customer == null) {
            throw new AppException(ErrorCode.CUSTOMER_NOT_FOUND);
        }

        List<Account> accountList = customer.getAccountList();
        return accountList.stream()
                .map(accountMapper::toResponse)
                .collect(Collectors.toList());
    }
}

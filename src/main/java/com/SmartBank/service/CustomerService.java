package com.SmartBank.service;

import com.SmartBank.dto.request.CustomerRequest;
import com.SmartBank.dto.response.AccountResponse;
import com.SmartBank.dto.response.CustomerResponse;
import com.SmartBank.entity.enums.ErrorCode;
import com.SmartBank.exception.AppException;
import com.SmartBank.mapper.AccountMapper;
import com.SmartBank.mapper.CustomerMapper;
import com.SmartBank.entity.Account;
import com.SmartBank.entity.Customer;
import com.SmartBank.entity.enums.CustomerStatus;
import com.SmartBank.repository.CustomerRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CustomerService {

    private final CustomerRepository repository;

    private final CustomerMapper customerMapper;

    private final AccountMapper accountMapper;

    public CustomerService(CustomerRepository repository, CustomerMapper customerMapper, AccountMapper accountMapper) {
        this.repository = repository;
        this.customerMapper = customerMapper;
        this.accountMapper = accountMapper;
    }

    public CustomerResponse create(CustomerRequest request) {

        if(repository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        if(repository.existsByPhone(request.getPhone())) {
            throw new AppException(ErrorCode.PHONE_ALREADY_EXISTS);
        }

        Customer customer = customerMapper.toEntity(request);
        Customer savedCustomer = repository.save(customer);
        return customerMapper.toResponse(savedCustomer);
    }

    @Cacheable(value = "customers_list")
    public List<CustomerResponse> getAllCustomers() {
        return repository.findAll()
                .stream()
                .map(customer -> customerMapper.toResponse(customer))
                .collect(Collectors.toList());
    }

    @Cacheable(value = "customers", key = "#id", condition = "#id > 0")
    public CustomerResponse getById(Long id) {
        Customer customer = repository.findById(id).orElse(null);
        if(customer == null) {
            throw new AppException(ErrorCode.CUSTOMER_NOT_FOUND);
        }
        return customerMapper.toResponse(customer);
    }

    @CacheEvict(value = "customers", key = "#id")
    public void deleteById(Long id) {
        Customer customer = repository.findById(id).orElse(null);
        if(customer == null) {
            throw new AppException(ErrorCode.CUSTOMER_NOT_FOUND);
        }
        customer.setStatus(CustomerStatus.LOCKED);
        repository.deleteById(id);
    }

    @CachePut(value = "customers", key = "#id")
    public CustomerResponse update(Long id, CustomerRequest request) {
        Customer customer = repository.findById(id).orElse(null);
        if(customer == null) {
            throw new AppException(ErrorCode.CUSTOMER_NOT_FOUND);
        }

        customer.setFullName(request.getFullName());
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhone());
        customer.setAddress(request.getAddress());
        customer.setDateOfBirth(request.getDateOfBirth());

        return customerMapper.toResponse(customer);
    }

    @Cacheable(value = "accounts_customer", key = "#id")
    public List<AccountResponse> getAccountsByID(Long id) {
        Customer customer = repository.findById(id).orElse(null);
        if(customer == null) {
            throw new AppException(ErrorCode.CUSTOMER_NOT_FOUND);
        }

        List<Account> accountList = customer.getAccountList();
        return accountList.stream()
                .map(account -> accountMapper.toResponse((account)))
                .collect(Collectors.toList());
    }
}


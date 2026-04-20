package com.SmartBank.service;

import com.SmartBank.dto.request.CustomerRequest;
import com.SmartBank.dto.response.AccountResponse;
import com.SmartBank.dto.response.CustomerResponse;
import com.SmartBank.exception.DuplicateResourceException;
import com.SmartBank.exception.ResourceNotFoundException;
import com.SmartBank.mapper.AccountMapper;
import com.SmartBank.mapper.CustomerMapper;
import com.SmartBank.entity.Account;
import com.SmartBank.entity.Customer;
import com.SmartBank.entity.enums.CustomerStatus;
import com.SmartBank.repository.CustomerRepository;
import org.jspecify.annotations.Nullable;
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
            throw new DuplicateResourceException("Email can not be duplicated");
        }

        if(repository.existsByPhone(request.getPhone())) {
            throw new DuplicateResourceException("Phone can not be duplicated");
        }

        Customer customer = customerMapper.toEntity(request);
        Customer savedCustomer = repository.save(customer);
        return customerMapper.toResponse(savedCustomer);
    }

    public List<CustomerResponse> getAllCustomers() {
        return repository.findAll()
                .stream()
                .map(customer -> customerMapper.toResponse(customer))
                .collect(Collectors.toList());
    }

    public CustomerResponse getById(Long id) {
        Customer customer = repository.findById(id).orElse(null);
        if(customer == null) {
            throw new ResourceNotFoundException("Customer not found");
        }
        return customerMapper.toResponse(customer);
    }

    public void deleteById(Long id) {
        Customer customer = repository.findById(id).orElse(null);
        if(customer == null) {
            throw new ResourceNotFoundException("Customer not found");
        }
        customer.setStatus(CustomerStatus.LOCKED);
        repository.deleteById(id);
    }

    public CustomerResponse update(Long id, CustomerRequest request) {
        Customer customer = repository.findById(id).orElse(null);
        if(customer == null) {
            throw new ResourceNotFoundException("Customer not found");
        }

        customer.setFullName(request.getFullName());
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhone());
        customer.setAddress(request.getAddress());
        customer.setDateOfBirth(request.getDateOfBirth());

        return customerMapper.toResponse(customer);
    }

    public @Nullable List<AccountResponse> getAccountsByID(Long id) {
        Customer customer = repository.findById(id).orElse(null);
        if(customer == null) {
            throw new ResourceNotFoundException("Customer not found");
        }

        List<Account> accountList = customer.getAccountList();
        return accountList.stream()
                .map(account -> accountMapper.toResponse((account)))
                .collect(Collectors.toList());
    }
}


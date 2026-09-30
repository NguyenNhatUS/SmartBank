package com.SmartBank.service;

import com.SmartBank.dto.request.CustomerRequest;
import com.SmartBank.dto.response.AccountResponse;
import com.SmartBank.dto.response.CustomerResponse;

import java.util.List;

public interface CustomerService {

    CustomerResponse create(CustomerRequest request);

    List<CustomerResponse> getAllCustomers();

    CustomerResponse getById(Long id);

    void deleteById(Long id);

    CustomerResponse update(Long id, CustomerRequest request);

    List<AccountResponse> getAccountsByID(Long customerId);
}

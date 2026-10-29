package com.SmartBank.service;

import com.SmartBank.dto.request.CustomerRequest;
import com.SmartBank.dto.response.AccountResponse;
import com.SmartBank.dto.response.CustomerResponse;
import com.SmartBank.dto.response.PageResponse;

import java.util.List;

public interface CustomerService {

    CustomerResponse create(CustomerRequest request);

    PageResponse<CustomerResponse> getAllCustomers(int page, int size);

    CustomerResponse getById(Long id);

    void deleteById(Long id);

    CustomerResponse update(Long id, CustomerRequest request);

    List<AccountResponse> getAccountsByID(Long customerId);
}

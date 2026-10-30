package com.SmartBank.service;

import com.SmartBank.dto.request.AccountCreateRequest;
import com.SmartBank.dto.request.CustomerAccountCreateRequest;
import com.SmartBank.dto.response.AccountResponse;
import com.SmartBank.dto.response.CustomerAccountResponse;
import com.SmartBank.dto.response.PageResponse;

import java.util.List;

public interface AccountService {

    AccountResponse create(AccountCreateRequest request);

    AccountResponse getByID(Long id);

    AccountResponse freeze(Long id);

    AccountResponse close(Long id);

    PageResponse<CustomerAccountResponse> findAllGroupedByCustomer(int page, int size);

    List<AccountResponse> getAccountsByUsername(String username);

    AccountResponse createAccountForCustomer(String username, CustomerAccountCreateRequest request);
}

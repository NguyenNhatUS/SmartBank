package com.SmartBank.service;

import com.SmartBank.dto.request.AccountCreateRequest;
import com.SmartBank.dto.response.AccountResponse;
import com.SmartBank.dto.response.CustomerAccountResponse;

import java.util.List;

public interface AccountService {

    AccountResponse create(AccountCreateRequest request);

    AccountResponse getByID(Long id);

    AccountResponse freeze(Long id);

    AccountResponse close(Long id);

    List<CustomerAccountResponse> findAllGroupedByCustomer();

    List<AccountResponse> getAccountsByUsername(String username);

    AccountResponse createAccountForCustomer(String username, AccountCreateRequest request);
}

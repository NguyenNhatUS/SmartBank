package com.SmartBank.mapper;

import com.SmartBank.dto.request.AccountCreateRequest;
import com.SmartBank.dto.response.AccountResponse;
import com.SmartBank.model.Account;
import com.SmartBank.model.Customer;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {
    public AccountResponse toResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .type(String.valueOf(account.getType()))
                .balance(account.getBalance())
                .status(String.valueOf(account.getStatus()))
                .createdAt(account.getCreatedAt())
                .customerId(account.getCustomer().getId())
                .customerName(account.getCustomer().getFullName())
                .build();
    }

    public Account toEntity(AccountCreateRequest request, Customer customer) {
        return Account.builder()
                .customer(customer)
                .type(request.getType())
                .build();
    }

}

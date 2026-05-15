package com.SmartBank.mapper;

import com.SmartBank.dto.request.AccountCreateRequest;
import com.SmartBank.dto.response.AccountResponse;
import com.SmartBank.entity.Account;
import com.SmartBank.entity.Customer;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class AccountMapper {
    public AccountResponse toResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .type(String.valueOf(account.getType()))
                .balance(account.getBalance())
                .status(String.valueOf(account.getStatus()))
                .interestRate(account.getInterestRate())
                .lastInterestCalculationDate(account.getLastInterestCalculationDate())
                .termMonths(account.getTermMonths())
                .maturityDate(account.getMaturityDate())
                .createdAt(account.getCreatedAt())
                .customerId(account.getCustomer().getId())
                .customerName(account.getCustomer().getFullName())
                .build();
    }

    public Account toEntity(AccountCreateRequest request, Customer customer) {
        Account account = Account.builder()
                .customer(customer)
                .type(request.getType())
                .interestRate(request.getInterestRate())
                .termMonths(request.getTermMonths())
                .build();

        if (request.getTermMonths() != null && request.getTermMonths() > 0) {
            account.setMaturityDate(LocalDateTime.now().plusMonths(request.getTermMonths()));
        }

        return account;
    }

}

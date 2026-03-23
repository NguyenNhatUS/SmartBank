package com.SmartBank.mapper;

import com.SmartBank.dto.response.TransactionResponse;
import com.SmartBank.model.Transaction;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {
    public TransactionResponse toResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .transactionCode(transaction.getTransactionCode())
                .type(String.valueOf(transaction.getType()))
                .amount(transaction.getAmount())
                .description(transaction.getDescription())
                .createdAt(transaction.getCreatedAt())
                .sourceAccountNumber(transaction.getSourceAccount().getAccountNumber())
                .targetAccountNumber(
                        (transaction.getTargetAccount() != null)
                                ? transaction.getTargetAccount().getAccountNumber()
                                : null
                )
                .build();
    }
}

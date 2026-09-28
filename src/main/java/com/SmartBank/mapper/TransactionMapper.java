package com.SmartBank.mapper;

import com.SmartBank.dto.response.TransactionResponse;
import com.SmartBank.entity.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    @Mapping(source = "sourceAccount.accountNumber", target = "sourceAccountNumber")
    @Mapping(source = "targetAccount.accountNumber", target = "targetAccountNumber")
    TransactionResponse toResponse(Transaction transaction);
}

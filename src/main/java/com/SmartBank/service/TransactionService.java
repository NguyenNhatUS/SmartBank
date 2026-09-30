package com.SmartBank.service;

import com.SmartBank.dto.request.DepositWithDrawRequest;
import com.SmartBank.dto.request.TransferRequest;
import com.SmartBank.dto.response.PageResponse;
import com.SmartBank.dto.response.TransactionResponse;
import jakarta.validation.Valid;

public interface TransactionService {

    TransactionResponse deposit(@Valid DepositWithDrawRequest request);

    TransactionResponse withdraw(@Valid DepositWithDrawRequest request);

    TransactionResponse transfer(@Valid TransferRequest request);

    PageResponse<TransactionResponse> getAccountStatement(String accountNumber, int page, int size);
}

package com.SmartBank.service;

import com.SmartBank.dto.request.DepositWithDrawRequest;
import com.SmartBank.dto.request.TransferRequest;
import com.SmartBank.dto.response.TransactionResponse;
import com.SmartBank.mapper.TransactionMapper;
import com.SmartBank.model.Account;
import com.SmartBank.model.Transaction;
import com.SmartBank.model.enums.AccountStatus;
import com.SmartBank.model.enums.TransactionType;
import com.SmartBank.repository.AccountRepository;
import com.SmartBank.repository.TransactionRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;

    private final AccountRepository accountRepository;

    private final TransactionMapper mapper;

    public TransactionService(TransactionRepository transactionRepository, AccountRepository accountRepository, TransactionMapper mapper) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.mapper = mapper;
    }

    @Transactional
    public TransactionResponse deposit(@Valid DepositWithDrawRequest request) {

        Account account = accountRepository.findByAccountNumber(request.getAccountNumber());

        if(account == null) {
            throw new RuntimeException("Account does not exists");
        }

        if(account.getStatus() != AccountStatus.ACTIVE) {
            throw new RuntimeException("Account is not in active");
        }

        account.setBalance(account.getBalance().add(request.getAmount()));
        accountRepository.save(account);

        Transaction transaction = Transaction.builder()
                .transactionCode(generateTransactionCode())
                .type(TransactionType.DEPOSIT)
                .amount(request.getAmount())
                .description(request.getDescription())
                .sourceAccount(account)
                .targetAccount(null)
                .build();

        return mapper.toResponse(transactionRepository.save(transaction));
    }

    private String generateTransactionCode() {
        return "TXN" + System.currentTimeMillis();
    }


    @Transactional
    public TransactionResponse withdraw(@Valid DepositWithDrawRequest request) {
        Account account = accountRepository.findByAccountNumber(request.getAccountNumber());

        if(account == null) {
            throw new RuntimeException("Account does not exists");
        }

        if(account.getStatus() != AccountStatus.ACTIVE) {
            throw new RuntimeException("Account is not in active");
        }

        if (account.getBalance().subtract(request.getAmount()).compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Insufficient balance");
        }

        account.setBalance(account.getBalance().subtract(request.getAmount()));
        accountRepository.save(account);

        Transaction transaction = Transaction.builder()
                .transactionCode(generateTransactionCode())
                .type(TransactionType.WITHDRAW)
                .amount(request.getAmount())
                .description(request.getDescription())
                .sourceAccount(account)
                .targetAccount(null)
                .build();

        return mapper.toResponse(transactionRepository.save(transaction));
    }

    @Transactional
    public TransactionResponse transfer(@Valid TransferRequest request) {
        Account source = accountRepository.findByAccountNumber(request.getSourceAccountNumber());

        Account target = accountRepository.findByAccountNumber(request.getTargetAccountNumber());

        if(source == null || target == null) {
            throw new RuntimeException("Account not found");
        }

        if(source.getStatus() != AccountStatus.ACTIVE) {
            throw new RuntimeException("Account is not in active");
        }

        if(target.getStatus() != AccountStatus.ACTIVE) {
            throw new RuntimeException("Account is not in active");
        }

        if(source.getBalance().compareTo(request.getAmount()) < 0) {
            throw new RuntimeException("Insufficient Funds Exception");
        }

        source.setBalance(source.getBalance().subtract(request.getAmount()));
        target.setBalance(target.getBalance().add(request.getAmount()));

        Transaction transaction = Transaction.builder()
                .transactionCode(generateTransactionCode())
                .type(TransactionType.TRANSFER)
                .amount(request.getAmount())
                .description(request.getDescription())
                .sourceAccount(source)
                .targetAccount(target)
                .build();

        return mapper.toResponse(transactionRepository.save(transaction));
    }
}

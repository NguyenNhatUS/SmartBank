package com.SmartBank.service;

import com.SmartBank.dto.request.DepositWithDrawRequest;
import com.SmartBank.dto.response.TransactionResponse;
import com.SmartBank.mapper.TransactionMapper;
import com.SmartBank.model.Account;
import com.SmartBank.model.Transaction;
import com.SmartBank.model.enums.AccountStatus;
import com.SmartBank.model.enums.TransactionType;
import com.SmartBank.repository.AccountRepository;
import com.SmartBank.repository.TransactionRepository;
import org.springframework.stereotype.Service;

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

    public TransactionResponse deposit(DepositWithDrawRequest request) {
        Account account = accountRepository.findByAccountNumber(request.getAccountNumber());

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

}

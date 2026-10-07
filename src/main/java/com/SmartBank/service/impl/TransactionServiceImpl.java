package com.SmartBank.service.impl;

import com.SmartBank.dto.request.DepositWithDrawRequest;
import com.SmartBank.dto.request.TransferRequest;
import com.SmartBank.dto.response.PageResponse;
import com.SmartBank.dto.response.TransactionResponse;
import com.SmartBank.entity.Account;
import com.SmartBank.entity.Transaction;
import com.SmartBank.entity.enums.AccountStatus;
import com.SmartBank.entity.enums.TransactionType;
import com.SmartBank.exception.AppException;
import com.SmartBank.exception.ErrorCode;
import com.SmartBank.mapper.TransactionMapper;
import com.SmartBank.repository.AccountRepository;
import com.SmartBank.repository.TransactionRepository;
import com.SmartBank.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;


@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final TransactionMapper mapper;

    @Override
    @Transactional
    @CacheEvict(value = {"accounts", "accounts_customers", "customers"}, allEntries = true)
    public TransactionResponse deposit(@Valid DepositWithDrawRequest request) {
        if (request.getAccountNumber() == null) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_FOUND);
        }
        Account account = accountRepository.findByAccountNumberWithLock(request.getAccountNumber());

        if (account == null) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_FOUND);
        }

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AppException(ErrorCode.ACCOUNT_INACTIVE);
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

    @Override
    @Transactional
    @CacheEvict(value = {"accounts", "accounts_customers", "customers"}, allEntries = true)
    public TransactionResponse withdraw(@Valid DepositWithDrawRequest request) {
        if (request.getAccountNumber() == null) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_FOUND);
        }
        Account account = accountRepository.findByAccountNumberWithLock(request.getAccountNumber());

        if (account == null) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_FOUND);
        }

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AppException(ErrorCode.ACCOUNT_INACTIVE);
        }

        if (account.getBalance().subtract(request.getAmount()).compareTo(BigDecimal.ZERO) < 0) {
            throw new AppException(ErrorCode.INVALID_TRANSACTION_AMOUNT);
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

    @Override
    @Transactional
    @CacheEvict(value = {"accounts", "accounts_customers", "customers"}, allEntries = true)
    public TransactionResponse transfer(@Valid TransferRequest request) {
        if (request.getSourceAccountNumber() == null || request.getTargetAccountNumber() == null) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_FOUND);
        }

        if (request.getSourceAccountNumber().equals(request.getTargetAccountNumber())) {
            throw new AppException(ErrorCode.SAME_ACCOUNT_TRANSFER);
        }

        // Lock in sorted order to avoid deadlock
        String firstLockNum = request.getSourceAccountNumber().compareTo(request.getTargetAccountNumber()) < 0
                ? request.getSourceAccountNumber() : request.getTargetAccountNumber();
        String secondLockNum = request.getSourceAccountNumber().compareTo(request.getTargetAccountNumber()) < 0
                ? request.getTargetAccountNumber() : request.getSourceAccountNumber();

        Account firstLock = accountRepository.findByAccountNumberWithLock(firstLockNum);
        Account secondLock = accountRepository.findByAccountNumberWithLock(secondLockNum);

        Account source = request.getSourceAccountNumber().equals(firstLockNum) ? firstLock : secondLock;
        Account target = request.getTargetAccountNumber().equals(firstLockNum) ? firstLock : secondLock;

        if (source == null || target == null) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_FOUND);
        }

        if (source.getStatus() != AccountStatus.ACTIVE || target.getStatus() != AccountStatus.ACTIVE) {
            throw new AppException(ErrorCode.ACCOUNT_INACTIVE);
        }

        if (source.getBalance().compareTo(request.getAmount()) < 0) {
            throw new AppException(ErrorCode.INVALID_TRANSACTION_AMOUNT);
        }

        source.setBalance(source.getBalance().subtract(request.getAmount()));
        target.setBalance(target.getBalance().add(request.getAmount()));

        accountRepository.save(source);
        accountRepository.save(target);

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

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> getAccountStatement(String accountNumber, int page, int size) {
        if (accountNumber == null || !accountRepository.existsByAccountNumber(accountNumber)) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_FOUND);
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Transaction> transactionPage = transactionRepository
                .findBySourceAccount_AccountNumberOrTargetAccount_AccountNumber(accountNumber, accountNumber, pageable);

        Page<TransactionResponse> responsePage = transactionPage.map(mapper::toResponse);
        return PageResponse.from(responsePage);
    }
}
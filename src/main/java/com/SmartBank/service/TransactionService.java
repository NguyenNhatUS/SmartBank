package com.SmartBank.service;

import com.SmartBank.dto.request.DepositWithDrawRequest;
import com.SmartBank.dto.request.TransferRequest;
import com.SmartBank.dto.request.TransactionFilterRequest;
import com.SmartBank.dto.request.BillPaymentRequest;
import com.SmartBank.dto.response.TransactionResponse;
import com.SmartBank.entity.enums.ErrorCode;
import com.SmartBank.exception.AppException;
import com.SmartBank.mapper.TransactionMapper;
import com.SmartBank.entity.Account;
import com.SmartBank.entity.Transaction;
import com.SmartBank.entity.enums.AccountStatus;
import com.SmartBank.entity.enums.TransactionType;
import com.SmartBank.repository.AccountRepository;
import com.SmartBank.repository.TransactionRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final TransactionMapper mapper;

    public TransactionService(TransactionRepository transactionRepository, AccountRepository accountRepository,
            TransactionMapper mapper) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.mapper = mapper;
    }

    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = { "accounts", "accounts_customers",
            "customers" }, allEntries = true)
    public TransactionResponse deposit(@Valid DepositWithDrawRequest request) {
        Account account = accountRepository.findByAccountNumber(request.getAccountNumber());

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

    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = { "accounts", "accounts_customers",
            "customers" }, allEntries = true)
    public TransactionResponse withdraw(@Valid DepositWithDrawRequest request) {
        Account account = accountRepository.findByAccountNumber(request.getAccountNumber());

        if (account == null) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_FOUND);
        }

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AppException(ErrorCode.ACCOUNT_INACTIVE);
        }

        if (account.getMaturityDate() != null && account.getMaturityDate().isAfter(LocalDateTime.now())) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_MATURED);
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

    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = { "accounts", "accounts_customers",
            "customers" }, allEntries = true)
    public TransactionResponse transfer(@Valid TransferRequest request) {
        Account source = accountRepository.findByAccountNumber(request.getSourceAccountNumber());
        Account target = accountRepository.findByAccountNumber(request.getTargetAccountNumber());

        if (source == null || target == null) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_FOUND);
        }

        if (source.getStatus() != AccountStatus.ACTIVE || target.getStatus() != AccountStatus.ACTIVE) {
            throw new AppException(ErrorCode.ACCOUNT_INACTIVE);
        }

        if (source.getMaturityDate() != null && source.getMaturityDate().isAfter(LocalDateTime.now())) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_MATURED);
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

    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = { "accounts", "accounts_customers",
            "customers" }, allEntries = true)
    public TransactionResponse payBill(@Valid BillPaymentRequest request) {
        Account account = accountRepository.findByAccountNumber(request.getAccountNumber());

        if (account == null) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_FOUND);
        }

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AppException(ErrorCode.ACCOUNT_INACTIVE);
        }

        if (account.getMaturityDate() != null && account.getMaturityDate().isAfter(LocalDateTime.now())) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_MATURED);
        }

        if (account.getBalance().compareTo(request.getAmount()) < 0) {
            throw new AppException(ErrorCode.INVALID_TRANSACTION_AMOUNT);
        }

        account.setBalance(account.getBalance().subtract(request.getAmount()));
        accountRepository.save(account);

        Transaction transaction = Transaction.builder()
                .transactionCode(generateTransactionCode())
                .type(TransactionType.BILL_PAYMENT)
                .amount(request.getAmount())
                .description("Bill payment for " + request.getBillerName() + " (Bill #" + request.getBillNumber() + ")")
                .sourceAccount(account)
                .targetAccount(null)
                .build();

        return mapper.toResponse(transactionRepository.save(transaction));
    }

    public List<TransactionResponse> filterTransactions(TransactionFilterRequest filter) {
        Specification<Transaction> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.getStartDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filter.getStartDate()));
            }
            if (filter.getEndDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), filter.getEndDate()));
            }
            if (filter.getMinAmount() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("amount"), filter.getMinAmount()));
            }
            if (filter.getMaxAmount() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("amount"), filter.getMaxAmount()));
            }
            if (filter.getType() != null) {
                predicates.add(cb.equal(root.get("type"), filter.getType()));
            }
            if (filter.getAccountNumber() != null) {
                Predicate source = cb.equal(root.get("sourceAccount").get("accountNumber"), filter.getAccountNumber());
                Predicate target = cb.equal(root.get("targetAccount").get("accountNumber"), filter.getAccountNumber());
                predicates.add(cb.or(source, target));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return transactionRepository.findAll(spec).stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }
}

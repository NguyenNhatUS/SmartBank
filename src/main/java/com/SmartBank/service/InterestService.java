package com.SmartBank.service;

import com.SmartBank.dto.request.DepositWithDrawRequest;
import com.SmartBank.entity.Account;
import com.SmartBank.entity.enums.AccountType;
import com.SmartBank.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InterestService {

    private final AccountRepository accountRepository;
    private final TransactionService transactionService;

    // Run on the 1st of every month at 2 AM
    @Scheduled(cron = "0 0 2 1 * ?")
    @Transactional
    public void calculateMonthlyInterest() {
        log.info("Starting monthly interest calculation at {}", LocalDateTime.now());
        List<Account> savingsAccounts = accountRepository.findByType(AccountType.SAVINGS);

        for (Account account : savingsAccounts) {
            if (account.getInterestRate() != null && account.getInterestRate().compareTo(BigDecimal.ZERO) > 0) {
                try {
                    // Simple monthly interest: Balance * (Annual Rate / 12)
                    BigDecimal monthlyRate = account.getInterestRate().divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);
                    BigDecimal interestAmount = account.getBalance().multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);

                    if (interestAmount.compareTo(BigDecimal.ZERO) > 0) {
                        DepositWithDrawRequest request = DepositWithDrawRequest.builder()
                                .accountNumber(account.getAccountNumber())
                                .amount(interestAmount)
                                .description("Monthly Interest Credit")
                                .build();

                        transactionService.deposit(request);
                        account.setLastInterestCalculationDate(LocalDateTime.now());
                        accountRepository.save(account);
                        log.info("Credited {} interest to account: {}", interestAmount, account.getAccountNumber());
                    }
                } catch (Exception e) {
                    log.error("Failed to calculate interest for account: {}. Error: {}", account.getAccountNumber(), e.getMessage());
                }
            }
        }
    }
}

package com.SmartBank.controller;

import com.SmartBank.dto.request.RecurringTransferRequest;
import com.SmartBank.entity.Account;
import com.SmartBank.entity.RecurringTransfer;
import com.SmartBank.repository.AccountRepository;
import com.SmartBank.repository.RecurringTransferRepository;
import com.SmartBank.security.RateLimit;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/recurring-transfers")
@RequiredArgsConstructor
public class RecurringTransferController {

    private final RecurringTransferRepository recurringRepository;
    private final AccountRepository accountRepository;

    @PostMapping
    @RateLimit(requests = 5, duration = 60)
    public ResponseEntity<Void> create(@Valid @RequestBody RecurringTransferRequest request) {
        Account source = accountRepository.findByAccountNumber(request.getSourceAccountNumber());
        Account target = accountRepository.findByAccountNumber(request.getTargetAccountNumber());

        if (source == null || target == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        RecurringTransfer recurring = RecurringTransfer.builder()
                .sourceAccount(source)
                .targetAccount(target)
                .amount(request.getAmount())
                .frequency(request.getFrequency())
                .nextExecutionDate(request.getStartDate())
                .isActive(true)
                .description(request.getDescription())
                .build();

        recurringRepository.save(recurring);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}

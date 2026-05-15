package com.SmartBank.service;

import com.SmartBank.dto.request.TransferRequest;
import com.SmartBank.entity.RecurringTransfer;
import com.SmartBank.repository.RecurringTransferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledTaskService {

    private final RecurringTransferRepository recurringRepository;
    private final TransactionService transactionService;

    @Scheduled(cron = "0 0 1 * * ?") // Run every day at 1 AM
    @Transactional
    public void processRecurringTransfers() {
        log.info("Starting processing recurring transfers at {}", LocalDateTime.now());
        List<RecurringTransfer> dueTransfers = recurringRepository.findAllByIsActiveTrueAndNextExecutionDateBefore(LocalDateTime.now());

        for (RecurringTransfer recurring : dueTransfers) {
            try {
                TransferRequest request = TransferRequest.builder()
                        .sourceAccountNumber(recurring.getSourceAccount().getAccountNumber())
                        .targetAccountNumber(recurring.getTargetAccount().getAccountNumber())
                        .amount(recurring.getAmount())
                        .description("Recurring Transfer: " + recurring.getDescription())
                        .build();

                transactionService.transfer(request);

                // Update next execution date
                recurring.setNextExecutionDate(calculateNextDate(recurring.getNextExecutionDate(), recurring.getFrequency()));
                recurringRepository.save(recurring);
                log.info("Successfully processed recurring transfer ID: {}", recurring.getId());
            } catch (Exception e) {
                log.error("Failed to process recurring transfer ID: {}. Error: {}", recurring.getId(), e.getMessage());
            }
        }
    }

    private LocalDateTime calculateNextDate(LocalDateTime current, com.SmartBank.entity.enums.Frequency frequency) {
        return switch (frequency) {
            case DAILY -> current.plusDays(1);
            case WEEKLY -> current.plusWeeks(1);
            case MONTHLY -> current.plusMonths(1);
        };
    }
}

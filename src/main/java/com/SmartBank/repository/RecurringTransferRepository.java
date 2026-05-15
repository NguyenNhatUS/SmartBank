package com.SmartBank.repository;

import com.SmartBank.entity.RecurringTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RecurringTransferRepository extends JpaRepository<RecurringTransfer, Long> {
    List<RecurringTransfer> findAllByIsActiveTrueAndNextExecutionDateBefore(LocalDateTime dateTime);
}

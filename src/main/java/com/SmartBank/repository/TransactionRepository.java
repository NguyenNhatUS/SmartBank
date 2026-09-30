package com.SmartBank.repository;

import com.SmartBank.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Page<Transaction> findBySourceAccount_AccountNumberOrTargetAccount_AccountNumber(
            String sourceAccountNumber, String targetAccountNumber, Pageable pageable);
}

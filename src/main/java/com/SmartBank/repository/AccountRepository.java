package com.SmartBank.repository;

import com.SmartBank.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    boolean existsByAccountNumber(String accountNumber);

    List<Account> findAllByOrderByCustomerIdAsc();

    Account findByAccountNumber(String accountNumber);
}

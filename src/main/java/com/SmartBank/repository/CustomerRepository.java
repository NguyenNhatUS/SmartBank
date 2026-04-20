package com.SmartBank.repository;


import com.SmartBank.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    Optional<Customer> findByUsername(String username);

    @Query("SELECT DISTINCT c FROM Customer c LEFT JOIN FETCH c.accountList")
    List<Customer> findAllCustomersWithAccounts();
}

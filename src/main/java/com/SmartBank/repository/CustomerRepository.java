package com.SmartBank.repository;


import com.SmartBank.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Integer> {
    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    Optional<Customer> findByUsername(String username);
}

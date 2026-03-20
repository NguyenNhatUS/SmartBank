package com.SmartBank.repository;


import com.SmartBank.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Integer> {
    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);
}

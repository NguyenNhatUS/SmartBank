package com.SmartBank.service;

import com.SmartBank.entity.Customer;
import com.SmartBank.entity.Employee;
import com.SmartBank.repository.CustomerRepository;
import com.SmartBank.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final EmployeeRepository employeeRepository;

    private final CustomerRepository customerRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<Employee> employee = employeeRepository.findByUsername(username);
        if(employee.isPresent()) {
            return User.builder()
                    .username(employee.get().getUsername())
                    .password(employee.get().getPassword())
                    .roles(employee.get().getRole().name())
                    .disabled(!employee.get().isEnabled())
                    .build();
        }

        Customer customer = customerRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return User.builder()
                .username(customer.getUsername())
                .password(customer.getPassword())
                .roles("CUSTOMER")
                .disabled(!customer.isEnabled())
                .build();
    }
}

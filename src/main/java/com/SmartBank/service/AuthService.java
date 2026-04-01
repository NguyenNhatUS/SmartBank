package com.SmartBank.service;


import com.SmartBank.dto.request.LoginRequest;
import com.SmartBank.dto.request.RegisterRequest;
import com.SmartBank.dto.response.LoginResponse;
import com.SmartBank.model.Customer;
import com.SmartBank.model.Employee;
import com.SmartBank.model.enums.Role;
import com.SmartBank.repository.CustomerRepository;
import com.SmartBank.repository.EmployeeRepository;
import com.SmartBank.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final EmployeeRepository employeeRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public LoginResponse login(LoginRequest request) {
        String username = request.getUsername();
        String rawPassword = request.getPassword();
        String role;
        String encodedPassword;

        Optional<Employee> employeeOpt = employeeRepository.findByUsername(username);
        if (employeeOpt.isPresent()) {
            encodedPassword = employeeOpt.get().getPassword();
            role = employeeOpt.get().getRole().name();
        } else {
            Customer customer = customerRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
            encodedPassword = customer.getPassword();
            role = "CUSTOMER";
        }

        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            throw new UsernameNotFoundException("Invalid credentials");
        }

        String token = jwtUtil.generateToken(username, role);
        return new LoginResponse(token, username, role);
    }

    public void register(RegisterRequest request) {
        boolean existsInEmployee = employeeRepository.findByUsername(request.getUsername()).isPresent();
        boolean existsInCustomer = customerRepository.findByUsername(request.getUsername()).isPresent();

        if (existsInEmployee || existsInCustomer) {
            throw new RuntimeException("Username already exists");
        }

        if (request.getRole() == Role.CUSTOMER) {
            Customer customer = new Customer();
            customer.setUsername(request.getUsername());
            customer.setPassword(passwordEncoder.encode(request.getPassword()));
            customer.setEnabled(true);
            customerRepository.save(customer);
        } else {
            Employee employee = new Employee();
            employee.setUsername(request.getUsername());
            employee.setPassword(passwordEncoder.encode(request.getPassword()));
            employee.setRole(request.getRole());
            employee.setEnabled(true);
            employeeRepository.save(employee);
        }
    }
}

package com.SmartBank.service;


import com.SmartBank.dto.request.LoginRequest;
import com.SmartBank.dto.request.RegisterRequest;
import com.SmartBank.dto.response.LoginResponse;
import com.SmartBank.model.Employee;
import com.SmartBank.repository.EmployeeRepository;
import com.SmartBank.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public LoginResponse login(LoginRequest request) {
        Employee employee = employeeRepository.findByUsername(request.getUsername()).orElseThrow(() -> new UsernameNotFoundException("User not found: " + request.getUsername()));

        if(!passwordEncoder.matches(request.getPassword(), employee.getPassword())) {
            throw new UsernameNotFoundException("Password incorrect");
        }
        String token = jwtUtil.generateToken(employee.getUsername(), employee.getRole().name());
        return new LoginResponse(token, employee.getUsername(), employee.getRole().name());
    }

    public void register(RegisterRequest request) {
        if(employeeRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        Employee employee = new Employee(
                request.getUsername(),
                passwordEncoder.encode(request.getPassword()),
                request.getRole(),
                true
        );

        employeeRepository.save(employee);
    }
}

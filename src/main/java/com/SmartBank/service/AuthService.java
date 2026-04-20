package com.SmartBank.service;

import com.SmartBank.dto.request.CreateEmployeeRequest;
import com.SmartBank.dto.request.LoginRequest;
import com.SmartBank.dto.request.RefreshRequest;
import com.SmartBank.dto.request.RegisterRequest;
import com.SmartBank.dto.response.LoginResponse;
import com.SmartBank.exception.DuplicateResourceException;
import com.SmartBank.entity.Customer;
import com.SmartBank.entity.Employee;
import com.SmartBank.entity.RefreshToken;
import com.SmartBank.entity.enums.Role;
import com.SmartBank.repository.CustomerRepository;
import com.SmartBank.repository.EmployeeRepository;
import com.SmartBank.repository.RefreshTokenRepository;
import com.SmartBank.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final EmployeeRepository employeeRepository;
    private final CustomerRepository customerRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

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

        String accessToken = jwtUtil.generateToken(username, role);
        String refreshToken = createRefreshToken(username, role);

        return LoginResponse
                .builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .username(username)
                .role(role)
                .build();
    }

    public LoginResponse refresh(RefreshRequest request) {
        RefreshToken stored = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (stored.isRevoked()) {
            throw new RuntimeException("Refresh token has been revoked");
        }

        if (stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(stored);
            throw new RuntimeException("Refresh token expired");
        }

        // Revoke token cũ, cấp token mới (rotation)
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        String newAccessToken = jwtUtil.generateToken(stored.getUsername(), stored.getRole());
        String newRefreshToken = createRefreshToken(stored.getUsername(), stored.getRole());

        return LoginResponse
                .builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .username(stored.getUsername())
                .role(stored.getRole())
                .build();
    }

    public void logout(String username) {
        refreshTokenRepository.deleteByUsername(username);
    }

    public void register(RegisterRequest request) {
        boolean existsInEmployee = employeeRepository.findByUsername(request.getUsername()).isPresent();
        boolean existsInCustomer = customerRepository.findByUsername(request.getUsername()).isPresent();

        if (existsInEmployee || existsInCustomer) {
            throw new DuplicateResourceException("Username already exists");
        }

        Customer customer = Customer
                .builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .enabled(true)
                .build();

        customerRepository.save(customer);
    }


    private String createRefreshToken(String username, String role) {
        RefreshToken refreshToken = RefreshToken
                .builder()
                .token(jwtUtil.generateRefreshToken())
                .username(username)
                .role(role)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshExpiration / 1000))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);
        return refreshToken.getToken();
    }

    public void createEmployee(CreateEmployeeRequest request) {
        if (request.getRole() == Role.CUSTOMER) {
            throw new RuntimeException("Use /auth/register for customer accounts");
        }

        if (employeeRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        Employee employee = Employee
                .builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .enabled(true)
                .build();

        employeeRepository.save(employee);
    }
}

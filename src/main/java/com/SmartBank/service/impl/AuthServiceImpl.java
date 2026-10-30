package com.SmartBank.service.impl;

import com.SmartBank.dto.request.CreateEmployeeRequest;
import com.SmartBank.dto.request.LoginRequest;
import com.SmartBank.dto.request.RefreshRequest;
import com.SmartBank.dto.request.RegisterRequest;
import com.SmartBank.dto.response.LoginResponse;
import com.SmartBank.entity.Customer;
import com.SmartBank.entity.Employee;
import com.SmartBank.entity.RefreshToken;
import com.SmartBank.entity.enums.CustomerStatus;
import com.SmartBank.entity.enums.Role;
import com.SmartBank.exception.AppException;
import com.SmartBank.exception.ErrorCode;
import com.SmartBank.repository.CustomerRepository;
import com.SmartBank.repository.EmployeeRepository;
import com.SmartBank.repository.RefreshTokenRepository;
import com.SmartBank.security.JwtTokenProvider;
import com.SmartBank.security.UserAccessValidator;
import com.SmartBank.service.AuthService;
import com.SmartBank.service.TokenBlacklistService;
import org.springframework.dao.DataAccessException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Optional;


@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final EmployeeRepository employeeRepository;
    private final CustomerRepository customerRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;
    private final UserAccessValidator userAccessValidator;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    @Override
    public LoginResponse login(LoginRequest request) {
        String username = request.getUsername();
        String rawPassword = request.getPassword();
        String role;
        String encodedPassword;
        boolean active;

        Optional<Employee> employeeOpt = employeeRepository.findByUsername(username);
        Optional<Customer> customerOpt = customerRepository.findByUsername(username);
        if (employeeOpt.isPresent() && customerOpt.isPresent()) {
            throw new AppException(ErrorCode.INVALID_USERNAME_OR_PASSWORD);
        }
        if (employeeOpt.isPresent()) {
            encodedPassword = employeeOpt.get().getPassword();
            role = employeeOpt.get().getRole().name();
            active = employeeOpt.get().isEnabled();
        } else {
            Customer customer = customerOpt
                    .orElseThrow(() -> new AppException(ErrorCode.INVALID_USERNAME_OR_PASSWORD));
            encodedPassword = customer.getPassword();
            role = "CUSTOMER";
            active = customer.isEnabled() && customer.getStatus() == CustomerStatus.ACTIVE;
        }

        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            throw new AppException(ErrorCode.INVALID_USERNAME_OR_PASSWORD);
        }

        // Only disclose account state after the password has been verified.
        if (!active) {
            throw new AppException(ErrorCode.USER_NOT_ACTIVE);
        }

        String accessToken = jwtTokenProvider.generateToken(username, role);
        String refreshToken = createRefreshToken(username, role);

        return LoginResponse
                .builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .username(username)
                .role(role)
                .build();
    }

    @Override
    public LoginResponse refresh(RefreshRequest request) {
        RefreshToken stored = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_TOKEN));

        if (stored.isRevoked()) {
            throw new AppException(ErrorCode.REFRESH_TOKEN_REVOKED);
        }

        if (stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(stored);
            throw new AppException(ErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        try {
            userAccessValidator.validate(stored.getUsername(), stored.getRole());
        } catch (DataAccessException e) {
            throw new AppException(ErrorCode.AUTH_SERVICE_UNAVAILABLE);
        }

        // Revoke token cũ, cấp token mới (rotation)
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        String newAccessToken = jwtTokenProvider.generateToken(stored.getUsername(), stored.getRole());
        String newRefreshToken = createRefreshToken(stored.getUsername(), stored.getRole());

        return LoginResponse
                .builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .username(stored.getUsername())
                .role(stored.getRole())
                .build();
    }

    @Override
    public void logout(String username, String accessToken) {
        try {
            tokenBlacklistService.blacklist(accessToken, jwtTokenProvider.extractExpiration(accessToken));
        } catch (DataAccessException e) {
            throw new AppException(ErrorCode.AUTH_SERVICE_UNAVAILABLE);
        }
        refreshTokenRepository.deleteByUsername(username);
    }

    @Override
    public void register(RegisterRequest request) {
        boolean existsInEmployee = employeeRepository.findByUsername(request.getUsername()).isPresent();
        boolean existsInCustomer = customerRepository.findByUsername(request.getUsername()).isPresent();

        if (existsInEmployee || existsInCustomer) {
            throw new AppException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }

        Customer customer = Customer
                .builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .enabled(true)
                .build();

        customerRepository.save(customer);
    }

    private String createRefreshToken(String username, String role) {
        RefreshToken refreshToken = RefreshToken
                .builder()
                .token(jwtTokenProvider.generateRefreshToken())
                .username(username)
                .role(role)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshExpiration / 1000))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);
        return refreshToken.getToken();
    }

    @Override
    public void createEmployee(CreateEmployeeRequest request) {
        if (request.getRole() == Role.CUSTOMER) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }

        if (employeeRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new AppException(ErrorCode.EMPLOYEE_ALREADY_EXISTS);
        }
        if (customerRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new AppException(ErrorCode.USERNAME_ALREADY_EXISTS);
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

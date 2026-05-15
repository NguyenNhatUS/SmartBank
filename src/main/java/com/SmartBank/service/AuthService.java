package com.SmartBank.service;

import com.SmartBank.dto.request.CreateEmployeeRequest;
import com.SmartBank.dto.request.LoginRequest;
import com.SmartBank.dto.request.RefreshRequest;
import com.SmartBank.dto.request.RegisterRequest;
import com.SmartBank.dto.response.LoginResponse;
import com.SmartBank.entity.Customer;
import com.SmartBank.entity.Employee;
import com.SmartBank.entity.RefreshToken;
import com.SmartBank.entity.enums.ErrorCode;
import com.SmartBank.entity.enums.Role;
import com.SmartBank.exception.AppException;
import com.SmartBank.repository.CustomerRepository;
import com.SmartBank.repository.EmployeeRepository;
import com.SmartBank.repository.RefreshTokenRepository;
import com.SmartBank.security.JwtTokenProvider;
import com.SmartBank.security.LoginAttemptService;
import com.SmartBank.security.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
    private final JwtTokenProvider jwtUtil;
    private final TokenBlacklistService blacklistService;
    private final LoginAttemptService loginAttemptService;
    private final AuditService auditService;
    private final OtpService otpService;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    public LoginResponse login(LoginRequest request) {
        String username = request.getUsername();
        String rawPassword = request.getPassword();
        
        if (loginAttemptService.isBlocked(username)) {
            throw new AppException(ErrorCode.ACCOUNT_LOCKED);
        }

        String role;
        String encodedPassword;
        boolean twoFactorEnabled;

        Optional<Employee> employeeOpt = employeeRepository.findByUsername(username);
        if (employeeOpt.isPresent()) {
            encodedPassword = employeeOpt.get().getPassword();
            role = employeeOpt.get().getRole().name();
            twoFactorEnabled = employeeOpt.get().isTwoFactorEnabled();
        } else {
            Customer customer = customerRepository.findByUsername(username)
                    .orElseThrow(() -> new AppException(ErrorCode.CUSTOMER_NOT_FOUND));
            encodedPassword = customer.getPassword();
            role = "CUSTOMER";
            twoFactorEnabled = customer.isTwoFactorEnabled();
        }

        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            loginAttemptService.loginFailed(username);
            auditService.log(username, "LOGIN_FAILED", "Invalid password");
            throw new AppException(ErrorCode.INVALID_USERNAME_OR_PASSWORD);
        }

        loginAttemptService.loginSucceeded(username);
        auditService.log(username, "LOGIN_SUCCESS", "User logged in successfully");

        if (twoFactorEnabled) {
            otpService.generateOtp(username);
            return LoginResponse.builder()
                    .username(username)
                    .role(role)
                    .mfaRequired(true)
                    .build();
        }

        String accessToken = jwtUtil.generateToken(username, role);
        String refreshToken = createRefreshToken(username, role);

        return LoginResponse
                .builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .username(username)
                .role(role)
                .mfaRequired(false)
                .build();
    }

    public LoginResponse verifyOtp(String username, String otpCode) {
        if (!otpService.validateOtp(username, otpCode)) {
            auditService.log(username, "OTP_FAILED", "Invalid OTP code");
            throw new AppException(ErrorCode.INVALID_OTP);
        }

        String role;
        Optional<Employee> employeeOpt = employeeRepository.findByUsername(username);
        if (employeeOpt.isPresent()) {
            role = employeeOpt.get().getRole().name();
        } else {
            role = "CUSTOMER";
        }

        auditService.log(username, "OTP_SUCCESS", "MFA verified");

        String accessToken = jwtUtil.generateToken(username, role);
        String refreshToken = createRefreshToken(username, role);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .username(username)
                .role(role)
                .mfaRequired(false)
                .build();
    }

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

    public void logout(String username, String token) {
        // Blacklist access token
        long remainingTime = jwtUtil.getRemainingTime(token);
        blacklistService.blacklistToken(token, remainingTime);

        // Xóa refresh token trong DB
        refreshTokenRepository.deleteByUsername(username);
        auditService.log(username, "LOGOUT", "User logged out");
    }

    public void register(RegisterRequest request) {
        validatePassword(request.getPassword());
        boolean existsInEmployee = employeeRepository.findByUsername(request.getUsername()).isPresent();
        boolean existsInCustomer = customerRepository.findByUsername(request.getUsername()).isPresent();

        if (existsInEmployee || existsInCustomer) {
            throw new AppException(ErrorCode.USERNAME_ALREADY_EXISTS);
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
        validatePassword(request.getPassword());
        if (request.getRole() == Role.CUSTOMER) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }

        if (employeeRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new AppException(ErrorCode.EMPLOYEE_ALREADY_EXISTS);
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
    private void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new AppException(ErrorCode.PASSWORD_TOO_WEAK);
        }
        // Regex: At least 1 upper, 1 lower, 1 digit, 1 special char
        String pattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$";
        if (!password.matches(pattern)) {
            throw new AppException(ErrorCode.PASSWORD_TOO_WEAK);
        }
    }
}

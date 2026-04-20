package com.SmartBank.service;

import com.SmartBank.dto.request.LoginRequest;
import com.SmartBank.dto.request.RefreshRequest;
import com.SmartBank.dto.request.RegisterRequest;
import com.SmartBank.dto.response.LoginResponse;
import com.SmartBank.entity.Customer;
import com.SmartBank.entity.Employee;
import com.SmartBank.entity.RefreshToken;
import com.SmartBank.entity.enums.Role;
import com.SmartBank.repository.CustomerRepository;
import com.SmartBank.repository.EmployeeRepository;
import com.SmartBank.repository.RefreshTokenRepository;
import com.SmartBank.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private EmployeeRepository employeeRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtUtil;

    @InjectMocks
    private AuthService authService;

    // ========== LOGIN ==========

    @Test
    void login_shouldReturnTokens_whenEmployeeCredentialsValid() {
        Employee employee = new Employee();
        employee.setUsername("admin");
        employee.setPassword("hashed");
        employee.setRole(Role.ADMIN);

        when(employeeRepository.findByUsername("admin")).thenReturn(Optional.of(employee));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtUtil.generateToken("admin", "ADMIN")).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken()).thenReturn("refresh-token");

        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("password123");

        LoginResponse response = authService.login(request);

        assertEquals("access-token", response.getAccessToken());
        assertEquals("admin", response.getUsername());
        assertEquals("ADMIN", response.getRole());
    }

    @Test
    void login_shouldThrow_whenUserNotFound() {
        when(employeeRepository.findByUsername("ghost")).thenReturn(Optional.empty());
        when(customerRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        LoginRequest request = new LoginRequest();
        request.setUsername("ghost");
        request.setPassword("password123");

        assertThrows(UsernameNotFoundException.class, () -> authService.login(request));
    }

    @Test
    void login_shouldThrow_whenPasswordWrong() {
        Employee employee = new Employee();
        employee.setUsername("admin");
        employee.setPassword("hashed");
        employee.setRole(Role.ADMIN);

        when(employeeRepository.findByUsername("admin")).thenReturn(Optional.of(employee));
        when(passwordEncoder.matches("wrongpass", "hashed")).thenReturn(false);

        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("wrongpass");

        assertThrows(UsernameNotFoundException.class, () -> authService.login(request));
    }

    // ========== REGISTER ==========

    @Test
    void register_shouldSaveCustomer_whenUsernameAvailable() {
        when(employeeRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(customerRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("123456")).thenReturn("hashed");

        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setPassword("123456");

        authService.register(request);

        verify(customerRepository, times(1)).save(any(Customer.class));
    }

    @Test
    void register_shouldThrow_whenUsernameAlreadyExists() {
        Customer existing = new Customer();
        existing.setUsername("existinguser");

        when(employeeRepository.findByUsername("existinguser")).thenReturn(Optional.empty());
        when(customerRepository.findByUsername("existinguser")).thenReturn(Optional.of(existing));

        RegisterRequest request = new RegisterRequest();
        request.setUsername("existinguser");
        request.setPassword("123456");

        assertThrows(RuntimeException.class, () -> authService.register(request));
        verify(customerRepository, never()).save(any());
    }

    // ========== REFRESH ==========

    @Test
    void refresh_shouldReturnNewTokens_whenRefreshTokenValid() {
        RefreshToken stored = new RefreshToken();
        stored.setToken("valid-refresh");
        stored.setUsername("admin");
        stored.setRole("ADMIN");
        stored.setExpiresAt(LocalDateTime.now().plusDays(7));
        stored.setRevoked(false);

        when(refreshTokenRepository.findByToken("valid-refresh")).thenReturn(Optional.of(stored));
        when(jwtUtil.generateToken("admin", "ADMIN")).thenReturn("new-access-token");
        when(jwtUtil.generateRefreshToken()).thenReturn("new-refresh-token");

        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("valid-refresh");

        LoginResponse response = authService.refresh(request);

        assertEquals("new-access-token", response.getAccessToken());
        assertTrue(stored.isRevoked()); // token cũ phải bị revoke
    }

    @Test
    void refresh_shouldThrow_whenRefreshTokenRevoked() {
        RefreshToken stored = new RefreshToken();
        stored.setToken("revoked-token");
        stored.setRevoked(true);

        when(refreshTokenRepository.findByToken("revoked-token")).thenReturn(Optional.of(stored));

        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("revoked-token");

        assertThrows(RuntimeException.class, () -> authService.refresh(request));
    }

    @Test
    void refresh_shouldThrow_whenRefreshTokenExpired() {
        RefreshToken stored = new RefreshToken();
        stored.setToken("expired-token");
        stored.setRevoked(false);
        stored.setExpiresAt(LocalDateTime.now().minusDays(1)); // đã hết hạn

        when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(stored));

        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("expired-token");

        assertThrows(RuntimeException.class, () -> authService.refresh(request));
    }
}
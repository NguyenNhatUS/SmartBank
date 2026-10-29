package com.SmartBank.service;

import com.SmartBank.dto.request.LoginRequest;
import com.SmartBank.dto.request.RefreshRequest;
import com.SmartBank.dto.request.RegisterRequest;
import com.SmartBank.dto.response.LoginResponse;
import com.SmartBank.entity.Customer;
import com.SmartBank.entity.Employee;
import com.SmartBank.entity.RefreshToken;
import com.SmartBank.entity.enums.CustomerStatus;
import com.SmartBank.entity.enums.Role;
import com.SmartBank.exception.ErrorCode;
import com.SmartBank.exception.AppException;
import com.SmartBank.repository.CustomerRepository;
import com.SmartBank.repository.EmployeeRepository;
import com.SmartBank.repository.RefreshTokenRepository;
import com.SmartBank.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.SmartBank.service.impl.AuthServiceImpl;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private EmployeeRepository employeeRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthServiceImpl authService;

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"EMPLOYEE", "ADMIN"})
    void login_shouldReturnTokens_whenEmployeeCredentialsValid(Role role) {
        Employee employee = Employee.builder()
                .username("admin").password("hashed").role(role).build();

        when(employeeRepository.findByUsername("admin")).thenReturn(Optional.of(employee));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtTokenProvider.generateToken("admin", role.name())).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken()).thenReturn("refresh-token");

        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("password123");

        LoginResponse response = authService.login(request);

        assertEquals("access-token", response.getAccessToken());
        assertEquals("admin", response.getUsername());
        assertEquals(role.name(), response.getRole());
        assertEquals("refresh-token", response.getRefreshToken());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void login_shouldThrow_whenUserNotFound() {
        when(employeeRepository.findByUsername("ghost")).thenReturn(Optional.empty());
        when(customerRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        LoginRequest request = new LoginRequest();
        request.setUsername("ghost");
        request.setPassword("password123");

        AppException exception = assertThrows(AppException.class, () -> authService.login(request));
        assertEquals(ErrorCode.INVALID_USERNAME_OR_PASSWORD, exception.getErrorCode());
        verifyNoInteractions(jwtTokenProvider, refreshTokenRepository);
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

        AppException exception = assertThrows(AppException.class, () -> authService.login(request));
        assertEquals(ErrorCode.INVALID_USERNAME_OR_PASSWORD, exception.getErrorCode());
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"EMPLOYEE", "ADMIN"})
    void login_shouldRejectDisabledEmployee_withoutIssuingTokensOrFallingBackToCustomer(Role role) {
        Employee employee = Employee.builder()
                .username("staff").password("hashed").role(role).enabled(false).build();
        when(employeeRepository.findByUsername("staff")).thenReturn(Optional.of(employee));
        when(passwordEncoder.matches("correct-password", "hashed")).thenReturn(true);

        LoginRequest request = new LoginRequest();
        request.setUsername("staff");
        request.setPassword("correct-password");

        AppException exception = assertThrows(AppException.class, () -> authService.login(request));

        assertEquals(ErrorCode.USER_NOT_ACTIVE, exception.getErrorCode());
        verifyNoInteractions(jwtTokenProvider, refreshTokenRepository, customerRepository);
    }

    @ParameterizedTest
    @CsvSource({"false, ACTIVE", "true, LOCKED", "false, LOCKED"})
    void login_shouldRejectInactiveCustomer_withoutIssuingTokens(boolean enabled, CustomerStatus status) {
        Customer customer = Customer.builder()
                .username("customer").password("hashed").enabled(enabled).status(status).build();
        when(employeeRepository.findByUsername("customer")).thenReturn(Optional.empty());
        when(customerRepository.findByUsername("customer")).thenReturn(Optional.of(customer));
        when(passwordEncoder.matches("correct-password", "hashed")).thenReturn(true);

        LoginRequest request = new LoginRequest();
        request.setUsername("customer");
        request.setPassword("correct-password");

        AppException exception = assertThrows(AppException.class, () -> authService.login(request));

        assertEquals(ErrorCode.USER_NOT_ACTIVE, exception.getErrorCode());
        verifyNoInteractions(jwtTokenProvider, refreshTokenRepository);
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void login_shouldNotDiscloseDisabledState_whenPasswordIsWrong(Role role) {
        if (role == Role.CUSTOMER) {
            Customer customer = Customer.builder()
                    .username("disabled").password("hashed").enabled(false).build();
            when(employeeRepository.findByUsername("disabled")).thenReturn(Optional.empty());
            when(customerRepository.findByUsername("disabled")).thenReturn(Optional.of(customer));
        } else {
            Employee employee = Employee.builder()
                    .username("disabled").password("hashed").role(role).enabled(false).build();
            when(employeeRepository.findByUsername("disabled")).thenReturn(Optional.of(employee));
        }
        when(passwordEncoder.matches("wrong-password", "hashed")).thenReturn(false);
        LoginRequest request = new LoginRequest();
        request.setUsername("disabled");
        request.setPassword("wrong-password");

        AppException exception = assertThrows(AppException.class, () -> authService.login(request));

        assertEquals(ErrorCode.INVALID_USERNAME_OR_PASSWORD, exception.getErrorCode());
        verifyNoInteractions(jwtTokenProvider, refreshTokenRepository);
    }

    @Test
    void login_shouldReturnTokens_whenCustomerIsActiveAndCredentialsValid() {
        Customer customer = Customer.builder()
                .username("customer").password("hashed").enabled(true).status(CustomerStatus.ACTIVE).build();
        when(employeeRepository.findByUsername("customer")).thenReturn(Optional.empty());
        when(customerRepository.findByUsername("customer")).thenReturn(Optional.of(customer));
        when(passwordEncoder.matches("correct-password", "hashed")).thenReturn(true);
        when(jwtTokenProvider.generateToken("customer", "CUSTOMER")).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken()).thenReturn("refresh-token");
        LoginRequest request = new LoginRequest();
        request.setUsername("customer");
        request.setPassword("correct-password");

        LoginResponse response = authService.login(request);

        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals("CUSTOMER", response.getRole());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
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
        RefreshToken stored = RefreshToken.builder()
                .token("valid-refresh")
                .username("admin")
                .role("ADMIN")
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByToken("valid-refresh")).thenReturn(Optional.of(stored));
        when(jwtTokenProvider.generateToken("admin", "ADMIN")).thenReturn("new-access-token");
        when(jwtTokenProvider.generateRefreshToken()).thenReturn("new-refresh-token");

        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("valid-refresh");

        LoginResponse response = authService.refresh(request);

        assertEquals("new-access-token", response.getAccessToken());
        assertTrue(stored.isRevoked()); // token cũ phải bị revoke
    }

    @Test
    void refresh_shouldThrow_whenRefreshTokenRevoked() {
        RefreshToken stored = RefreshToken.builder()
                .token("revoked-token")
                .revoked(true)
                .build();

        when(refreshTokenRepository.findByToken("revoked-token")).thenReturn(Optional.of(stored));

        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("revoked-token");

        assertThrows(RuntimeException.class, () -> authService.refresh(request));
    }

    @Test
    void refresh_shouldThrow_whenRefreshTokenExpired() {
        RefreshToken stored = RefreshToken.builder()
                .token("expired-token")
                .revoked(false)
                .expiresAt(LocalDateTime.now().minusDays(1))
                .build();

        when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(stored));

        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("expired-token");

        assertThrows(RuntimeException.class, () -> authService.refresh(request));
    }
}

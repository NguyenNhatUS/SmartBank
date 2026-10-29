package com.SmartBank.controller;

import com.SmartBank.controller.v1.AuthController;
import com.SmartBank.entity.Customer;
import com.SmartBank.entity.enums.CustomerStatus;
import com.SmartBank.exception.ErrorCode;
import com.SmartBank.exception.GlobalExceptionHandler;
import com.SmartBank.repository.CustomerRepository;
import com.SmartBank.repository.EmployeeRepository;
import com.SmartBank.repository.RefreshTokenRepository;
import com.SmartBank.security.JwtTokenProvider;
import com.SmartBank.service.TokenBlacklistService;
import com.SmartBank.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Exercises the real controller, service and advice without starting a DB/Redis context.
// This standalone setup does not exercise the security filter chain or rate-limit aspect.
@ExtendWith(MockitoExtension.class)
class AuthLoginResponseTest {
    @Mock private EmployeeRepository employeeRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private TokenBlacklistService tokenBlacklistService;
    @InjectMocks private AuthServiceImpl authService;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new AuthController(authService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @ParameterizedTest
    @CsvSource({
            "true, false, ACTIVE, true, USER_NOT_ACTIVE",
            "true, true, LOCKED, true, USER_NOT_ACTIVE",
            "true, false, ACTIVE, false, INVALID_USERNAME_OR_PASSWORD",
            "false, true, ACTIVE, false, INVALID_USERNAME_OR_PASSWORD"
    })
    void login_shouldReturnExpectedStatusAndErrorBody(
            boolean exists, boolean enabled, CustomerStatus status,
            boolean passwordMatches, ErrorCode expectedError) throws Exception {
        when(employeeRepository.findByUsername("customer")).thenReturn(Optional.empty());
        if (exists) {
            Customer customer = Customer.builder().username("customer").password("hashed")
                    .enabled(enabled).status(status).build();
            when(customerRepository.findByUsername("customer")).thenReturn(Optional.of(customer));
            when(passwordEncoder.matches("submitted-password", "hashed")).thenReturn(passwordMatches);
        } else {
            when(customerRepository.findByUsername("customer")).thenReturn(Optional.empty());
        }

        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"customer","password":"submitted-password"}
                                """))
                .andExpect(status().is(expectedError.getStatus().value()))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(expectedError.getCode()))
                .andExpect(jsonPath("$.message").value(expectedError.getMessage()))
                .andExpect(jsonPath("$.path").value("/auth/login"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data").doesNotExist());

        verifyNoInteractions(jwtTokenProvider, refreshTokenRepository);
    }
}

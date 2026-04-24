package com.SmartBank.security;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {

    private JwtTokenProvider jwtUtil;

    private static final String USERNAME = "testuser";
    private static final String ROLE = "CUSTOMER";
    private static final String SECRET = "my-very-secret-key-for-jwt-testing-need-to-be-long-enough-123456";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtTokenProvider();

        ReflectionTestUtils.setField(jwtUtil, "secretKey", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expiration", 3600000L);
        ReflectionTestUtils.setField(jwtUtil, "refreshExpiration", 86400000L);
    }

    @Test
    void generateToken_shouldReturnNonNullToken() {
        String token = jwtUtil.generateToken(USERNAME, ROLE);
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void extractUsername_shouldReturnCorrectUsername() {
        String token = jwtUtil.generateToken(USERNAME, ROLE);
        Assertions.assertEquals(USERNAME, jwtUtil.extractUsername(token));
    }

    @Test
    void extractRole_shouldReturnCorrectRole() {
        String token = jwtUtil.generateToken(USERNAME, ROLE);
        Assertions.assertEquals(ROLE, jwtUtil.extractRole(token));
    }

    @Test
    void isTokenValid_shouldReturnTrue_whenValidToken() {
        String token = jwtUtil.generateToken(USERNAME, ROLE);
        Assertions.assertTrue(jwtUtil.isTokenValid(token));
    }

    @Test
    void isTokenValid_shouldReturnFalse_whenTamperedToken() {
        String token = jwtUtil.generateToken(USERNAME, ROLE) + "tampered";
        Assertions.assertFalse(jwtUtil.isTokenValid(token));
    }

    @Test
    void generateRefreshToken_shouldReturnValidUUID() {
        String refreshToken = jwtUtil.generateRefreshToken();
        assertNotNull(refreshToken);
        assertEquals(36, refreshToken.length());
        assertTrue(refreshToken.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"));
    }

    @Test
    void isTokenValid_shouldReturnFalse_whenTokenExpired() {
        ReflectionTestUtils.setField(jwtUtil, "expiration", -1000L);
        String token = jwtUtil.generateToken(USERNAME, ROLE);

        Assertions.assertFalse(jwtUtil.isTokenValid(token));
    }

    @Test
    void isTokenValid_shouldReturnFalse_whenTokenIsNull() {
        try {
            Assertions.assertFalse(jwtUtil.isTokenValid(null));
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException || e instanceof NullPointerException);
        }
    }
}
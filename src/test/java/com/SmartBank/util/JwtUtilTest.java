package com.SmartBank.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    private static final String USERNAME = "testuser";
    private static final String ROLE = "CUSTOMER";
    private static final String SECRET = "my-very-secret-key-for-jwt-testing-need-to-be-long-enough-123456";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();

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
        assertEquals(USERNAME, jwtUtil.extractUsername(token));
    }

    @Test
    void extractRole_shouldReturnCorrectRole() {
        String token = jwtUtil.generateToken(USERNAME, ROLE);
        assertEquals(ROLE, jwtUtil.extractRole(token));
    }

    @Test
    void isTokenValid_shouldReturnTrue_whenValidToken() {
        String token = jwtUtil.generateToken(USERNAME, ROLE);
        assertTrue(jwtUtil.isTokenValid(token));
    }

    @Test
    void isTokenValid_shouldReturnFalse_whenTamperedToken() {
        String token = jwtUtil.generateToken(USERNAME, ROLE) + "tampered";
        assertFalse(jwtUtil.isTokenValid(token));
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

        assertFalse(jwtUtil.isTokenValid(token));
    }

    @Test
    void isTokenValid_shouldReturnFalse_whenTokenIsNull() {
        try {
            assertFalse(jwtUtil.isTokenValid(null));
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException || e instanceof NullPointerException);
        }
    }
}
package com.SmartBank.security;

import com.SmartBank.exception.AppException;
import com.SmartBank.exception.ErrorCode;
import com.SmartBank.service.impl.AuthServiceImpl;
import com.SmartBank.service.impl.RedisTokenBlacklistService;
import com.SmartBank.repository.RefreshTokenRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.HandlerExceptionResolver;
import jakarta.servlet.FilterChain;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TokenBlacklistTest {
    private StringRedisTemplate redis;
    private ValueOperations<String, String> values;
    private RedisTokenBlacklistService blacklist;
    private JwtTokenProvider jwt;
    private RefreshTokenRepository refreshTokens;
    private AuthServiceImpl auth;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        SecurityContextHolder.clearContext();
        redis = mock(StringRedisTemplate.class);
        values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        blacklist = new RedisTokenBlacklistService(redis);
        jwt = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwt, "secretKey", "test-only-signing-key-with-at-least-32-bytes-long");
        ReflectionTestUtils.setField(jwt, "expiration", 900000L);
        refreshTokens = mock(RefreshTokenRepository.class);
        auth = new AuthServiceImpl(null, null, refreshTokens, null, jwt, blacklist);
        HandlerExceptionResolver resolver = (request, response, handler, exception) -> {
            AppException error = (AppException) exception;
            response.setStatus(error.getErrorCode().getStatus().value());
            return null;
        };
        filter = new JwtAuthenticationFilter(jwt, blacklist, resolver);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void logoutRevokesPresentedTokenButLeavesOtherAccessTokensUsable() throws Exception {
        Map<String, String> stored = new HashMap<>();
        doAnswer(call -> {
            stored.put(call.getArgument(0), call.getArgument(1));
            return null;
        }).when(values).set(anyString(), anyString(), any(Duration.class));
        when(redis.hasKey(anyString())).thenAnswer(call -> stored.containsKey(call.getArgument(0)));
        String token = jwt.generateToken("alice", "CUSTOMER");
        String otherToken = jwt.generateToken("alice", "CUSTOMER");
        assertNotEquals(token, otherToken);

        assertAllowed(token);
        auth.logout("alice", token);
        verify(refreshTokens).deleteByUsername("alice");

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(request(token), response, chain);
        assertEquals(401, response.getStatus());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(chain);
        assertAllowed(otherToken);
    }

    @Test
    void blacklistWritesHashWithRemainingLifetimeInOneSet() {
        Instant expiresAt = Instant.now().plusSeconds(60);
        blacklist.blacklist("sensitive-bearer-token", expiresAt);
        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Duration> ttl = ArgumentCaptor.forClass(Duration.class);
        verify(values).set(key.capture(), eq("1"), ttl.capture());
        assertTrue(key.getValue().matches("token_blacklist:[0-9a-f]{64}"));
        assertFalse(key.getValue().contains("sensitive-bearer-token"));
        assertTrue(ttl.getValue().toMillis() > 0);
        assertTrue(ttl.getValue().compareTo(Duration.ofSeconds(60)) <= 0);
        when(redis.hasKey(key.getValue())).thenReturn(true);
        assertTrue(blacklist.isBlacklisted("sensitive-bearer-token"));
    }

    @Test
    void expiredTokenNeedsNoRedisEntryButLogoutStillDeletesRefreshTokens() {
        ReflectionTestUtils.setField(jwt, "expiration", -1000L);
        String token = jwt.generateToken("alice", "CUSTOMER");
        auth.logout("alice", token);
        verifyNoInteractions(values);
        verify(refreshTokens).deleteByUsername("alice");
    }

    @Test
    void redisReadFailureRejectsRequestWith503() throws Exception {
        when(redis.hasKey(anyString())).thenThrow(new RedisConnectionFailureException("offline"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(request(jwt.generateToken("alice", "CUSTOMER")), response, chain);
        assertEquals(503, response.getStatus());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(chain);
    }

    @Test
    void redisWriteFailureDoesNotReportSuccessfulLogout() {
        doThrow(new RedisConnectionFailureException("offline"))
                .when(values).set(anyString(), anyString(), any(Duration.class));
        AppException error = assertThrows(AppException.class,
                () -> auth.logout("alice", jwt.generateToken("alice", "CUSTOMER")));
        assertEquals(ErrorCode.AUTH_SERVICE_UNAVAILABLE, error.getErrorCode());
        verifyNoInteractions(refreshTokens);
    }

    @Test
    void invalidJwtIsRejectedWithoutConsultingRedis() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(request("invalid-token"), response, chain);
        assertEquals(401, response.getStatus());
        verify(redis, never()).hasKey(anyString());
        verifyNoInteractions(chain);
    }

    @Test
    void requestWithoutBearerContinuesWithoutRedisLookup() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        verify(redis, never()).hasKey(anyString());
    }

    private MockHttpServletRequest request(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }

    private void assertAllowed(String token) throws Exception {
        SecurityContextHolder.clearContext();
        MockHttpServletRequest request = request(token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        assertEquals("alice", SecurityContextHolder.getContext().getAuthentication().getName());
    }
}

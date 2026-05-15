package com.SmartBank.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final int MAX_ATTEMPT = 5;
    private static final long LOCK_TIME_DURATION = 15; // minutes
    private static final String ATTEMPT_PREFIX = "LOGIN_ATTEMPT_";
    private static final String LOCK_PREFIX = "LOGIN_LOCK_";

    public void loginSucceeded(String key) {
        redisTemplate.delete(ATTEMPT_PREFIX + key);
        redisTemplate.delete(LOCK_PREFIX + key);
    }

    public void loginFailed(String key) {
        String attemptKey = ATTEMPT_PREFIX + key;
        Integer attempts = (Integer) redisTemplate.opsForValue().get(attemptKey);
        
        if (attempts == null) {
            attempts = 0;
        }
        attempts++;
        
        redisTemplate.opsForValue().set(attemptKey, attempts, 1, TimeUnit.HOURS);

        if (attempts >= MAX_ATTEMPT) {
            redisTemplate.opsForValue().set(LOCK_PREFIX + key, "locked", LOCK_TIME_DURATION, TimeUnit.MINUTES);
        }
    }

    public boolean isBlocked(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(LOCK_PREFIX + key));
    }
}

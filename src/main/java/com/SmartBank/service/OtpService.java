package com.SmartBank.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String OTP_PREFIX = "OTP_";
    private static final int OTP_LENGTH = 6;
    private static final long OTP_EXPIRATION = 5; // minutes

    public String generateOtp(String username) {
        SecureRandom random = new SecureRandom();
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < OTP_LENGTH; i++) {
            otp.append(random.nextInt(10));
        }

        String code = otp.toString();
        redisTemplate.opsForValue().set(OTP_PREFIX + username, code, OTP_EXPIRATION, TimeUnit.MINUTES);
        
        // In a real app, send via Email/SMS
        log.info("OTP for user {}: {}", username, code);
        
        return code;
    }

    public boolean validateOtp(String username, String code) {
        String storedOtp = (String) redisTemplate.opsForValue().get(OTP_PREFIX + username);
        if (storedOtp != null && storedOtp.equals(code)) {
            redisTemplate.delete(OTP_PREFIX + username);
            return true;
        }
        return false;
    }
}

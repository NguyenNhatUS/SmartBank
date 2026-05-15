package com.SmartBank.security;

import com.SmartBank.entity.enums.ErrorCode;
import com.SmartBank.exception.AppException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.TimeUnit;

@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final RedisTemplate<String, Object> redisTemplate;

    @Before("@annotation(rateLimit)")
    public void checkRateLimit(RateLimit rateLimit) {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest();
        String key = generateKey(request);

        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, rateLimit.duration(), TimeUnit.SECONDS);
        }

        if (count != null && count > rateLimit.requests()) {
            throw new AppException(ErrorCode.TOO_MANY_REQUESTS);
        }
    }

    private String generateKey(HttpServletRequest request) {
        // Use IP address + URI as the key
        String remoteAddr = request.getRemoteAddr();
        String uri = request.getRequestURI();
        return "rate_limit:" + remoteAddr + ":" + uri;
    }
}

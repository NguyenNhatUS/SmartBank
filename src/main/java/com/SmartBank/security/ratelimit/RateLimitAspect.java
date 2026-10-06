package com.SmartBank.security.ratelimit;

import com.SmartBank.exception.ErrorCode;
import com.SmartBank.exception.AppException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.util.List;

@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private static final RedisScript<Long> RATE_LIMIT_SCRIPT = RedisScript.of(
            "local current = redis.call('incr', KEYS[1])\n" +
            "if current == 1 then\n" +
            "    redis.call('expire', KEYS[1], ARGV[1])\n" +
            "end\n" +
            "return current;",
            Long.class
    );

    private final RedisTemplate<String, Object> redisTemplate;

    @Before("@annotation(rateLimit)")
    public void checkRateLimit(RateLimit rateLimit) {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest();
        String key = generateKey(request);

        Long count = redisTemplate.execute(
                RATE_LIMIT_SCRIPT,
                List.of(key),
                String.valueOf(rateLimit.duration())
        );

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

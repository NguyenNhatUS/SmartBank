package com.SmartBank.security.ratelimit;

import com.SmartBank.exception.ErrorCode;
import com.SmartBank.exception.AppException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.util.List;

@Slf4j
@Aspect
@Component
public class RateLimitAspect {

    private static final RedisScript<Long> RATE_LIMIT_SCRIPT = RedisScript.of(
            "local current = redis.call('incr', KEYS[1])\n" +
            "if current == 1 then\n" +
            "    redis.call('expire', KEYS[1], ARGV[1])\n" +
            "end\n" +
            "return current;",
            Long.class
    );

    private final StringRedisTemplate redisTemplate;

    public RateLimitAspect(
            @Qualifier("rateLimitRedisTemplate") StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Before("@annotation(rateLimit)")
    public void checkRateLimit(RateLimit rateLimit) {
        try {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                    .getRequest();
            String key = generateKey(request);

            log.info("[RateLimit] Key: {}, Max requests: {}, Duration: {}s", key, rateLimit.requests(), rateLimit.duration());

            Long count = redisTemplate.execute(
                    RATE_LIMIT_SCRIPT,
                    List.of(key),
                    String.valueOf(rateLimit.duration())
            );

            log.info("[RateLimit] Current count: {}", count);

            if (count != null && count > rateLimit.requests()) {
                log.warn("[RateLimit] BLOCKED! count={} > limit={}", count, rateLimit.requests());
                throw new AppException(ErrorCode.TOO_MANY_REQUESTS);
            }
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("[RateLimit] ERROR in rate limit check: {}", e.getMessage(), e);
            throw e;
        }
    }

    private String generateKey(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        String uri = request.getRequestURI();
        return "rate_limit:" + remoteAddr + ":" + uri;
    }
}

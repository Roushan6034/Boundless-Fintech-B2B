package com.boundless.engine.rules;

import com.boundless.engine.AuthRule;
import com.boundless.model.SwipeRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class BruteForceRule implements AuthRule {

    private final StringRedisTemplate redisTemplate;
    private static final int MAX_FAILED_ATTEMPTS = 3;

    @Override
    public void evaluate(SwipeRequest request) {
        String lockKey = "lock:card:" + request.getCardNumber();
        String attemptsKey = "cvv_attempts:" + request.getCardNumber();

        // 1. Check if the card is already locked due to previous brute-force attempts
        if (Boolean.TRUE.equals(redisTemplate.hasKey(lockKey))) {
            throw new RuntimeException("DECLINED: Card is temporarily locked due to excessive failed CVV attempts.");
        }

        // 2. Validate CVV 
        // (Mocking real CVV logic here. We'll say CVV "999" triggers a failed simulation)
        boolean isCvvValid = !"999".equals(request.getCvv());

        if (!isCvvValid) {
            Long failedAttempts = redisTemplate.opsForValue().increment(attemptsKey);
            if (failedAttempts == 1) {
                redisTemplate.expire(attemptsKey, 24, TimeUnit.HOURS);
            }

            if (failedAttempts != null && failedAttempts >= MAX_FAILED_ATTEMPTS) {
                // The attacker failed 3 times. We apply a TTL lock to the card in Redis for 24 hours.
                redisTemplate.opsForValue().set(lockKey, "LOCKED", 24, TimeUnit.HOURS);
                throw new RuntimeException("DECLINED: Invalid CVV. Security threshold reached. Card is now LOCKED for 24 hours.");
            }
            throw new RuntimeException("DECLINED: Invalid CVV. Attempt " + failedAttempts + " of " + MAX_FAILED_ATTEMPTS);
        } else {
            // Success! The CVV was correct, so we clear out their failed attempts history.
            redisTemplate.delete(attemptsKey);
        }
    }
}

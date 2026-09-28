package com.boundless.engine.rules;

import com.boundless.engine.AuthRule;
import com.boundless.model.SwipeRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class VelocityRule implements AuthRule {

    private final StringRedisTemplate redisTemplate;
    private static final int MAX_SWIPES_PER_DAY = 15;

    @Override
    public void evaluate(SwipeRequest request) {
        String key = "velocity:" + request.getCardNumber();
        
        // Atomically increment the counter in Redis
        Long currentSwipes = redisTemplate.opsForValue().increment(key);
        
        if (currentSwipes != null && currentSwipes == 1) {
            // First swipe of the day, set TTL to 24 hours
            redisTemplate.expire(key, 24, TimeUnit.HOURS);
        }
        
        if (currentSwipes != null && currentSwipes > MAX_SWIPES_PER_DAY) {
            throw new RuntimeException("DECLINED: High-Risk Velocity. Max " + MAX_SWIPES_PER_DAY + " swipes allowed per day.");
        }
    }
}

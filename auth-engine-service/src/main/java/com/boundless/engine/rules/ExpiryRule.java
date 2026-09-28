package com.boundless.engine.rules;

import com.boundless.engine.AuthRule;
import com.boundless.model.SwipeRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExpiryRule implements AuthRule {

    private final StringRedisTemplate redisTemplate;

    @Override
    public void evaluate(SwipeRequest request) {
        if (request.getExpiryDate() == null || request.getExpiryDate().trim().isEmpty()) {
            throw new RuntimeException("DECLINED: Expiry Date is missing in the request.");
        }

        String expectedExpiry = redisTemplate.opsForValue().get("card:" + request.getCardNumber() + ":expiry");
        
        if (expectedExpiry == null) {
            // For older cards created before this rule, we bypass or we can log a warning
            log.warn("No expiry date found in Redis for card {}", request.getCardNumber());
            return;
        }

        if (!expectedExpiry.equals(request.getExpiryDate())) {
            throw new RuntimeException("DECLINED: Invalid Expiry Date. Please check your card details.");
        }

        // We can also parse and check if the card is physically expired (in the past)
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/yy");
            YearMonth cardExpiry = YearMonth.parse(request.getExpiryDate(), formatter);
            YearMonth currentMonth = YearMonth.now();

            if (cardExpiry.isBefore(currentMonth)) {
                throw new RuntimeException("DECLINED: Card has expired.");
            }
        } catch (Exception e) {
            throw new RuntimeException("DECLINED: Invalid Expiry Date format. Expected MM/YY.");
        }
    }
}

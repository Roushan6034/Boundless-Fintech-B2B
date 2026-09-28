package com.boundless.engine.rules;

import com.boundless.engine.AuthRule;
import com.boundless.model.SwipeRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class RbiLimitRule implements AuthRule {

    private final StringRedisTemplate redisTemplate;

    // The Reserve Bank of India mandates an OTP for transactions over ₹5,000
    private static final BigDecimal RBI_LIMIT = new BigDecimal("5000.00");

    @Override
    public void evaluate(SwipeRequest request) {
        // If the user has already verified an OTP in the last 5 minutes, bypass this rule
        if (Boolean.TRUE.equals(redisTemplate.hasKey("otp_verified:" + request.getCardNumber()))) {
            return;
        }

        // Perform fast currency conversion to INR to properly evaluate the RBI limit
        String swipeCurrency = request.getCurrency() != null ? request.getCurrency().toUpperCase() : "USD";
        BigDecimal conversionRate = getFxRate(swipeCurrency, "INR");
        BigDecimal convertedAmount = request.getAmount().multiply(conversionRate);

        // Evaluate against the ₹5000 limit using the converted INR amount
        if (convertedAmount.compareTo(RBI_LIMIT) >= 0) {
            // Instead of a hard DECLINE, this triggers a specialized response flow back to the Gateway.
            // The AuthEngineController will catch this exact exception string and return a PENDING_OTP HTTP status.
            throw new RuntimeException("PENDING_OTP: Transaction exceeds ₹5,000 RBI limit. Email Challenge triggered.");
        }
    }

    // Ultra-fast simulated FX cache lookup for Auth Engine (< 5ms)
    private BigDecimal getFxRate(String from, String to) {
        if (from.equals(to)) return BigDecimal.ONE;
        if (from.equals("USD") && to.equals("INR")) return new BigDecimal("83.50");
        if (from.equals("EUR") && to.equals("INR")) return new BigDecimal("91.00");
        return BigDecimal.ONE;
    }
}

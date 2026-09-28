package com.boundless.engine.rules;

import com.boundless.engine.AuthRule;
import com.boundless.model.SwipeRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class ThreeDSecureRule implements AuthRule {

    private final StringRedisTemplate redisTemplate;

    @Override
    public void evaluate(SwipeRequest request) {
        
        // 1. Frictionless Bypass: If the user already passed an OTP challenge recently.
        if (Boolean.TRUE.equals(redisTemplate.hasKey("otp_verified:" + request.getCardNumber()))) {
            return;
        }

        int riskScore = 0;





        // -------------------------------------------------------------------------
        // B. AVS (Address Verification System) MISMATCH
        // -------------------------------------------------------------------------
        // If the merchant provided a zip code, we cross-reference it with Redis.
        if (request.getBillingZipCode() != null) {
            String expectedZip = redisTemplate.opsForValue().get("card:" + request.getCardNumber() + ":zip");
            
            if (expectedZip != null && !expectedZip.equals(request.getBillingZipCode())) {
                riskScore += 60; // Huge red flag! Hacker doesn't know where the victim lives.
                log.warn("3DS 2.0: AVS Zip Code Mismatch for Card {}. Expected: {}, Provided: {}", 
                        request.getCardNumber(), expectedZip, request.getBillingZipCode());
            } else if (expectedZip != null && expectedZip.equals(request.getBillingZipCode())) {
                log.info("3DS 2.0: AVS Zip Code matched! Reducing risk profile.");
                riskScore -= 10; // Positive reinforcement!
            }
        }

        // -------------------------------------------------------------------------
        // C. TIME-OF-DAY ANOMALY (Corporate Hours Check)
        // -------------------------------------------------------------------------
        if (request.getLocalTime() != null) {
            try {
                java.time.LocalTime time = java.time.LocalTime.parse(request.getLocalTime());
                // If it's between 11:00 PM (23:00) and 5:00 AM (05:00)
                if (time.isAfter(java.time.LocalTime.of(23, 0)) || time.isBefore(java.time.LocalTime.of(5, 0))) {
                    riskScore += 40;
                    log.warn("3DS 2.0: Time Anomaly! Corporate swipe attempted at {} (Outside Business Hours).", time);
                }
            } catch (Exception e) {
                log.warn("3DS 2.0: Invalid localTime format provided: {}", request.getLocalTime());
            }
        }

        // -------------------------------------------------------------------------
        // D. THE "MICRO-TEST BREAKOUT" PATTERN (Card Testing)
        // -------------------------------------------------------------------------
        
        // Convert to standard USD for anomaly detection thresholds
        String swipeCurrency = request.getCurrency() != null ? request.getCurrency().toUpperCase() : "USD";
        BigDecimal amountInUsd = request.getAmount().multiply(getFxRateToUsd(swipeCurrency));

        // Hackers test a stolen card with a ₹1 charge. If it works, they immediately 
        // buy a ₹1000 item before the card is locked.
        String lastSwipeAmountStr = redisTemplate.opsForValue().get("card:" + request.getCardNumber() + ":last_amount_usd");
        
        if (lastSwipeAmountStr != null) {
            BigDecimal lastAmountUsd = new BigDecimal(lastSwipeAmountStr);
            // If the last swipe was tiny (< ₹2.00) AND this new swipe is huge (> ₹500.00)
            if (lastAmountUsd.compareTo(new BigDecimal("2.00")) < 0 && amountInUsd.compareTo(new BigDecimal("500.00")) > 0) {
                riskScore += 80;
                log.error("3DS 2.0: MICRO-TEST BREAKOUT DETECTED! Card {} did a ₹{} test, now attempting ₹{}", 
                        request.getCardNumber(), lastAmountUsd, amountInUsd);
            }
        }
        
        // Save THIS swipe's USD amount for the next time (expires in 30 minutes, since breakouts happen fast)
        redisTemplate.opsForValue().set("card:" + request.getCardNumber() + ":last_amount_usd", 
                amountInUsd.toString(), 30, TimeUnit.MINUTES);

        // -------------------------------------------------------------------------
        // E. UNUSUAL AMOUNT ANOMALY
        // -------------------------------------------------------------------------
        if (amountInUsd.compareTo(new BigDecimal("1000.00")) > 0) {
            riskScore += 20;
        }

        // -------------------------------------------------------------------------
        // THE DECISION
        // -------------------------------------------------------------------------
        log.info("3DS 2.0 Risk Score for Card {}: {}", request.getCardNumber(), riskScore);
        
        if (riskScore >= 60) {
            throw new RuntimeException("PENDING_OTP: 3DS 2.0 Risk Score is " + riskScore + " (High). Identity Challenge triggered.");
        }
    }

    private BigDecimal getFxRateToUsd(String from) {
        if ("USD".equals(from)) return BigDecimal.ONE;
        if ("INR".equals(from)) return new BigDecimal("0.012");
        if ("EUR".equals(from)) return new BigDecimal("1.09");
        return BigDecimal.ONE;
    }
}

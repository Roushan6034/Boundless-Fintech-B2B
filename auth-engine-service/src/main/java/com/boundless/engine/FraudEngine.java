package com.boundless.engine;

import com.boundless.model.SwipeRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FraudEngine {

    // Spring Boot magically injects ALL classes that implement AuthRule (MccRule, VelocityRule)
    // This automatically forms our Chain of Responsibility pipeline!
    private final List<AuthRule> rules;

    public void processSwipe(SwipeRequest request) {
        log.info("⚡ Evaluating swipe for Card {} at {} for ${}", 
                request.getCardNumber(), request.getMerchantName(), request.getAmount());
        
        // Run the swipe through every rule in the chain
        for (AuthRule rule : rules) {
            rule.evaluate(request); // Will throw an exception if declined
        }
        
        log.info("✅ All Fraud Rules Passed for Card {}", request.getCardNumber());
    }
}

package com.boundless.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class SagaCompensationListener {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "transaction-failed-compensation-topic", groupId = "auth-engine-saga-group")
    public void handleSagaCompensation(String message) {
        try {
            log.warn("🚨 SAGA COMPENSATING TRANSACTION INITIATED: Received failure event from Ledger Service.");
            
            JsonNode payload = objectMapper.readTree(message);
            String cardNumber = payload.get("cardNumber").asText();
            BigDecimal amountToRefund = new BigDecimal(payload.get("amount").asText());
            
            // 1. Fetch the wallet ID routing from Redis
            String walletIdStr = redisTemplate.opsForValue().get("card:" + cardNumber + ":wallet");
            
            if (walletIdStr != null) {
                // 2. Perform the exact inverse of DECRBYFLOAT to refund the wallet!
                String balanceKey = "wallet:" + walletIdStr + ":balance";
                redisTemplate.opsForValue().increment(balanceKey, amountToRefund.doubleValue());
                
                log.info("✅ SAGA COMPLETED: Successfully executed compensating INCRBYFLOAT for Wallet {} (Refunded ${})", 
                        walletIdStr, amountToRefund);
            } else {
                log.error("❌ SAGA FAILED: Could not locate Wallet routing in Redis for Card {}", cardNumber);
            }

        } catch (Exception e) {
            log.error("💥 CRITICAL SAGA FAILURE: Could not process compensating transaction: {}", e.getMessage());
        }
    }
}

package com.boundless.controller;

import com.boundless.engine.FraudEngine;
import com.boundless.model.SwipeRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.redis.core.StringRedisTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/api/network")
@RequiredArgsConstructor
public class AuthEngineController {

    private final FraudEngine fraudEngine;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // This is the Webhook that Visa/Mastercard hits in real-time
    @PostMapping("/swipe")
    public ResponseEntity<?> handleNetworkSwipe(@RequestBody SwipeRequest request) {
        boolean usedOtpBypass = Boolean.TRUE.equals(redisTemplate.hasKey("otp_verified:" + request.getCardNumber()));
        
        try {
            // 1. Run the Chain of Responsibility
            fraudEngine.processSwipe(request);
            
            // 2. [Next Step] Deduct funds using BalanceRule
            
            // 3. Drop an AuthApprovedEvent into Kafka for the Ledger to save permanently
            String currency = request.getCurrency() != null ? request.getCurrency() : "USD";
            String event = String.format("{\"cardNumber\":\"%s\", \"merchantName\":\"%s\", \"amount\":\"%s\", \"currency\":\"%s\"}", 
                    request.getCardNumber(), request.getMerchantName(), request.getAmount().toString(), currency);
            kafkaTemplate.send("auth-approved-topic", event);

            // 4. BURNER CARD LOGIC: If this is a single-use card, destroy it immediately!
            String cardType = redisTemplate.opsForValue().get("card:" + request.getCardNumber() + ":type");
            if ("BURNER".equals(cardType)) {
                // Delete from Redis so it can never be used again
                redisTemplate.delete("card:" + request.getCardNumber() + ":wallet");
                redisTemplate.delete("card:" + request.getCardNumber() + ":status");
                redisTemplate.delete("card:" + request.getCardNumber() + ":type");
                
                // Drop event to Identity Service to mark it CLOSED in Postgres
                kafkaTemplate.send("card-closed-topic", request.getCardNumber());
                log.warn("🔥 BURNER CARD {} DESTROYED AFTER SINGLE USE!", request.getCardNumber());
            }

            return ResponseEntity.ok("APPROVED");

        } catch (RuntimeException ex) {
            String errorMessage = ex.getMessage();
            
            // ------------------------------------------------------------------
            // ⚡ OTP EMAIL DISPATCH LOGIC (RBI Rule Caught)
            // ------------------------------------------------------------------
            if (errorMessage != null && errorMessage.startsWith("PENDING_OTP")) {
                
                // 1. Generate a 6-digit OTP
                String otp = String.format("%06d", (int)(Math.random() * 999999));
                
                // 2. Save OTP to Redis with a 5-minute TTL so the user can verify it later
                redisTemplate.opsForValue().set("otp:" + request.getCardNumber(), otp, 5, TimeUnit.MINUTES);
                
                // 2.5 Save the original SwipeRequest so we can auto-process it upon verification!
                try {
                    String requestJson = objectMapper.writeValueAsString(request);
                    redisTemplate.opsForValue().set("pending_tx:" + request.getCardNumber(), requestJson, 5, TimeUnit.MINUTES);
                } catch (Exception e) {
                    log.error("Failed to cache pending transaction");
                }
                
                // 3. FIRE AND FORGET: Drop event into Kafka to send the actual Email async
                String kafkaMessage = String.format("{\"cardNumber\":\"%s\", \"otp\":\"%s\"}", request.getCardNumber(), otp);
                kafkaTemplate.send("otp-email-topic", kafkaMessage);
                
                log.info("RBI Limit exceeded for card {}. OTP Email dispatched to Kafka.", request.getCardNumber());
                
                // 4. Tell Visa the transaction is pending user action
                return ResponseEntity.status(202).body("PENDING_OTP: Please check your email for the 6-digit verification code.");
            }
            
            // ------------------------------------------------------------------
            // 🚨 UNIVERSAL INTERNAL OPS PROACTIVE ALERTING
            // ------------------------------------------------------------------
            if (errorMessage != null) {
                String role;
                String targetEmail;
                String alertBody;

                // Group 1: High-Risk AML/Fraud (Route to Risk & Compliance)
                if (errorMessage.contains("BRUTE_FORCE") || errorMessage.contains("MCC") || errorMessage.contains("VELOCITY")) {
                    role = "ROLE_PLATFORM_COMPLIANCE";
                    targetEmail = "compliance@boundless.com";
                    alertBody = String.format("SECURITY INCIDENT: Swipe declined for %s. Reason: %s. Please review card %s immediately.", 
                            request.getMerchantName(), errorMessage, request.getCardNumber());
                } 
                // Group 2: User Errors / Status (Route to Customer Success)
                else {
                    role = "ROLE_PLATFORM_SUPPORT";
                    targetEmail = "support@boundless.com";
                    alertBody = String.format("USER FRICTION: Swipe declined at %s. Reason: %s. The employee (Card %s) may call in for help.", 
                            request.getMerchantName(), errorMessage, request.getCardNumber());
                }

                String alertJson = String.format("{\"role\":\"%s\", \"targetEmail\":\"%s\", \"alert\":\"%s\"}", 
                        role, targetEmail, alertBody);
                kafkaTemplate.send("internal-alert-topic", alertJson);
                log.info("🚨 Proactive Alert Routed to {}: {}", role, errorMessage);
            }
            
            // Finally, return the decline response to Visa
            log.warn("Swipe Declined: {}", errorMessage);
            return ResponseEntity.badRequest().body(errorMessage);
        } finally {
            // 5. Consume OTP Flag UNCONDITIONALLY: If they had an active OTP bypass, 
            // consume it even if the transaction failed (e.g. insufficient funds) so it's strictly one-time use!
            if (usedOtpBypass) {
                redisTemplate.delete("otp_verified:" + request.getCardNumber());
            }
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestParam String cardNumber, @RequestParam String otp) {
        String savedOtp = redisTemplate.opsForValue().get("otp:" + cardNumber);
        if (savedOtp != null && savedOtp.equals(otp)) {
            // OTP is correct! Set a flag that they are verified for the next 5 minutes
            redisTemplate.opsForValue().set("otp_verified:" + cardNumber, "true", 5, TimeUnit.MINUTES);
            // Delete the old OTP so it can't be reused
            redisTemplate.delete("otp:" + cardNumber);
            
            // Check if we have a pending transaction to auto-process!
            String pendingTxJson = redisTemplate.opsForValue().get("pending_tx:" + cardNumber);
            if (pendingTxJson != null) {
                try {
                    SwipeRequest originalRequest = objectMapper.readValue(pendingTxJson, SwipeRequest.class);
                    log.info("Auto-processing cached transaction for card {} after successful OTP!", cardNumber);
                    
                    // Delete the pending cache to prevent duplicate processing
                    redisTemplate.delete("pending_tx:" + cardNumber);
                    
                    // Re-route it through the swipe endpoint
                    ResponseEntity<?> autoProcessResult = handleNetworkSwipe(originalRequest);
                    
                    if (autoProcessResult.getStatusCode().is2xxSuccessful()) {
                        return ResponseEntity.ok("OTP Verified! Transaction automatically processed and APPROVED.");
                    } else {
                        return ResponseEntity.badRequest().body("OTP Verified, but transaction subsequently failed: " + autoProcessResult.getBody());
                    }
                } catch (Exception e) {
                    log.error("Failed to auto-process transaction", e);
                }
            }
            
            return ResponseEntity.ok("OTP Verified! You may now retry the transaction.");
        }
        return ResponseEntity.badRequest().body("Invalid or expired OTP.");
    }
}

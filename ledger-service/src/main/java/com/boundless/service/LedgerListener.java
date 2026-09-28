package com.boundless.service;

import com.boundless.entity.Transaction;
import com.boundless.repository.TransactionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LedgerListener {

    private final TransactionRepository transactionRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final SimpMessagingTemplate websocket;
    private final AuditService auditService;
    private final WebhookService webhookService;
    private final CurrencyService currencyService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "auth-approved-topic", groupId = "ledger-settlement-group")
    public void processSettlement(String message) {
        try {
            log.info("📥 Received Auth Event from Kafka: {}", message);
            
            JsonNode payload = objectMapper.readTree(message);
            String cardNumber = payload.get("cardNumber").asText();
            BigDecimal amount = new BigDecimal(payload.get("amount").asText());
            String merchantName = payload.get("merchantName").asText();
            
            // Simulated DB crash for Saga testing
            if (amount.compareTo(new BigDecimal("13.37")) == 0) {
                throw new RuntimeException("Simulated PostgreSQL Connection Timeout / Crash!");
            }

            // Feature 4: Currency Conversion
            String originalCurrency = payload.has("currency") ? payload.get("currency").asText() : "USD";
            String settledCurrency = "INR";
            BigDecimal exchangeRate = currencyService.getRate(originalCurrency, settledCurrency);
            BigDecimal settledAmount = currencyService.convert(amount, originalCurrency, settledCurrency);
            
            // The receipt SLA rule is designed for purchases above ₹50.00 USD.
            // Since our base ledger currency is INR, we evaluate the threshold at ₹4,000 INR.
            BigDecimal receiptThresholdInr = new BigDecimal("4000.00");
            boolean requiresReceipt = settledAmount.compareTo(receiptThresholdInr) > 0;
            
            Transaction tx = Transaction.builder()
                    .cardNumber(cardNumber)
                    .merchantName(merchantName)
                    .amount(settledAmount)
                    .originalCurrency(originalCurrency)
                    .exchangeRate(exchangeRate)
                    .settledCurrency(settledCurrency)
                    .status("SETTLED")
                    .timestamp(LocalDateTime.now())
                    .receiptUploaded(!requiresReceipt)
                    .receiptDeadline(requiresReceipt ? LocalDateTime.now().plusHours(24) : null)
                    .build();

            transactionRepository.save(tx);
            log.info("✅ Transaction settled. ID: {}", tx.getId());

            // Feature 6: Audit Trail
            auditService.log("TRANSACTION_SETTLED", "Transaction", tx.getId().toString(),
                    cardNumber, String.format("₹%s %s → ₹%s INR @ rate %s at %s", 
                    amount, originalCurrency, settledAmount, exchangeRate, merchantName));

            // Feature 1: WebSocket Real-Time Push
            websocket.convertAndSend("/topic/transactions", (Object) Map.of(
                    "id", tx.getId().toString(),
                    "card", cardNumber,
                    "merchant", merchantName,
                    "amount", settledAmount.toString(),
                    "currency", settledCurrency,
                    "originalAmount", amount.toString(),
                    "originalCurrency", originalCurrency,
                    "status", "SETTLED",
                    "timestamp", tx.getTimestamp().toString()
            ));

            // Feature 7: Webhook Notification
            try {
                // Lookup company name from Identity Service
                String companyName = new RestTemplate()
                        .getForObject("http://localhost:8081/api/internal/cards/" + cardNumber + "/company", String.class);
                if (companyName != null) {
                    webhookService.fire(companyName, "TRANSACTION_APPROVED", Map.of(
                            "transactionId", tx.getId().toString(),
                            "cardLastFour", cardNumber.substring(cardNumber.length() - 4),
                            "amount", settledAmount.toString(), "currency", settledCurrency,
                            "merchant", merchantName, "status", "SETTLED"
                    ));
                }
            } catch (Exception e) {
                log.debug("Webhook company lookup skipped: {}", e.getMessage());
            }

            // Receipt reminder
            if (requiresReceipt) {
                String reminderEvent = String.format("{\"cardNumber\":\"%s\", \"amount\":\"%s\", \"merchantName\":\"%s\"}", 
                        cardNumber, amount, merchantName);
                kafkaTemplate.send("receipt-reminder-topic", reminderEvent);
            }

        } catch (Exception e) {
            log.error("❌ Settlement failed: {}. Initiating Saga Compensation!", e.getMessage());
            kafkaTemplate.send("transaction-failed-compensation-topic", message);
        }
    }
}

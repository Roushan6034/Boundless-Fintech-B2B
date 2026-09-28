package com.boundless.service;

import com.boundless.entity.Transaction;
import com.boundless.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiptSlaCronJob {

    private final TransactionRepository transactionRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    // A Cron Job that runs at the top of every single hour (e.g., 1:00, 2:00, 3:00)
    @Scheduled(cron = "0 0 * * * *")
    public void enforceReceiptSla() {
        log.info("🔍 Running 24-Hour Receipt Compliance Audit...");

        // 1. Query PostgreSQL for any transactions that are older than 24 hours missing a receipt
        List<Transaction> breachedTransactions = transactionRepository
                .findByReceiptUploadedFalseAndReceiptDeadlineBefore(LocalDateTime.now());

        if (breachedTransactions.isEmpty()) {
            log.info("✅ All employees are compliant with their receipt SLAs.");
            return;
        }

        // 2. Penalize the non-compliant employees!
        for (Transaction tx : breachedTransactions) {
            log.warn("🚨 SLA BREACH: Card {} missed the 24-hour receipt deadline for a ${} purchase at {}.", 
                    tx.getCardNumber(), tx.getAmount(), tx.getMerchantName());

            // Fire an event to the Card & Identity Service to instantly FREEZE the employee's card
            String freezeEvent = String.format("{\"cardNumber\":\"%s\", \"reason\":\"RECEIPT_SLA_BREACH\"}", tx.getCardNumber());
            kafkaTemplate.send("card-freeze-topic", freezeEvent);

            // Fire an event to the Notification Service to email the Employee a strict warning
            kafkaTemplate.send("compliance-warning-topic", freezeEvent);
        }
        
        log.info("🔒 Dispatched freeze events for {} non-compliant cards.", breachedTransactions.size());
    }
}

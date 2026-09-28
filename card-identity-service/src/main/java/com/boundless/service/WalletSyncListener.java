package com.boundless.service;

import com.boundless.entity.VirtualCard;
import com.boundless.entity.Wallet;
import com.boundless.repository.VirtualCardRepository;
import com.boundless.repository.WalletRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletSyncListener {

    private final VirtualCardRepository virtualCardRepository;
    private final WalletRepository walletRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    @KafkaListener(topics = "auth-approved-topic", groupId = "identity-wallet-sync-group")
    public void syncWalletBalance(String message) {
        try {
            log.info("📥 Identity Service Received Auth Event to sync Wallet Balance: {}", message);
            
            JsonNode payload = objectMapper.readTree(message);
            String cardNumber = payload.get("cardNumber").asText();
            BigDecimal amount = new BigDecimal(payload.get("amount").asText());

            Optional<VirtualCard> cardOpt = virtualCardRepository.findByCardNumber(cardNumber);
            
            if (cardOpt.isPresent()) {
                Wallet wallet = cardOpt.get().getWallet();
                BigDecimal newBalance = wallet.getBalance().subtract(amount);
                wallet.setBalance(newBalance);
                walletRepository.save(wallet);
                log.info("✅ Postgres Wallet {} synced! New Balance: ${}", wallet.getId(), newBalance);
            } else {
                log.warn("⚠️ Could not find Postgres card {} to sync wallet balance.", cardNumber);
            }
        } catch (Exception e) {
            log.error("❌ Failed to sync Postgres wallet balance: {}", e.getMessage());
        }
    }
}

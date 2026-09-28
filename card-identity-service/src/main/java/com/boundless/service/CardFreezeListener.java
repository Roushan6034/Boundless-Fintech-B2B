package com.boundless.service;

import com.boundless.entity.VirtualCard;
import com.boundless.repository.VirtualCardRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CardFreezeListener {

    private final VirtualCardRepository virtualCardRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Listens to the exact topic our Ledger SLA Cron Job publishes to!
    @KafkaListener(topics = "card-freeze-topic", groupId = "card-identity-group")
    public void handleCardFreeze(String message) {
        try {
            JsonNode payload = objectMapper.readTree(message);
            String cardNumber = payload.get("cardNumber").asText();
            String reason = payload.get("reason").asText();

            Optional<VirtualCard> cardOpt = virtualCardRepository.findByCardNumber(cardNumber);
            
            if (cardOpt.isPresent()) {
                // 1. Permanently freeze the card in the PostgreSQL Database
                VirtualCard card = cardOpt.get();
                card.setStatus("FROZEN");
                virtualCardRepository.save(card);

                // 2. 🔴 CRITICAL: Freeze it in Redis so the ultra-fast Auth Engine instantly blocks swipes!
                redisTemplate.opsForValue().set("card:" + cardNumber + ":status", "FROZEN");

                log.warn("❄️ Card {} FROZEN due to: {}", cardNumber, reason);
            }
        } catch (Exception e) {
            log.error("Failed to freeze card: {}", e.getMessage());
        }
    }

    // NEW: Listen for receipt uploads to UNFREEZE the card
    @KafkaListener(topics = "card-unfreeze-topic", groupId = "card-identity-group")
    public void handleCardUnfreeze(String cardNumber) {
        try {
            Optional<VirtualCard> cardOpt = virtualCardRepository.findByCardNumber(cardNumber);
            
            if (cardOpt.isPresent()) {
                // 1. Unfreeze in Postgres
                VirtualCard card = cardOpt.get();
                card.setStatus("ACTIVE");
                virtualCardRepository.save(card);

                // 2. 🟢 CRITICAL: Unfreeze in Redis! (Or delete the FROZEN key)
                redisTemplate.delete("card:" + cardNumber + ":status");

                log.info("🔥 Card {} UNFROZEN because receipt was uploaded!", cardNumber);
            }
        } catch (Exception e) {
            log.error("Failed to unfreeze card: {}", e.getMessage());
        }
    }

    // Listen for BURNER card destruction
    @KafkaListener(topics = "card-closed-topic", groupId = "card-identity-group")
    public void handleCardClosed(String cardNumber) {
        try {
            Optional<VirtualCard> cardOpt = virtualCardRepository.findByCardNumber(cardNumber);
            if (cardOpt.isPresent()) {
                VirtualCard card = cardOpt.get();
                card.setStatus("CLOSED"); // Permanent death
                virtualCardRepository.save(card);
                log.info("💀 BURNER Card {} permanently CLOSED in PostgreSQL.", cardNumber);
            }
        } catch (Exception e) {
            log.error("Failed to close card: {}", e.getMessage());
        }
    }
}

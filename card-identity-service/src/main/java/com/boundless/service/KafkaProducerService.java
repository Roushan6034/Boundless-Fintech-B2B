package com.boundless.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void sendWelcomeEmailEvent(String email, String empId, String tempPassword) {
        // Constructing a simple JSON payload for the event
        String message = String.format("{\"email\":\"%s\", \"empId\":\"%s\", \"tempPassword\":\"%s\"}", 
                email, empId, tempPassword);
        
        // Publish the event to Kafka!
        // A separate Notification Microservice will listen to this topic and send the actual SMTP email.
        kafkaTemplate.send("employee-onboarding-topic", message);
        
        log.info("⚡ Kafka Event Published: Welcome email queued for {}", email);
    }
    
    public void sendInternalAlert(String role, String targetEmail, String alertMessage) {
        String message = String.format("{\"role\":\"%s\", \"targetEmail\":\"%s\", \"alert\":\"%s\"}", 
                role, targetEmail, alertMessage);
        
        kafkaTemplate.send("internal-alert-topic", message);
        log.info("🚨 Internal Alert Published to Kafka for {}", role);
    }

    public void sendCompanyApprovalEmail(String founderEmail, String companyName, String tempPassword) {
        String message = String.format("{\"email\":\"%s\", \"companyName\":\"%s\", \"tempPassword\":\"%s\"}", 
                founderEmail, companyName, tempPassword);
        kafkaTemplate.send("company-approval-topic", message);
        log.info("🎉 Company Approval Email Event published to Kafka for {}", founderEmail);
    }

    public void sendCardIssuedNotification(String email, String employeeName, String cardNumber, 
                                          String cvv, String expiryDate, String cardType, 
                                          String walletName, String balance) {
        String message = String.format(
            "{\"email\":\"%s\", \"employeeName\":\"%s\", \"cardNumber\":\"%s\", \"cvv\":\"%s\", " +
            "\"expiryDate\":\"%s\", \"cardType\":\"%s\", \"walletName\":\"%s\", \"balance\":\"%s\"}",
            email, employeeName, cardNumber, cvv, expiryDate, cardType, walletName, balance
        );
        kafkaTemplate.send("card-issued-topic", message);
        log.info("💳 Card Issued Notification Event published to Kafka for {}", email);
    }
}

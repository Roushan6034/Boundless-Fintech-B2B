package com.boundless.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailListener {

    // Spring Boot automatically injects this when spring-boot-starter-mail is in the pom.xml
    private final JavaMailSender mailSender;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ---------------------------------------------------------
    // 1. Listen for the Welcome Email Event
    // ---------------------------------------------------------
    @KafkaListener(topics = "employee-onboarding-topic", groupId = "notification-group")
    public void handleWelcomeEmail(String message) {
        try {
            // Parse the JSON dropped by the AdminController
            JsonNode payload = objectMapper.readTree(message);
            String email = payload.get("email").asText();
            String empId = payload.get("empId").asText();
            String tempPassword = payload.get("tempPassword").asText();

            String emailBody = String.format(
                "Welcome to Boundless Corporate Cards!\n\n" +
                "Your Employee ID is: %s\n" +
                "Your Temporary Password is: %s\n\n" +
                "Please log in to the dashboard to change your password and view your Virtual Wallet.",
                empId, tempPassword
            );

            sendEmail(email, "Welcome to Boundless!", emailBody);
        } catch (Exception e) {
            log.error("Failed to process welcome email event: {}", e.getMessage());
        }
    }

    // ---------------------------------------------------------
    // 1b. Listen for the Company Approval Email Event (Founder Credentials)
    // ---------------------------------------------------------
    @KafkaListener(topics = "company-approval-topic", groupId = "notification-group")
    public void handleCompanyApprovalEmail(String message) {
        try {
            JsonNode payload = objectMapper.readTree(message);
            String email = payload.get("email").asText();
            String companyName = payload.get("companyName").asText();
            String tempPassword = payload.get("tempPassword").asText();

            String emailBody = String.format(
                "Congratulations! Your company '%s' has been APPROVED by Boundless Compliance!\n\n" +
                "Here are your Company Administrator credentials:\n" +
                "• Login Email: %s\n" +
                "• Temporary Password: %s\n\n" +
                "Please log in to your dashboard at http://localhost:8080/api/auth/login to allocate budgets and issue virtual cards.",
                companyName, email, tempPassword
            );

            sendEmail(email, "Company Approved: Your Boundless Admin Credentials", emailBody);
            log.info("🎉 Sent founder credentials email to {}", email);
        } catch (Exception e) {
            log.error("Failed to process company approval email event: {}", e.getMessage());
        }
    }

    // ---------------------------------------------------------
    // 1c. Listen for Card Issued Event (Card Details + Usage Rules & Limits)
    // ---------------------------------------------------------
    @KafkaListener(topics = "card-issued-topic", groupId = "notification-group")
    public void handleCardIssuedNotification(String message) {
        try {
            JsonNode payload = objectMapper.readTree(message);
            String email = payload.get("email").asText();
            String employeeName = payload.get("employeeName").asText();
            String cardNumber = payload.get("cardNumber").asText();
            String cvv = payload.get("cvv").asText();
            String expiryDate = payload.get("expiryDate").asText();
            String cardType = payload.get("cardType").asText();
            String walletName = payload.get("walletName").asText();
            String balance = payload.get("balance").asText();

            String maskedCard = "****-****-****-" + cardNumber.substring(cardNumber.length() - 4);

            String emailBody = String.format(
                "Hello %s,\n\n" +
                "A new Boundless Corporate Virtual Card has been issued for your '%s' wallet!\n\n" +
                "====================================================\n" +
                "💳 CARD DETAILS:\n" +
                "• Card Number (PAN): %s\n" +
                "• CVV: %s\n" +
                "• Expiry Date: %s\n" +
                "• Card Type: %s %s\n" +
                "• Assigned Budget: ₹%s\n" +
                "====================================================\n\n" +
                "🛡️ CORPORATE USAGE RULES & FRAUD LIMITATIONS:\n" +
                "1. Velocity Limit: Maximum 5 swipes per rolling 24-hour window.\n" +
                "2. Restricted Merchants (MCC): Gambling/Casinos (7995) and Bars/Nightclubs (5813) are strictly BLOCKED.\n" +
                "3. Large Transaction Verification: Transactions above ₹5,000 / ₹60 require email OTP verification.\n" +
                "4. Receipt SLA: Any purchase above ₹50.00 requires a receipt upload within 24 hours to prevent auto-freeze.\n" +
                "5. Burner Cards: If marked as BURNER, the card will self-destruct immediately after the first swipe.\n\n" +
                "You can view your card details anytime in your employee portal.\n\n" +
                "— Boundless FinTech Corporate Treasury Team",
                employeeName, walletName, cardNumber, cvv, expiryDate, cardType,
                ("BURNER".equals(cardType) ? "(Single-Use Only)" : "(Multi-Use)"),
                balance
            );

            sendEmail(email, "New Virtual Card Issued: " + maskedCard + " (Usage Rules & Limitations)", emailBody);
            log.info("💳 Dispatched virtual card details and policy guidelines to {}", email);

        } catch (Exception e) {
            log.error("Failed to process card issued notification: {}", e.getMessage());
        }
    }

    // ---------------------------------------------------------
    // 2. Listen for the OTP Email Event (RBI Limit)
    // ---------------------------------------------------------
    @KafkaListener(topics = "otp-email-topic", groupId = "notification-group")
    public void handleOtpEmail(String message) {
        try {
            // Parse the JSON dropped by the AuthEngineController
            JsonNode payload = objectMapper.readTree(message);
            String cardNumber = payload.get("cardNumber").asText();
            String otp = payload.get("otp").asText();

            // Mask the card number for security (e.g., ****-****-****-1234)
            String maskedCard = "****-****-****-" + cardNumber.substring(cardNumber.length() - 4);
            
            String emailBody = String.format(
                "SECURITY ALERT: Large Transaction Attempt\n\n" +
                "A swipe exceeding ₹5,000 was attempted on your card %s.\n\n" +
                "Your 6-digit Verification Code is: %s\n\n" +
                "This code will expire in 5 minutes. Do not share this code with anyone.",
                maskedCard, otp
            );

            // In a real system, we would query the DB for the employee's email using the card number.
            // For testing, we are routing this directly to your email!
            sendEmail("sidharth6034@gmail.com", "Action Required: Transaction OTP", emailBody);
        } catch (Exception e) {
            log.error("Failed to process OTP email event: {}", e.getMessage());
        }
    }

    // ---------------------------------------------------------
    // 3. Listen for the Receipt Required Event
    // ---------------------------------------------------------
    @KafkaListener(topics = "receipt-reminder-topic", groupId = "notification-group")
    public void handleReceiptReminder(String message) {
        try {
            JsonNode payload = objectMapper.readTree(message);
            String cardNumber = payload.get("cardNumber").asText();
            String amount = payload.get("amount").asText();
            String merchantName = payload.get("merchantName").asText();

            String maskedCard = "****-****-****-" + cardNumber.substring(cardNumber.length() - 4);
            
            String emailBody = String.format(
                "ACTION REQUIRED: Missing Receipt\n\n" +
                "You recently made a purchase of ₹%s at %s using your card %s.\n\n" +
                "Because this amount exceeds the company threshold, you must upload a receipt within 24 hours.\n" +
                "Failure to upload a receipt will result in an automatic freeze of your corporate card.",
                amount, merchantName, maskedCard
            );

            // -------------------------------------------------------------
            // SOLUTION 2: The Service-to-Service API Call
            // -------------------------------------------------------------
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            String identityServiceUrl = "http://localhost:8081/api/internal/cards/" + cardNumber + "/email";
            String targetEmail = null;
            
            try {
                // We ask the Identity Service: "Who owns this card?"
                targetEmail = restTemplate.getForObject(identityServiceUrl, String.class);
            } catch (Exception httpException) {
                log.warn("Identity DB lookup failed for card {}. Using fallback testing email.", cardNumber);
                targetEmail = "sidharth6034@gmail.com";
            }

            if (targetEmail != null) {
                sendEmail(targetEmail, "Action Required: Upload Receipt", emailBody);
            }

        } catch (Exception e) {
            log.error("Failed to process receipt reminder event: {}", e.getMessage());
        }
    }

    // ---------------------------------------------------------
    // 4. Listen for Internal Ops Proactive Alerts
    // ---------------------------------------------------------
    @KafkaListener(topics = "internal-alert-topic", groupId = "notification-group")
    public void handleInternalAlert(String message) {
        try {
            JsonNode payload = objectMapper.readTree(message);
            String role = payload.get("role").asText();
            String targetEmail = payload.get("targetEmail").asText();
            String alertText = payload.get("alert").asText();

            String emailBody = String.format(
                "🚨 PLATFORM ALERT [%s]\n\n" +
                "The system has generated an automated alert requiring your attention:\n\n" +
                "%s\n\n" +
                "Please log in to the Internal Dashboard to take action immediately.",
                role, alertText
            );

            // Using your testing email so you actually get the alert on your phone!
            sendEmail("sidharth6034@gmail.com", "URGENT: Internal Platform Alert", emailBody);
            
        } catch (Exception e) {
            log.error("Failed to process internal alert event: {}", e.getMessage());
        }
    }

    // ---------------------------------------------------------
    // 5. Listen for Receipt Mismatch Event
    // ---------------------------------------------------------
    @KafkaListener(topics = "receipt-mismatch-topic", groupId = "notification-group")
    public void handleReceiptMismatch(String message) {
        try {
            JsonNode payload = objectMapper.readTree(message);
            String cardNumber = payload.get("cardNumber").asText();
            String receiptUrl = payload.has("receiptUrl") ? payload.get("receiptUrl").asText() : "N/A";

            String maskedCard = "****-****-****-" + cardNumber.substring(cardNumber.length() - 4);
            
            String emailBody = String.format(
                "ACTION REQUIRED: Receipt Validation Failed\n\n" +
                "The receipt you uploaded for card %s could not be verified by our AI system.\n" +
                "The AI scanned the image and found a MISMATCH with the expected transaction details.\n\n" +
                "Your card will remain frozen pending manual review by the finance team.\n" +
                "Uploaded Receipt: %s\n\n" +
                "If this was a mistake, please reach out to support.",
                maskedCard, receiptUrl
            );

            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            String identityServiceUrl = "http://localhost:8081/api/internal/cards/" + cardNumber + "/email";
            String targetEmail = null;
            
            try {
                targetEmail = restTemplate.getForObject(identityServiceUrl, String.class);
            } catch (Exception httpException) {
                targetEmail = "sidharth6034@gmail.com";
            }

            if (targetEmail != null) {
                sendEmail(targetEmail, "Action Required: Receipt Validation Failed", emailBody);
            }

        } catch (Exception e) {
            log.error("Failed to process receipt mismatch event: {}", e.getMessage());
        }
    }

    // ---------------------------------------------------------
    // 6. Listen for IDOR Attack (Unauthorized Access)
    // ---------------------------------------------------------
    @KafkaListener(topics = "idor-alert-topic", groupId = "notification-group")
    public void handleIdorAlert(String message) {
        try {
            JsonNode payload = objectMapper.readTree(message);
            String cardNumber = payload.get("cardNumber").asText();
            String maskedCard = "****-****-****-" + cardNumber.substring(cardNumber.length() - 4);
            
            String emailBody = String.format(
                "SECURITY ALERT: Unauthorized Access Attempt Detected\n\n" +
                "An unauthorized user just attempted to upload or modify a receipt belonging to your corporate card (%s).\n\n" +
                "Our security systems successfully blocked the attempt. No action is required from you, but we wanted to keep you informed.\n\n" +
                "If you notice any unusual activity, please contact IT Security immediately.",
                maskedCard
            );

            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            String identityServiceUrl = "http://localhost:8081/api/internal/cards/" + cardNumber + "/email";
            String targetEmail = null;
            
            try {
                targetEmail = restTemplate.getForObject(identityServiceUrl, String.class);
            } catch (Exception httpException) {
                targetEmail = "sidharth6034@gmail.com";
            }

            if (targetEmail != null) {
                sendEmail(targetEmail, "URGENT: Unauthorized Access Blocked", emailBody);
            }

        } catch (Exception e) {
            log.error("Failed to process IDOR alert event: {}", e.getMessage());
        }
    }

    // ---------------------------------------------------------
    // The actual SMTP dispatch logic
    // ---------------------------------------------------------
    private void sendEmail(String to, String subject, String text) {
        
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(to);
        mailMessage.setSubject(subject);
        mailMessage.setText(text);
        mailMessage.setFrom("sidharth6034@gmail.com"); // Must match your authenticated Gmail!
        mailSender.send(mailMessage);
        
        log.info("\n========================================");
        log.info("✉️  SENDING EMAIL TO: {}", to);
        log.info("✉️  SUBJECT: {}", subject);
        log.info("✉️  BODY:\n{}", text);
        log.info("========================================\n");
    }
}

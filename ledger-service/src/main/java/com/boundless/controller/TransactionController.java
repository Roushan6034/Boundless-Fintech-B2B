package com.boundless.controller;

import com.boundless.entity.Transaction;
import com.boundless.repository.TransactionRepository;
import com.boundless.service.AuditService;
import com.boundless.service.GeminiService;
import com.boundless.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.client.RestTemplate;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.UUID;
import java.util.Map;
import java.util.List;
import java.util.Collections;
import java.io.IOException;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionRepository transactionRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final S3Service s3Service;
    private final AuditService auditService;
    private final GeminiService geminiService;

    @PostMapping(value = "/{transactionId}/receipt", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadReceipt(
            @PathVariable UUID transactionId, 
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "X-Authorization", required = false) String authHeader) {
            
        Transaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body("Unauthorized: Missing JWT Token");
        }

        // Cross-Microservice Ownership Check
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", authHeader);
            HttpEntity<String> entity = new HttpEntity<>("parameters", headers);
            String verifyUrl = "http://localhost:8081/api/internal/verify-card-ownership?cardNumber=" + tx.getCardNumber();
            ResponseEntity<Boolean> response = restTemplate.exchange(verifyUrl, HttpMethod.GET, entity, Boolean.class);
            if (response.getBody() == null || !response.getBody()) {
                // IDOR DETECTED! Notify the actual employee!
                kafkaTemplate.send("idor-alert-topic", String.format("{\"cardNumber\":\"%s\"}", tx.getCardNumber()));
                return ResponseEntity.status(403).body("Forbidden: You do not own this card!");
            }
        } catch (Exception e) {
            // Network failure or 403 returned from Identity Service
            kafkaTemplate.send("idor-alert-topic", String.format("{\"cardNumber\":\"%s\"}", tx.getCardNumber()));
            return ResponseEntity.status(403).body("Forbidden: Ownership Check Failed");
        }

        try {
            // 1. Upload to Cloudinary
            String s3Url = s3Service.uploadFile(transactionId, file);
            tx.setReceiptUrl(s3Url);

            // 2. Feature 19: Smart Receipt OCR & Auto-Matching
            try {
                String ocrPrompt = String.format(
                    "Extract the merchant name and total amount from this receipt image. " +
                    "Return ONLY a JSON object like: {\"merchant\":\"...\", \"amount\":\"...\"}. " +
                    "If you can't extract, return {\"merchant\":\"UNKNOWN\", \"amount\":\"0\"}");
                
                String ocrResult = geminiService.analyzeImage(file.getBytes(), 
                        file.getContentType() != null ? file.getContentType() : "image/png", ocrPrompt);
                tx.setOcrResult(ocrResult);

                // Simple match check: does the OCR result mention the merchant or amount?
                String lowerOcr = ocrResult.toLowerCase();
                boolean merchantMatch = lowerOcr.contains(tx.getMerchantName().toLowerCase());
                boolean amountMatch = lowerOcr.contains(tx.getAmount().toPlainString());
                tx.setOcrMatchStatus(merchantMatch || amountMatch ? "MATCHED" : "MISMATCHED");
                
            } catch (Exception ocrEx) {
                tx.setOcrResult("OCR unavailable");
                tx.setOcrMatchStatus("PENDING");
            }
            
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Failed to upload: " + e.getMessage());
        }

        tx.setReceiptUploaded(true);
        transactionRepository.save(tx);

        // Feature 6: Audit Trail
        auditService.log("RECEIPT_UPLOADED", "Transaction", transactionId.toString(),
                tx.getCardNumber(), "Receipt uploaded. OCR: " + tx.getOcrMatchStatus());

        // Unfreeze card only if the receipt matches!
        if ("MATCHED".equals(tx.getOcrMatchStatus())) {
            kafkaTemplate.send("card-unfreeze-topic", tx.getCardNumber());
            return ResponseEntity.ok(Map.of(
                    "message", "Receipt uploaded and matched! Card unfrozen.",
                    "receiptUrl", tx.getReceiptUrl(),
                    "ocrResult", tx.getOcrResult() != null ? tx.getOcrResult() : "N/A",
                    "ocrMatchStatus", tx.getOcrMatchStatus()
            ));
        } else {
            // Drop an event to notify the employee of the mismatch via email
            String mismatchEvent = String.format("{\"cardNumber\":\"%s\", \"receiptUrl\":\"%s\"}", 
                    tx.getCardNumber(), tx.getReceiptUrl());
            kafkaTemplate.send("receipt-mismatch-topic", mismatchEvent);

            return ResponseEntity.status(400).body(Map.of(
                    "message", "Receipt uploaded but OCR MISMATCHED. Card remains frozen pending manual review.",
                    "receiptUrl", tx.getReceiptUrl(),
                    "ocrResult", tx.getOcrResult() != null ? tx.getOcrResult() : "N/A",
                    "ocrMatchStatus", tx.getOcrMatchStatus()
            ));
        }
    }

    @GetMapping("/company/{companyName}")
    public ResponseEntity<?> getCompanyTransactions(@PathVariable String companyName) {
        RestTemplate restTemplate = new RestTemplate();
        String identityUrl = "http://localhost:8081/api/internal/companies/" + companyName + "/cards";
        try {
            List<String> cardNumbers = restTemplate.getForObject(identityUrl, List.class);
            if (cardNumbers == null || cardNumbers.isEmpty()) return ResponseEntity.ok(Collections.emptyList());
            return ResponseEntity.ok(transactionRepository.findByCardNumberIn(cardNumbers));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/my-history")
    public ResponseEntity<?> getMyTransactionHistory(@RequestHeader(value = "X-Trusted-User-Email", required = false) String email) {
        if (email == null) {
            return ResponseEntity.status(401).body("Unauthorized: Missing User Context");
        }
        
        RestTemplate restTemplate = new RestTemplate();
        String identityUrl = "http://localhost:8081/api/internal/employees/" + email + "/cards";
        try {
            List<String> cardNumbers = restTemplate.getForObject(identityUrl, List.class);
            if (cardNumbers == null || cardNumbers.isEmpty()) return ResponseEntity.ok(Collections.emptyList());
            return ResponseEntity.ok(transactionRepository.findByCardNumberIn(cardNumbers));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error retrieving transaction history: " + e.getMessage());
        }
    }
}

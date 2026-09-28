package com.boundless.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transaction_ledger")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String cardNumber;

    @Column(nullable = false)
    private String merchantName;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String status; // SETTLED, DECLINED, REFUNDED

    @Column(nullable = false)
    private LocalDateTime timestamp;
    
    // ----------------------------------------------------
    // COMPLIANCE: The 24-hour receipt rule
    // ----------------------------------------------------
    private boolean receiptUploaded;
    private LocalDateTime receiptDeadline;
    
    // Stores the file path of the uploaded image
    private String receiptUrl;

    // Feature 4: Multi-Currency Support
    private String originalCurrency;  // USD, INR, EUR, GBP
    private BigDecimal exchangeRate;
    private String settledCurrency;   // Company's base currency

    // Feature 19: OCR Auto-Match result
    private String ocrResult;
    private String ocrMatchStatus; // MATCHED, MISMATCHED, PENDING
}

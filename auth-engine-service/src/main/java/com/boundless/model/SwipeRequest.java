package com.boundless.model;

import lombok.Data;
import java.math.BigDecimal;

// This represents the JSON payload sent to us from the Visa/Mastercard network
@Data
public class SwipeRequest {
    private String cardNumber;
    private String cvv;
    private BigDecimal amount;
    private String currency; // Added currency field (e.g. "USD", "INR")
    private String merchantName;
    private String mcc; // Merchant Category Code (e.g. 7995 for Gambling, 5812 for Restaurants)
    private String terminalId;
    private String expiryDate; // Format: MM/YY
    
    // 3DS 2.0 Risk-Based Authentication Fields
    private String ipAddress;
    private String deviceId;
    private Long timeOnCheckoutPageMs; // Time between page load and hitting 'Pay'
    private String billingZipCode; // For AVS Check
    private String localTime; // Format "HH:mm" (24-hour clock)
}

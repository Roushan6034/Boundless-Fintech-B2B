package com.boundless.controller;

import com.boundless.entity.VirtualCard;
import com.boundless.repository.VirtualCardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
public class VerifyOwnershipController {

    private final VirtualCardRepository virtualCardRepository;

    // The Ledger Service calls this endpoint and passes the Employee's JWT token
    @GetMapping("/verify-card-ownership")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<Boolean> verifyOwnership(@RequestParam String cardNumber, Authentication auth) {
        String loggedInEmail = auth.getName();
        
        VirtualCard card = virtualCardRepository.findByCardNumber(cardNumber)
            .orElseThrow(() -> new RuntimeException("Card not found"));
            
        boolean ownsCard = card.getWallet().getEmployee().getEmail().equals(loggedInEmail);
        return ResponseEntity.ok(ownsCard);
    }
}

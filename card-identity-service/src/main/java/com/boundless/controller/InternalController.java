package com.boundless.controller;

import com.boundless.entity.VirtualCard;
import com.boundless.repository.VirtualCardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
public class InternalController {

    private final VirtualCardRepository virtualCardRepository;

    // This endpoint is meant for internal microservices ONLY.
    // It returns the true email address associated with a given card number.
    @GetMapping("/cards/{cardNumber}/email")
    public ResponseEntity<String> getEmailForCard(@PathVariable String cardNumber) {
        return virtualCardRepository.findByCardNumber(cardNumber)
                .map(card -> ResponseEntity.ok(card.getWallet().getEmployee().getEmail()))
                .orElse(ResponseEntity.notFound().build());
    }

    // NEW ENDPOINT: Return ALL 16-digit card numbers that belong to a specific company
    @GetMapping("/companies/{companyName}/cards")
    public ResponseEntity<List<String>> getCardsForCompany(@PathVariable String companyName) {
        List<String> cardNumbers = virtualCardRepository.findByWalletEmployeeCompanyName(companyName)
                .stream()
                .map(VirtualCard::getCardNumber)
                .toList();
        return ResponseEntity.ok(cardNumbers);
    }

    // NEW ENDPOINT: Return ALL 16-digit card numbers that belong to a specific employee email
    @GetMapping("/employees/{email}/cards")
    public ResponseEntity<List<String>> getCardsForEmployee(@PathVariable String email) {
        List<String> cardNumbers = virtualCardRepository.findByWalletEmployeeEmail(email)
                .stream()
                .map(VirtualCard::getCardNumber)
                .toList();
        return ResponseEntity.ok(cardNumbers);
    }

    // NEW ENDPOINT: Return the Company Name that owns a specific card (For Webhooks)
    @GetMapping("/cards/{cardNumber}/company")
    public ResponseEntity<String> getCompanyForCard(@PathVariable String cardNumber) {
        return virtualCardRepository.findByCardNumber(cardNumber)
                .map(card -> ResponseEntity.ok(card.getWallet().getEmployee().getCompanyName()))
                .orElse(ResponseEntity.notFound().build());
    }
}

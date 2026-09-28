package com.boundless.controller;

import com.boundless.entity.Wallet;
import com.boundless.repository.VirtualCardRepository;
import com.boundless.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final WalletRepository walletRepository;
    private final VirtualCardRepository virtualCardRepository;

    @PreAuthorize("hasRole('EMPLOYEE')")
    @GetMapping("/{employeeId}/wallets")
    public ResponseEntity<?> getMyWallets(@PathVariable UUID employeeId, Authentication auth) {
        
        // 1. Get the authenticated email extracted from the JWT token
        String loggedInEmail = auth.getName();
        
        // 2. Fetch the wallets from the database
        List<Wallet> wallets = walletRepository.findByEmployeeId(employeeId);
        
        // 3. STRICT IDOR SECURITY CHECK 
        // We must ensure the person requesting these wallets actually owns them.
        if (!wallets.isEmpty() && !wallets.get(0).getEmployee().getEmail().equals(loggedInEmail)) {
            log.warn("SECURITY ALERT: User {} attempted an IDOR attack on employee {}", loggedInEmail, employeeId);
            throw new AccessDeniedException("You are not authorized to view another employee's wallets.");
        }
        
        return ResponseEntity.ok(wallets);
    }

    @PreAuthorize("hasRole('EMPLOYEE')")
    @GetMapping("/{employeeId}/cards")
    public ResponseEntity<?> getMyCards(@PathVariable UUID employeeId, Authentication auth) {
        
        String loggedInEmail = auth.getName();
        
        // Custom repository method we wrote earlier!
        var cards = virtualCardRepository.findByWalletEmployeeId(employeeId);
        
        // STRICT IDOR SECURITY CHECK
        if (!cards.isEmpty() && !cards.get(0).getWallet().getEmployee().getEmail().equals(loggedInEmail)) {
            throw new AccessDeniedException("You are not authorized to view another employee's cards.");
        }
        
        return ResponseEntity.ok(cards);
    }
}

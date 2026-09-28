package com.boundless.controller;

import com.boundless.entity.Company;
import com.boundless.entity.MasterWallet;
import com.boundless.entity.User;
import com.boundless.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.boundless.repository.CompanyRepository;
import com.boundless.repository.MasterWalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Duration;

@RestController
@RequestMapping("/api/internal-ops")
@RequiredArgsConstructor
public class InternalOpsController {

    private final MasterWalletRepository masterWalletRepository;
    private final CompanyRepository companyRepository;
    private final StringRedisTemplate redisTemplate;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.boundless.service.KafkaProducerService kafkaProducerService;

    @PreAuthorize("hasRole('PLATFORM_COMPLIANCE')")
    @PostMapping("/companies/{companyName}/approve")
    public ResponseEntity<?> approveCompany(@PathVariable String companyName) {
        Company company = companyRepository.findByCompanyName(companyName)
                .orElseThrow(() -> new RuntimeException("Company not found."));
                
        if (!company.getKybStatus().equals("PENDING_MANUAL_REVIEW")) {
            return ResponseEntity.badRequest().body("Company is not in a pending state.");
        }
                
        company.setKybStatus("APPROVED");
        companyRepository.save(company);
        
        MasterWallet newWallet = new MasterWallet();
        newWallet.setCompanyName(company.getCompanyName());
        newWallet.setTotalBalance(new BigDecimal("0.00"));
        masterWalletRepository.save(newWallet);
        
        // 1. Determine Admin Email: Use founderEmail provided during registration, or fallback
        String adminEmail = (company.getFounderEmail() != null && !company.getFounderEmail().isBlank()) 
                ? company.getFounderEmail() 
                : "admin@" + companyName.toLowerCase().replace(" ", "") + ".com";
        String initialPassword = "Admin123!";

        User adminUser = User.builder()
                .email(adminEmail)
                .fullName(companyName + " Founder")
                .password(passwordEncoder.encode(initialPassword))
                .role("ROLE_COMPANY_ADMIN")
                .companyName(companyName)
                .requiresPasswordChange(false)
                .build();
        userRepository.save(adminUser);

        // 2. Dispatch credentials via Kafka to Notification Service
        kafkaProducerService.sendCompanyApprovalEmail(adminEmail, companyName, initialPassword);
        
        return ResponseEntity.ok("COMPLIANCE ACTION: " + companyName + " has been manually APPROVED. Master Wallet generated. Credentials sent to: " + adminEmail);
    }

    @PreAuthorize("hasRole('PLATFORM_COMPLIANCE')")
    @PostMapping("/companies/{companyName}/freeze")
    public ResponseEntity<?> freezeCompany(@PathVariable String companyName) {
        Company company = companyRepository.findByCompanyName(companyName)
                .orElseThrow(() -> new RuntimeException("Company not found."));
                
        company.setKybStatus("FROZEN_FOR_AML_INVESTIGATION");
        companyRepository.save(company);
        
        return ResponseEntity.ok("COMPLIANCE ACTION: " + companyName + " has been locked. All transactions halted.");
    }

    // TREASURY ROLE: Wire money to the platform using Idempotency & Pessimistic Locking
    @Transactional
    @PreAuthorize("hasRole('PLATFORM_TREASURY')")
    @PostMapping("/master-wallet/fund")
    public ResponseEntity<?> fundMasterWallet(
            @RequestParam String companyName, 
            @RequestParam BigDecimal amount,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
            
        // 1. IDEMPOTENCY CHECK: Ensure we haven't already processed this exact wire transfer!
        String redisKey = "idempotency:wire:" + idempotencyKey;
        Boolean isNewTransaction = redisTemplate.opsForValue().setIfAbsent(redisKey, "PROCESSED", Duration.ofHours(24));
        
        if (Boolean.FALSE.equals(isNewTransaction)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("DUPLICATE DETECTED: This Idempotency Key was already processed. Ignoring request to prevent double-funding.");
        }

        // 2. PESSIMISTIC LOCK: Lock the database row for this MasterWallet so no concurrent threads can alter the balance during calculation!
        MasterWallet masterWallet = masterWalletRepository.findByCompanyNameForUpdate(companyName)
                .orElseThrow(() -> new RuntimeException("Master Wallet not found."));
        
        // 3. EXECUTE: Safely add the funds and save.
        masterWallet.setTotalBalance(masterWallet.getTotalBalance().add(amount));
        masterWalletRepository.save(masterWallet);
        
        return ResponseEntity.ok("TREASURY ACTION: Successfully wired $" + amount + " to " + companyName + " (Protected by Row Lock & Idempotency Key)");
    }
}

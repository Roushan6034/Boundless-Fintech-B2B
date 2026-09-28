package com.boundless.controller;

import com.boundless.entity.MasterWallet;
import com.boundless.entity.User;
import com.boundless.entity.VirtualCard;
import com.boundless.entity.Wallet;
import com.boundless.repository.MasterWalletRepository;
import com.boundless.repository.UserRepository;
import com.boundless.repository.VirtualCardRepository;
import com.boundless.repository.WalletRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import com.boundless.service.KafkaProducerService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/company-admin")
@RequiredArgsConstructor
public class CompanyAdminController {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final MasterWalletRepository masterWalletRepository;
    private final VirtualCardRepository virtualCardRepository;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;
    private final KafkaProducerService kafkaProducerService;

    // View all employees in this company
    @PreAuthorize("hasRole('COMPANY_ADMIN')")
    @GetMapping("/employees")
    public ResponseEntity<?> getMyCompanyEmployees(Authentication auth) {
        User adminUser = userRepository.findByEmail(auth.getName()).get();
        List<User> companyEmployees = userRepository.findByCompanyName(adminUser.getCompanyName());
        return ResponseEntity.ok(companyEmployees);
    }

    // View all wallets belonging to this company
    @PreAuthorize("hasRole('COMPANY_ADMIN')")
    @GetMapping("/wallets")
    public ResponseEntity<?> getAllCompanyWallets(Authentication auth) {
        User adminUser = userRepository.findByEmail(auth.getName()).get();
        List<Wallet> wallets = walletRepository.findByEmployeeCompanyName(adminUser.getCompanyName());
        return ResponseEntity.ok(wallets);
    }

    // View all wallets for a specific employee
    @PreAuthorize("hasRole('COMPANY_ADMIN')")
    @GetMapping("/employees/{employeeId}/wallets")
    public ResponseEntity<?> getEmployeeWallets(@PathVariable UUID employeeId, Authentication auth) {
        User adminUser = userRepository.findByEmail(auth.getName()).get();
        User employee = userRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        if (!employee.getCompanyName().equals(adminUser.getCompanyName())) {
            return ResponseEntity.status(403).body("SECURITY VIOLATION: Employee does not belong to your company!");
        }

        List<Wallet> wallets = walletRepository.findByEmployeeId(employeeId);
        return ResponseEntity.ok(wallets);
    }

    // View all virtual cards for a specific employee
    @PreAuthorize("hasRole('COMPANY_ADMIN')")
    @GetMapping("/employees/{employeeId}/cards")
    public ResponseEntity<?> getEmployeeCards(@PathVariable UUID employeeId, Authentication auth) {
        User adminUser = userRepository.findByEmail(auth.getName()).get();
        User employee = userRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        if (!employee.getCompanyName().equals(adminUser.getCompanyName())) {
            return ResponseEntity.status(403).body("SECURITY VIOLATION: Employee does not belong to your company!");
        }

        var cards = virtualCardRepository.findByWalletEmployeeId(employeeId);
        return ResponseEntity.ok(cards);
    }

    // View all virtual cards across the whole company
    @PreAuthorize("hasRole('COMPANY_ADMIN')")
    @GetMapping("/cards")
    public ResponseEntity<?> getAllCompanyCards(Authentication auth) {
        User adminUser = userRepository.findByEmail(auth.getName()).get();
        var cards = virtualCardRepository.findByWalletEmployeeCompanyName(adminUser.getCompanyName());
        return ResponseEntity.ok(cards);
    }
    
    // Freeze or Unfreeze a specific employee and all their virtual cards
    @PreAuthorize("hasRole('COMPANY_ADMIN')")
    @PutMapping("/employees/{employeeId}/status")
    public ResponseEntity<?> updateEmployeeStatus(
            @PathVariable UUID employeeId, 
            @RequestParam(defaultValue = "FROZEN") String status, 
            Authentication auth) {
        
        User adminUser = userRepository.findByEmail(auth.getName()).get();
        User targetEmployee = userRepository.findById(employeeId).get();
        
        if (!targetEmployee.getCompanyName().equals(adminUser.getCompanyName())) {
            return ResponseEntity.status(403).body("SECURITY VIOLATION: Employee does not belong to your company!");
        }

        String targetStatus = status.equalsIgnoreCase("ACTIVE") ? "ACTIVE" : "FROZEN";
        
        // Update all virtual cards belonging to this employee
        var cards = virtualCardRepository.findByWalletEmployeeId(employeeId);
        for (var card : cards) {
            card.setStatus(targetStatus);
            virtualCardRepository.save(card);

            if ("FROZEN".equals(targetStatus)) {
                redisTemplate.opsForValue().set("card:" + card.getCardNumber() + ":status", "FROZEN");
            } else {
                redisTemplate.delete("card:" + card.getCardNumber() + ":status");
            }
        }

        return ResponseEntity.ok(String.format("Successfully set status to %s for %s (%d cards updated)", 
                targetStatus, targetEmployee.getFullName(), cards.size()));
    }

    // Onboard a new employee to this specific company!
    @PreAuthorize("hasRole('COMPANY_ADMIN')")
    @PostMapping("/employees/onboard")
    public ResponseEntity<?> onboardEmployee(@RequestBody OnboardRequest request, Authentication auth) {
        User adminUser = userRepository.findByEmail(auth.getName()).get();

        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body("Employee already exists");
        }

        String tempPassword = UUID.randomUUID().toString().substring(0, 8);
        String empId = "EMP-" + (int)(Math.random() * 90000 + 10000);

        User employee = User.builder()
                .employeeId(empId)
                .email(request.getEmail())
                .fullName(request.getFullName())
                .password(passwordEncoder.encode(tempPassword))
                .role("ROLE_EMPLOYEE")
                .companyName(adminUser.getCompanyName()) // Automatically tied to the Admin's company!
                .requiresPasswordChange(true)
                .build();

        userRepository.save(employee);
        kafkaProducerService.sendWelcomeEmailEvent(employee.getEmail(), empId, tempPassword);
        
        return ResponseEntity.ok("Employee Onboarded! ID: " + empId + " to " + adminUser.getCompanyName());
    }

    // Allocate budget to an employee from the company's master wallet
    @PreAuthorize("hasRole('COMPANY_ADMIN')")
    @PostMapping("/wallets")
    public ResponseEntity<?> createWallet(@RequestBody WalletRequest request, Authentication auth) {
        User adminUser = userRepository.findByEmail(auth.getName()).get();

        User employee = userRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        if (!employee.getCompanyName().equals(adminUser.getCompanyName())) {
            return ResponseEntity.status(403).body("Cannot fund an employee outside your company!");
        }

        MasterWallet masterWallet = masterWalletRepository.findByCompanyName(adminUser.getCompanyName())
                .orElseThrow(() -> new RuntimeException("Master Wallet not found"));

        if (masterWallet.getTotalBalance().compareTo(request.getInitialBalance()) < 0) {
            
            // 🚨 INTERNAL OPS PROACTIVE ALERTING (Treasury)
            String alertMsg = String.format("Client '%s' attempted to issue a ₹%.2f wallet, but their Master Balance is only ₹%.2f. Please contact their founder for a wire transfer.", 
                    adminUser.getCompanyName(), request.getInitialBalance(), masterWallet.getTotalBalance());
            kafkaProducerService.sendInternalAlert("ROLE_PLATFORM_TREASURY", "sidharth6034@gmail.com", alertMsg);

            return ResponseEntity.badRequest().body("DECLINED: Insufficient funds in your Master Wallet.");
        }

        masterWallet.setTotalBalance(masterWallet.getTotalBalance().subtract(request.getInitialBalance()));
        masterWalletRepository.save(masterWallet);

        Wallet wallet = Wallet.builder()
                .employee(employee)
                .name(request.getWalletName())
                .balance(request.getInitialBalance())
                .status("ACTIVE")
                .build();

        Wallet savedWallet = walletRepository.save(wallet);

        redisTemplate.opsForValue().set("wallet:" + savedWallet.getId() + ":balance", savedWallet.getBalance().toString());

        return ResponseEntity.ok(savedWallet);
    }

    // Top-up an existing wallet from the company's master wallet
    @PreAuthorize("hasRole('COMPANY_ADMIN')")
    @PostMapping("/wallets/{walletId}/topup")
    public ResponseEntity<?> topUpWallet(@PathVariable UUID walletId, @RequestBody TopUpRequest request, Authentication auth) {
        User adminUser = userRepository.findByEmail(auth.getName()).get();

        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        if (!wallet.getEmployee().getCompanyName().equals(adminUser.getCompanyName())) {
            return ResponseEntity.status(403).body("Cannot fund a wallet outside your company!");
        }

        MasterWallet masterWallet = masterWalletRepository.findByCompanyName(adminUser.getCompanyName())
                .orElseThrow(() -> new RuntimeException("Master Wallet not found"));

        if (masterWallet.getTotalBalance().compareTo(request.getAmount()) < 0) {
            String alertMsg = String.format("Client '%s' attempted to top-up ₹%.2f, but their Master Balance is only ₹%.2f.", 
                    adminUser.getCompanyName(), request.getAmount(), masterWallet.getTotalBalance());
            kafkaProducerService.sendInternalAlert("ROLE_PLATFORM_TREASURY", "sidharth6034@gmail.com", alertMsg);

            return ResponseEntity.badRequest().body("DECLINED: Insufficient funds in your Master Wallet.");
        }

        // Deduct from Master Wallet
        masterWallet.setTotalBalance(masterWallet.getTotalBalance().subtract(request.getAmount()));
        masterWalletRepository.save(masterWallet);

        // Add to Employee Wallet
        wallet.setBalance(wallet.getBalance().add(request.getAmount()));
        Wallet savedWallet = walletRepository.save(wallet);

        // Update Redis cache so Auth Engine sees the new balance instantly
        redisTemplate.opsForValue().set("wallet:" + savedWallet.getId() + ":balance", savedWallet.getBalance().toString());

        return ResponseEntity.ok(String.format("Successfully added ₹%.2f to wallet '%s'. New Balance: ₹%.2f", 
                request.getAmount(), savedWallet.getName(), savedWallet.getBalance()));
    }

    @PreAuthorize("hasRole('COMPANY_ADMIN')")
    @PostMapping("/wallets/{walletId}/issue-card")
    public ResponseEntity<?> issueVirtualCard(@PathVariable UUID walletId, @RequestBody CardIssueRequest request, Authentication auth) {
        User adminUser = userRepository.findByEmail(auth.getName()).get();
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        if (!wallet.getEmployee().getCompanyName().equals(adminUser.getCompanyName())) {
            return ResponseEntity.status(403).body("Wallet does not belong to your company!");
        }

        String cardNumber = "4" + (long)(Math.random() * 900000000000000L + 100000000000000L);
        String cvv = String.format("%03d", (int)(Math.random() * 999));
        String type = (request.getCardType() != null) ? request.getCardType().toUpperCase() : "STANDARD";

        VirtualCard card = VirtualCard.builder()
                .wallet(wallet)
                .cardNumber(cardNumber)
                .cvv(cvv)
                .expiryDate(LocalDate.now().plusYears(3))
                .status("ACTIVE")
                .cardType(type)
                .build();

        VirtualCard savedCard = virtualCardRepository.save(card);
        
        // Push the mapping AND the card type to Redis for the Auth Engine
        redisTemplate.opsForValue().set("card:" + savedCard.getCardNumber() + ":wallet", wallet.getId().toString());
        redisTemplate.opsForValue().set("card:" + savedCard.getCardNumber() + ":type", type);
        redisTemplate.opsForValue().set("card:" + savedCard.getCardNumber() + ":expiry", savedCard.getExpiryDate().format(java.time.format.DateTimeFormatter.ofPattern("MM/yy")));
        
        // AVS MOCK: We assume the company headquarters is in New York (10001) for this employee
        redisTemplate.opsForValue().set("card:" + savedCard.getCardNumber() + ":zip", "10001");

        // Notify the employee with full card details, policy guidelines, and usage limitations!
        User employee = wallet.getEmployee();
        kafkaProducerService.sendCardIssuedNotification(
                employee.getEmail(),
                employee.getFullName(),
                savedCard.getCardNumber(),
                savedCard.getCvv(),
                savedCard.getExpiryDate().toString(),
                type,
                wallet.getName(),
                wallet.getBalance().toString()
        );

        return ResponseEntity.ok("Card Issued! PAN: " + savedCard.getCardNumber() + " | Type: " + type + " | Notification sent to " + employee.getEmail());
    }
}

@Data
class CardIssueRequest {
    private String cardType; // "STANDARD" or "BURNER"
}

@Data
class OnboardRequest {
    private String email;
    private String fullName;
}

@Data
class WalletRequest {
    private UUID employeeId;
    private String walletName;
    private BigDecimal initialBalance;
}

@Data
class TopUpRequest {
    private BigDecimal amount;
}

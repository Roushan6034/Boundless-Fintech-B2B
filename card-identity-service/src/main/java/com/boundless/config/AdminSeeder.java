package com.boundless.config;

import com.boundless.entity.MasterWallet;
import com.boundless.entity.User;
import com.boundless.repository.MasterWalletRepository;
import com.boundless.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final MasterWalletRepository masterWalletRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.email:founder@boundless.com}")
    private String adminEmail;

    @Value("${admin.password:SuperSecretStr0ngP@ssword!}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        log.info("Checking for existing Admin account in PostgreSQL...");
        
        if (!userRepository.existsByEmail("support@boundless.com")) {
            log.info("Seeding Platform Operations Users...");
            
            User supportUser = User.builder()
                    .email("support@boundless.com")
                    .fullName("Customer Success Rep")
                    .password(passwordEncoder.encode("Password123!"))
                    .role("ROLE_PLATFORM_SUPPORT")
                    .companyName("Boundless Corp")
                    .requiresPasswordChange(false)
                    .build();
            userRepository.save(supportUser);

            User complianceUser = User.builder()
                    .email("compliance@boundless.com")
                    .fullName("Risk Officer")
                    .password(passwordEncoder.encode("Password123!"))
                    .role("ROLE_PLATFORM_COMPLIANCE")
                    .companyName("Boundless Corp")
                    .requiresPasswordChange(false)
                    .build();
            userRepository.save(complianceUser);

            User treasuryUser = User.builder()
                    .email("treasury@boundless.com")
                    .fullName("Treasury Manager")
                    .password(passwordEncoder.encode("Password123!"))
                    .role("ROLE_PLATFORM_TREASURY")
                    .companyName("Boundless Corp")
                    .requiresPasswordChange(false)
                    .build();
            userRepository.save(treasuryUser);
            
            log.info("Platform Ops accounts seeded successfully.");
        }

        if (masterWalletRepository.findByCompanyName("Boundless Corp").isEmpty()) {
            MasterWallet masterWallet = MasterWallet.builder()
                    .companyName("Boundless Corp")
                    .totalBalance(BigDecimal.ZERO)
                    .build();
            masterWalletRepository.save(masterWallet);
            log.info("Master Company Wallet seeded with $0.00 balance.");
        }
    }
}

package com.boundless.controller;

import com.boundless.entity.Company;
import com.boundless.repository.CompanyRepository;
import com.boundless.service.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicOnboardingController {

    private final CompanyRepository companyRepository;
    private final KafkaProducerService kafkaProducerService;

    public static class CompanyRegistrationRequest {
        private String companyName;
        private String ein;
        private String corporateAddress;
        private String founderEmail;
        
        public String getCompanyName() { return companyName; }
        public void setCompanyName(String companyName) { this.companyName = companyName; }
        public String getEin() { return ein; }
        public void setEin(String ein) { this.ein = ein; }
        public String getCorporateAddress() { return corporateAddress; }
        public void setCorporateAddress(String corporateAddress) { this.corporateAddress = corporateAddress; }
        public String getFounderEmail() { return founderEmail; }
        public void setFounderEmail(String founderEmail) { this.founderEmail = founderEmail; }
    }

    @PostMapping("/register-company")
    public ResponseEntity<?> registerNewCompany(@RequestBody CompanyRegistrationRequest request) {
        
        if (companyRepository.findByCompanyName(request.getCompanyName()).isPresent()) {
            return ResponseEntity.badRequest().body("Company name already taken.");
        }

        Company newCompany = new Company();
        newCompany.setCompanyName(request.getCompanyName());
        newCompany.setEin(request.getEin());
        newCompany.setCorporateAddress(request.getCorporateAddress());
        newCompany.setFounderEmail(request.getFounderEmail());
        
        // 1. Force status to PENDING (Manual Human Review Required)
        newCompany.setKybStatus("PENDING_MANUAL_REVIEW");
        companyRepository.save(newCompany);

        // 2. Alert the Compliance Team via Kafka
        String alertMessage = String.format("URGENT: New Company Registration. Name: %s | EIN: %s | Email: %s | Address: %s. Please review and approve.",
                request.getCompanyName(), request.getEin(), request.getFounderEmail(), request.getCorporateAddress());
                
        kafkaProducerService.sendInternalAlert("ROLE_PLATFORM_COMPLIANCE", "sidharth6034@gmail.com", alertMessage);

        return ResponseEntity.ok("Registration Received! Your application is PENDING. Our compliance officers will review your EIN. You will be notified via email upon approval.");
    }
}

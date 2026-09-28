package com.boundless.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "companies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String companyName;

    // Employer Identification Number (Tax ID)
    @Column(nullable = false, unique = true)
    private String ein;

    @Column(nullable = false)
    private String corporateAddress;

    @Column(nullable = true)
    private String founderEmail;

    // PENDING, APPROVED, REJECTED
    @Column(nullable = false)
    private String kybStatus;
}

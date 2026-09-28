package com.boundless.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Human-readable Corporate Employee ID (e.g., "EMP-54321")
    @Column(unique = true)
    private String employeeId;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String role; // ROLE_ADMIN or ROLE_EMPLOYEE

    @Column(nullable = false)
    private boolean requiresPasswordChange;
    
    // B2B SaaS: Which client company does this employee belong to?
    @Column(name = "company_name")
    private String companyName;
}

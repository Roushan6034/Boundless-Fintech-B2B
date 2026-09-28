package com.boundless.repository;

import com.boundless.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    
    // Used by our startup script to check if the Super Admin is already seeded
    boolean existsByRole(String role);
    
    // Fetch all employees for a specific B2B Client
    java.util.List<User> findByCompanyName(String companyName);
}

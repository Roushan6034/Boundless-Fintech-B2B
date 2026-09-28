package com.boundless.repository;

import com.boundless.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, UUID> {
    List<Wallet> findByEmployeeId(UUID employeeId);
    List<Wallet> findByEmployeeCompanyName(String companyName);
    
    // Used to find the central funding source
    Optional<Wallet> findByName(String name);
}

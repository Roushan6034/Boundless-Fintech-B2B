package com.boundless.repository;

import com.boundless.entity.VirtualCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VirtualCardRepository extends JpaRepository<VirtualCard, UUID> {
    
    // Used by the Auth Engine to look up a card when a swipe happens
    Optional<VirtualCard> findByCardNumber(String cardNumber);
    
    // Get all cards attached to a specific budget/wallet
    List<VirtualCard> findByWalletId(UUID walletId);
    
    // Get all cards owned by a specific employee across all their wallets
    List<VirtualCard> findByWalletEmployeeId(UUID employeeId);
    
    // Find by employee email
    List<VirtualCard> findByWalletEmployeeEmail(String email);
    
    // B2B: Get ALL cards for a specific company
    List<VirtualCard> findByWalletEmployeeCompanyName(String companyName);
}

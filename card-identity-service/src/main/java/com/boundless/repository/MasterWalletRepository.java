package com.boundless.repository;

import com.boundless.entity.MasterWallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MasterWalletRepository extends JpaRepository<MasterWallet, UUID> {
    Optional<MasterWallet> findByCompanyName(String companyName);

    // Pessimistic Write Lock: SELECT ... FOR UPDATE
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM MasterWallet m WHERE m.companyName = :companyName")
    Optional<MasterWallet> findByCompanyNameForUpdate(@Param("companyName") String companyName);
}

package com.boundless.repository;

import com.boundless.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    
    List<Transaction> findByReceiptUploadedFalseAndReceiptDeadlineBefore(LocalDateTime now);
    List<Transaction> findByCardNumberIn(List<String> cardNumbers);
    List<Transaction> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
    List<Transaction> findByAmountGreaterThan(BigDecimal amount);
}

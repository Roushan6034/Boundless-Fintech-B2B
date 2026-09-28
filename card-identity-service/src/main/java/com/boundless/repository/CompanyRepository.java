package com.boundless.repository;

import com.boundless.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompanyRepository extends JpaRepository<Company, UUID> {
    boolean existsByCompanyName(String companyName);
    boolean existsByEin(String ein);
    Optional<Company> findByCompanyName(String companyName);
}

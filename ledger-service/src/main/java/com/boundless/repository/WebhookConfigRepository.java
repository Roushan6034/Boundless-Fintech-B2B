package com.boundless.repository;

import com.boundless.entity.WebhookConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface WebhookConfigRepository extends JpaRepository<WebhookConfig, UUID> {
    List<WebhookConfig> findByCompanyNameAndActiveTrue(String companyName);
}

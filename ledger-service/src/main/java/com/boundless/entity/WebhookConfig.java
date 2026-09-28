package com.boundless.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity @Table(name = "webhook_configs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WebhookConfig {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false)
    private String companyName;
    @Column(nullable = false)
    private String webhookUrl;
    @Column(nullable = false)
    private String secret;
    private String events; // comma-separated: TRANSACTION_APPROVED,CARD_FROZEN
    private boolean active;
}

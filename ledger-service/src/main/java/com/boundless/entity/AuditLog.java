package com.boundless.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity @Table(name = "audit_log")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String action;
    private String entityType;
    private String entityId;
    private String performedBy;
    @Column(length = 1000)
    private String details;
    private LocalDateTime timestamp;
}

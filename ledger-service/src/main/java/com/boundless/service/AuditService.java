package com.boundless.service;

import com.boundless.entity.AuditLog;
import com.boundless.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditService {
    private final AuditLogRepository repo;

    public void log(String action, String entityType, String entityId, String performedBy, String details) {
        repo.save(AuditLog.builder()
                .action(action).entityType(entityType).entityId(entityId)
                .performedBy(performedBy).details(details).timestamp(LocalDateTime.now())
                .build());
    }
}

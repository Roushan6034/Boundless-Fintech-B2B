package com.boundless.controller;

import com.boundless.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {
    private final AuditLogRepository repo;

    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(repo.findAll());
    }

    @GetMapping("/{entityType}/{entityId}")
    public ResponseEntity<?> getByEntity(@PathVariable String entityType, @PathVariable String entityId) {
        return ResponseEntity.ok(repo.findByEntityTypeAndEntityIdOrderByTimestampDesc(entityType, entityId));
    }
}

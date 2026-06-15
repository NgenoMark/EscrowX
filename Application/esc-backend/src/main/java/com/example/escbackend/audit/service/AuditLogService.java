package com.example.escbackend.audit.service;


import com.example.escbackend.audit.dto.AuditLogResponse;
import com.example.escbackend.audit.repository.AuditLogRepository;
import com.example.escbackend.user.service.AdminAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final AdminAuthorizationService authz;

    public Page<AuditLogResponse>  getAllAudits(UUID adminUserId ,Pageable pageable) {

        authz.requireAdminOrSuperAdmin(adminUserId);

        return auditLogRepository.findAll(pageable)
                .map(AuditLogResponse::fromEntity);
    }

    public AuditLogResponse getAuditById(UUID adminUserId, UUID id){

        authz.requireAdminOrSuperAdmin(adminUserId);

        return auditLogRepository.findById(id)
                .map(AuditLogResponse::fromEntity)
                .orElseThrow(() -> new IllegalArgumentException("Audit log not found for this id" + id));
    }
}

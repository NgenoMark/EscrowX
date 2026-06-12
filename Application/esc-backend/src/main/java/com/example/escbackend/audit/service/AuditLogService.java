package com.example.escbackend.audit.service;


import com.example.escbackend.audit.dto.AuditLogResponse;
import com.example.escbackend.audit.repository.AuditLogRepository;
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

    public Page<AuditLogResponse>  getAllAudits(Pageable pageable){
        return auditLogRepository.findAll(pageable)
                .map(AuditLogResponse::fromEntity);
    }

    public AuditLogResponse getAuditById(UUID id){
        return auditLogRepository.findById(id)
                .map(AuditLogResponse::fromEntity)
                .orElseThrow(() -> new IllegalArgumentException("Audit log not found for this id" + id));
    }
}

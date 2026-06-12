package com.example.escbackend.audit.controller;


import com.example.escbackend.audit.dto.AuditLogResponse;
import com.example.escbackend.audit.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("api/v1/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {


    private final AuditLogService auditLogService;

    @GetMapping("/all-logs")
    public ResponseEntity<Page<AuditLogResponse>> getAllLogs(
            @PageableDefault(size = 20 ,  sort = "createdAt" , direction = Sort.Direction.DESC) Pageable pageable){

        Page<AuditLogResponse> logsPage = auditLogService.getAllAudits(pageable);
        return ResponseEntity.ok(logsPage);

    }

    @GetMapping("/log/{id}")
    public ResponseEntity<AuditLogResponse> getAuditLogById(
            @PathVariable UUID id){
        AuditLogResponse auditLog = auditLogService.getAuditById(id);
        return ResponseEntity.ok(auditLog);
    }
}

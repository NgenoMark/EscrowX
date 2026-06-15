package com.example.escbackend.audit.controller;


import com.example.escbackend.audit.dto.AuditLogResponse;
import com.example.escbackend.audit.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("api/v1/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {


    private final AuditLogService auditLogService;

    @GetMapping("/all-logs")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Page<AuditLogResponse>> getAllLogs(
            @RequestHeader("X-Actor-User-Id") UUID adminUserId,
            @PageableDefault(size = 20 ,  sort = "createdAt" , direction = Sort.Direction.DESC) Pageable pageable
           ){

        Page<AuditLogResponse> logsPage = auditLogService.getAllAudits(adminUserId, pageable );
        return ResponseEntity.ok(logsPage);

    }

    @GetMapping("/log/{id}")
    public ResponseEntity<AuditLogResponse> getAuditLogById(
            @PathVariable UUID id,
            @RequestHeader("X-Actor-User-Id") UUID adminUserId){
        AuditLogResponse auditLog = auditLogService.getAuditById(adminUserId,id);
        return ResponseEntity.ok(auditLog);
    }
}

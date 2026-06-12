package com.example.escbackend.audit.dto;

import com.example.escbackend.audit.entity.AuditLogEntity;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;


@Data
@Builder
public class AuditLogResponse {

    private UUID id;
    private UUID actorUserId;
    private String action;
    private String entityType;
    private UUID entityId;
    private Map<String, Object> metadata;
    private OffsetDateTime createdAt;

    public static AuditLogResponse fromEntity(AuditLogEntity entity){
        if(entity == null) return null;

        return AuditLogResponse.builder()
                .id(entity.getId())
                .actorUserId(entity.getActorUserId())
                .action(entity.getAction())
                .entityType(entity.getEntityType())
                .entityId(entity.getEntityId())
                .metadata(entity.getMetadata())
                .createdAt(entity.getCreatedAt())
                .build();
    }

}

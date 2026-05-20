package com.example.escbackend.user.service;

import com.example.escbackend.common.constants.UserRole;
import com.example.escbackend.common.constants.UserStatus;
import com.example.escbackend.common.exception.ApiException;
import com.example.escbackend.infrastructure.audit.AuditLogEntity;
import com.example.escbackend.infrastructure.audit.AuditLogRepository;
import com.example.escbackend.user.dto.UserDetailsResponse;
import com.example.escbackend.user.dto.UserRoleStatusUpdateResponse;
import com.example.escbackend.user.dto.UserRoleUpdateRequest;
import com.example.escbackend.user.dto.UserStatusUpdateRequest;
import com.example.escbackend.user.entity.UserEntity;
import com.example.escbackend.user.repository.ProfileRepository;
import com.example.escbackend.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Locale;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final UserMapperService mapper;
    private final AdminAuthorizationService authz;
    private final AuditLogRepository auditRepo;

    public UserService(
        UserRepository userRepository,
        ProfileRepository profileRepository,
        UserMapperService mapper,
        AdminAuthorizationService authz,
        AuditLogRepository auditRepo
    ) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.mapper = mapper;
        this.authz = authz;
        this.auditRepo = auditRepo;
    }

    public UserDetailsResponse getById(UUID id) {
        UserEntity user = userRepository.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        return mapper.toDetails(user, profileRepository.findById(user.getId()).orElse(null));
    }

    public UserDetailsResponse getByPhone(String phone) {
        UserEntity user = userRepository.findByPhone(phone)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        return mapper.toDetails(user, profileRepository.findById(user.getId()).orElse(null));
    }

    public UserDetailsResponse getByEmail(String email){
        UserEntity user = userRepository.findByEmail(email).
                orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        return mapper.toDetails(user, profileRepository.findById(user.getId()).orElse(null));
    }

    public Page<UserDetailsResponse> list(String phone, String role, String status, int page, int size) {
        String phoneFilter = phone == null ? "" : phone;
        Pageable pageable = PageRequest.of(page, size);
        Page<UserEntity> users;

        if (isNotBlank(role) && isNotBlank(status)) {
            users = userRepository.findByPhoneContainingAndRoleAndStatus(
                phoneFilter,
                parseRole(role),
                parseStatus(status),
                pageable
            );
        } else if (isNotBlank(role)) {
            users = userRepository.findByRole(parseRole(role), pageable);
        } else if (isNotBlank(status)) {
            users = userRepository.findByStatus(parseStatus(status), pageable);
        } else {
            users = userRepository.findByPhoneContaining(phoneFilter, pageable);
        }

        return users.map(user -> mapper.toDetails(user, profileRepository.findById(user.getId()).orElse(null)));
    }

    @Transactional
    public UserRoleStatusUpdateResponse updateRole(UUID targetUserId, UUID actorUserId, UserRoleUpdateRequest request) {
        authz.requireAdminOrSuperAdmin(actorUserId);

        UserEntity target = userRepository.findById(targetUserId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Target user not found"));

        String oldRole = target.getRole().name();
        target.setRole(request.getRole());
        userRepository.save(target);

        saveAudit(actorUserId, "UPDATE_ROLE", targetUserId, request.getReason());

        return UserRoleStatusUpdateResponse.builder()
            .userId(targetUserId)
            .oldValue(oldRole)
            .newValue(target.getRole().name())
            .updatedBy(actorUserId)
            .updatedAt(OffsetDateTime.now())
            .build();
    }

    @Transactional
    public UserRoleStatusUpdateResponse updateStatus(UUID targetUserId, UUID actorUserId, UserStatusUpdateRequest request) {
        authz.requireAdminOrSuperAdmin(actorUserId);

        UserEntity target = userRepository.findById(targetUserId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Target user not found"));

        String oldStatus = target.getStatus().name();
        target.setStatus(request.getStatus());
        userRepository.save(target);

        saveAudit(actorUserId, "UPDATE_STATUS", targetUserId, request.getReason());

        return UserRoleStatusUpdateResponse.builder()
            .userId(targetUserId)
            .oldValue(oldStatus)
            .newValue(target.getStatus().name())
            .updatedBy(actorUserId)
            .updatedAt(OffsetDateTime.now())
            .build();
    }

    private void saveAudit(UUID actorId, String action, UUID entityId, String reason) {
        AuditLogEntity log = new AuditLogEntity();
        log.setActorUserId(actorId);
        log.setAction(action);
        log.setEntityType("users");
        log.setEntityId(entityId);
        log.setMetadata(Map.of("reason", reason));
        auditRepo.save(log);
    }

    private UserRole parseRole(String role) {
        try {
            return UserRole.valueOf(role.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid role value");
        }
    }

    private UserStatus parseStatus(String status) {
        try {
            return UserStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid status value");
        }
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}

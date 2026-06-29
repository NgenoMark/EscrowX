package com.example.escbackend.user.service;

import com.example.escbackend.common.constants.BlackListStatus;
import com.example.escbackend.common.constants.UserRole;
import com.example.escbackend.common.constants.UserStatus;
import com.example.escbackend.common.exception.ApiException;
import com.example.escbackend.auth.service.OtpDeliveryService;
import com.example.escbackend.audit.entity.AuditLogEntity;
import com.example.escbackend.audit.repository.AuditLogRepository;
import com.example.escbackend.user.dto.*;
import com.example.escbackend.user.entity.ProfileEntity;
import com.example.escbackend.user.entity.UserBlacklistEntity;
import com.example.escbackend.user.entity.UserEntity;
import com.example.escbackend.user.repository.ProfileRepository;
import com.example.escbackend.user.repository.UserRepository;
import com.example.escbackend.user.repository.UserBlacklistRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.UUID;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final UserMapperService mapper;
    private final UserBlacklistRepository blacklistRepo; // <-- Added
    private final AdminAuthorizationService authz;
    private final AuditLogRepository auditRepo;
    private final PasswordEncoder passwordEncoder;
    private final OtpDeliveryService otpDeliveryService;

    public UserService(
        UserRepository userRepository,
        ProfileRepository profileRepository,
        UserMapperService mapper,
        UserBlacklistRepository blacklistRepo,
        AdminAuthorizationService authz,
        AuditLogRepository auditRepo,
        PasswordEncoder passwordEncoder,
        OtpDeliveryService otpDeliveryService
    ) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.mapper = mapper;
        this.blacklistRepo = blacklistRepo;
        this.authz = authz;
        this.auditRepo = auditRepo;
        this.passwordEncoder = passwordEncoder;
        this.otpDeliveryService = otpDeliveryService;
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

    public Page<UserDetailsResponse> listMarketplaceUsers(UUID actorUserId, String phone, String status, int page, int size) {
        authz.requireAdminOrSuperAdmin(actorUserId);
        return listByRoles(phone, status, page, size, List.of(UserRole.BUYER, UserRole.SELLER));
    }

    public Page<UserDetailsResponse> listBuyers(UUID actorUserId, String phone, String status, int page, int size) {
        authz.requireAdminOrSuperAdmin(actorUserId);
        return listByRoles(phone, status, page, size, List.of(UserRole.BUYER));
    }

    public Page<UserDetailsResponse> listSellers(UUID actorUserId, String phone, String status, int page, int size) {
        authz.requireAdminOrSuperAdmin(actorUserId);
        return listByRoles(phone, status, page, size, List.of(UserRole.SELLER));
    }

    public Page<UserDetailsResponse> listEmployees(UUID actorUserId, String phone, String status, int page, int size) {
        UserEntity actor = authz.requireAdminOrSuperAdmin(actorUserId);
        List<UserRole> visibleRoles = actor.getRole() == UserRole.SUPER_ADMIN
            ? List.of(UserRole.ADMIN, UserRole.SUPER_ADMIN)
            : List.of(UserRole.ADMIN);

        return listByRoles(phone, status, page, size, visibleRoles);
    }

    @Transactional
    public UserDetailsResponse createEmployee(UUID actorUserId, CreateEmployeeRequest request, UserRole targetRole) {
        authz.requireSuperAdmin(actorUserId);

        if (targetRole != UserRole.ADMIN && targetRole != UserRole.SUPER_ADMIN) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only ADMIN or SUPER_ADMIN can be created via this API");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already registered");
        }

        if (userRepository.existsByPhone(request.getPhone())) {
            throw new ApiException(HttpStatus.CONFLICT, "Phone number already registered");
        }

        UserEntity user = new UserEntity();
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(targetRole);
        user.setStatus(UserStatus.ACTIVE);
        user = userRepository.save(user);

        ProfileEntity profile = new ProfileEntity();
        profile.setUserId(user.getId());
        profile.setDisplayName(request.getDisplayName());
        profile.setBusinessName(request.getBusinessName());
        profile.setAddress(request.getAddress());
        profile.setAvatarUrl(request.getAvatarUrl());
        profileRepository.save(profile);

        saveAudit(actorUserId, "CREATE_" + targetRole.name(), user.getId(), "Employee account created by SUPER_ADMIN");

        return mapper.toDetails(user, profile);
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

    @Transactional
    public UserRoleStatusUpdateResponse approveSeller(UUID targetUserId, UUID actorUserId, SellerApprovalRequest request) {
        authz.requireAdminOrSuperAdmin(actorUserId);

        UserEntity target = userRepository.findById(targetUserId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Target user not found"));

        if (target.getRole() != UserRole.SELLER) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only SELLER accounts can be approved with this endpoint");
        }

        if (target.getStatus() != UserStatus.PENDING_ADMIN_APPROVAL) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "Seller account is not pending admin approval"
            );
        }

        String oldStatus = target.getStatus().name();
        target.setStatus(UserStatus.ACTIVE);
        userRepository.save(target);

        String reason = request == null || request.getReason() == null || request.getReason().isBlank()
            ? "Seller account approved"
            : request.getReason();
        saveAudit(actorUserId, "APPROVE_SELLER", targetUserId, reason);


        try {
            otpDeliveryService.sendAdminApprovalEmail(target.getEmail());
        } catch (Exception ex){
            log.warn("Admin-approval email failed for {}", target.getEmail() , ex);
        }

        return UserRoleStatusUpdateResponse.builder()
            .userId(targetUserId)
            .oldValue(oldStatus)
            .newValue(target.getStatus().name())
            .updatedBy(actorUserId)
            .updatedAt(OffsetDateTime.now())
            .build();
    }


    @Transactional
    public UpdateUserResponse updateUserDetails(UUID actorUserId , UpdateUserRequest request){
        UserEntity user = userRepository.findById(actorUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "The user is not found"));

        ProfileEntity profile = profileRepository.findById(actorUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "The user profile has not been located"));

        
        if (request.getEmail() != null){
            user.setEmail(request.getEmail());
        }

        if (request.getPhone () != null){
            user.setPhone(request.getPhone());
        }

        if (request.getPasswordHash() != null){
            user.setPasswordHash(passwordEncoder.encode(request.getPasswordHash()));
        }

        if (request.getDisplayName() != null){
            profile.setDisplayName(request.getDisplayName());
        }

        if (request.getBusinessName() != null){
            profile.setBusinessName(request.getBusinessName());
        }

        if (request.getAvatarUrl() != null){
            profile.setAvatarUrl(request.getAvatarUrl());
        }

        if (request.getAddress() != null){
            profile.setAddress(request.getAddress());
        }

        userRepository.save(user);
        profileRepository.save(profile);

        return mapper.toUpdateUserResponse(user , profile);
    }

    @Transactional
    public BlacklistUpdateResponse updateBlacklistStatus(UUID targetUserId, UUID actorUserId, BlacklistUpdateRequest request) {
        // 1. Authorize that the actor executing this change is an Admin/SuperAdmin
        authz.requireAdminOrSuperAdmin(actorUserId);

        // 2. Retrieve both target user and admin executor entities
        UserEntity targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Target user not found"));

        UserEntity adminActor = userRepository.findById(actorUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Admin user performing this action not found"));

        // 3. Update high-performance flags on the core UserEntity
        targetUser.setBlacklistStatus(request.getBlacklistStatus());

        // Operational rule: If banned or investigated, update core account status to reflecting state
        if (request.getBlacklistStatus() == BlackListStatus.PERMANENTLY_BANNED ||
                request.getBlacklistStatus() == BlackListStatus.TEMPORARILY_MUTED) {
            targetUser.setStatus(UserStatus.BLACKLISTED);
        } else if (request.getBlacklistStatus() == BlackListStatus.NOT_BLACKLISTED) {
            targetUser.setStatus(UserStatus.ACTIVE);
        }

        userRepository.save(targetUser);

        // 4. Manage detail log record inside the 'user_blacklists' details tracking table
        UserBlacklistEntity blacklistDetails = blacklistRepo.findByUserId(targetUserId)
                .orElseGet(() -> {
                    UserBlacklistEntity newRecord = new UserBlacklistEntity();
                    newRecord.setUser(targetUser);
                    return newRecord;
                });

        blacklistDetails.setBlacklistType(request.getBlacklistType());
        blacklistDetails.setReason(request.getReason());
        blacklistDetails.setEvidenceSummary(request.getEvidenceSummary());
        blacklistDetails.setBlacklistedBy(adminActor);
        blacklistDetails.setExpiresAt(request.getExpiresAt());

        blacklistRepo.save(blacklistDetails);

        // 5. Audit log tracking entry
        saveAudit(actorUserId, "UPDATE_BLACKLIST_STATUS", targetUserId, request.getReason());

        // 6. Map and return response payload
        return BlacklistUpdateResponse.builder()
                .userId(targetUserId)
                .userStatus(targetUser.getStatus())
                .blacklistStatus(targetUser.getBlacklistStatus())
                .blacklistRecordId(blacklistDetails.getId())
                .blacklistType(blacklistDetails.getBlacklistType())
                .reason(blacklistDetails.getReason())
                .blacklistedBy(actorUserId)
                .expiresAt(blacklistDetails.getExpiresAt())
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

    private Page<UserDetailsResponse> listByRoles(String phone, String status, int page, int size, List<UserRole> roles) {
        String phoneFilter = phone == null ? "" : phone;
        Pageable pageable = PageRequest.of(page, size);
        Page<UserEntity> users;

        if (isNotBlank(status) && isNotBlank(phone)) {
            users = userRepository.findByPhoneContainingAndRoleInAndStatus(phoneFilter, roles, parseStatus(status), pageable);
        } else if (isNotBlank(status)) {
            users = userRepository.findByRoleInAndStatus(roles, parseStatus(status), pageable);
        } else if (isNotBlank(phone)) {
            users = userRepository.findByPhoneContainingAndRoleIn(phoneFilter, roles, pageable);
        } else {
            users = userRepository.findByRoleIn(roles, pageable);
        }

        return users.map(user -> mapper.toDetails(user, profileRepository.findById(user.getId()).orElse(null)));
    }
}

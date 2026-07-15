package com.example.escbackend.user.service;

import com.example.escbackend.common.constants.BlackListStatus;
import com.example.escbackend.common.constants.UserRole;
import com.example.escbackend.common.constants.UserStatus;
import com.example.escbackend.common.exception.ApiException;
import com.example.escbackend.auth.service.OtpService;
import com.example.escbackend.auth.service.OtpDeliveryService;
import com.example.escbackend.audit.entity.AuditLogEntity;
import com.example.escbackend.audit.repository.AuditLogRepository;
import com.example.escbackend.notification.entity.InAppNotificationEntity;
import com.example.escbackend.notification.entity.NotificationDeliveryLogEntity;
import com.example.escbackend.notification.repository.InAppNotificationRepository;
import com.example.escbackend.notification.repository.NotificationDeliveryLogRepository;
import com.example.escbackend.notification.service.PushNotificationService;
import com.example.escbackend.notification.service.PushSendResult;
import com.example.escbackend.user.dto.*;
import com.example.escbackend.user.entity.ProfileEntity;
import com.example.escbackend.user.entity.RiderProfileEntity;
import com.example.escbackend.user.entity.UserBlacklistEntity;
import com.example.escbackend.user.entity.UserEntity;
import com.example.escbackend.user.repository.ProfileRepository;
import com.example.escbackend.user.repository.RiderProfileRepository;
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
    private final RiderProfileRepository riderProfileRepository;
    private final UserMapperService mapper;
    private final UserBlacklistRepository blacklistRepo; // <-- Added
    private final AdminAuthorizationService authz;
    private final AuditLogRepository auditRepo;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final OtpDeliveryService otpDeliveryService;
    private final PushNotificationService pushNotificationService;
    private final InAppNotificationRepository inAppNotificationRepository;
    private final NotificationDeliveryLogRepository notificationDeliveryLogRepository;

    public UserService(
        UserRepository userRepository,
        ProfileRepository profileRepository,
        RiderProfileRepository riderProfileRepository,
        UserMapperService mapper,
        UserBlacklistRepository blacklistRepo,
        AdminAuthorizationService authz,
        AuditLogRepository auditRepo,
        PasswordEncoder passwordEncoder,
        OtpService otpService,
        OtpDeliveryService otpDeliveryService,
        PushNotificationService pushNotificationService,
        InAppNotificationRepository inAppNotificationRepository,
        NotificationDeliveryLogRepository notificationDeliveryLogRepository
    ) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.riderProfileRepository = riderProfileRepository;
        this.mapper = mapper;
        this.blacklistRepo = blacklistRepo;
        this.authz = authz;
        this.auditRepo = auditRepo;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
        this.otpDeliveryService = otpDeliveryService;
        this.pushNotificationService = pushNotificationService;
        this.inAppNotificationRepository = inAppNotificationRepository;
        this.notificationDeliveryLogRepository = notificationDeliveryLogRepository;
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

    public RiderProfileResponse getRiderProfileByUserId(UUID userId) {
        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        if (user.getRole() != UserRole.RIDER) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "User is not a rider");
        }

        RiderProfileEntity riderProfile = riderProfileRepository.findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Rider profile not found"));

        return RiderProfileResponse.builder()
            .userId(riderProfile.getUserId())
            .displayName(riderProfile.getDisplayName())
            .phone(riderProfile.getPhone())
            .operationArea(riderProfile.getOperationArea())
            .licenseNumber(riderProfile.getLicenseNumber())
            .vehicleType(riderProfile.getVehicleType())
            .vehiclePlate(riderProfile.getVehiclePlate())
            .riderStatus(riderProfile.getRiderStatus())
            .createdAt(riderProfile.getCreatedAt())
            .updatedAt(riderProfile.getUpdatedAt())
            .build();
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

    public Page<UserDetailsResponse> listRiders (UUID actorUserId, String phone, String Status, int page, int size) {
        authz.requireAdminOrSuperAdmin(actorUserId);
        return listByRoles(phone, Status, page, size, List.of(UserRole.RIDER));
    }

    public Page<UserDetailsResponse> listEmployees(UUID actorUserId, String phone, String status, int page, int size) {
        UserEntity actor = authz.requireAdminOrSuperAdmin(actorUserId);
        List<UserRole> visibleRoles = actor.getRole() == UserRole.SUPER_ADMIN
            ? List.of(UserRole.ADMIN, UserRole.SUPER_ADMIN, UserRole.RIDER)
            : List.of(UserRole.ADMIN, UserRole.RIDER);

        return listByRoles(phone, status, page, size, visibleRoles);
    }

    @Transactional
    public UserDetailsResponse createEmployee(UUID actorUserId, CreateEmployeeRequest request, UserRole targetRole) {
        if (targetRole != UserRole.RIDER) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "This endpoint supports RIDER creation only");
        }
        return createRiderEmployee(actorUserId, request);
    }

    @Transactional
    public UserDetailsResponse createPrivilegedEmployee(UUID actorUserId, CreatePrivilegedEmployeeRequest request, UserRole targetRole) {
        authz.requireSuperAdmin(actorUserId);
        if (targetRole != UserRole.ADMIN && targetRole != UserRole.SUPER_ADMIN) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only ADMIN or SUPER_ADMIN can be created via this endpoint");
        }

        UserEntity user = createUserRecord(
            request.getEmail(),
            request.getPhone(),
            request.getPassword(),
            targetRole,
            UserStatus.ACTIVE
        );

        ProfileEntity profile = createProfileRecord(
            user.getId(),
            request.getDisplayName(),
            request.getBusinessName(),
            request.getAddress(),
            request.getAvatarUrl()
        );

        saveAudit(actorUserId, "CREATE_" + targetRole.name(), user.getId(), "Privileged employee account created by SUPER_ADMIN");
        return mapper.toDetails(user, profile);
    }

    @Transactional
    public UserDetailsResponse createRiderEmployee(UUID actorUserId, CreateEmployeeRequest request) {
        authz.requireAdminOrSuperAdmin(actorUserId);
        validateRiderEnrollment(request);

        UserEntity user = createUserRecord(
            request.getEmail(),
            request.getPhone(),
            request.getPassword(),
            UserRole.RIDER,
            UserStatus.ACTIVE
        );

        ProfileEntity profile = createProfileRecord(
            user.getId(),
            request.getDisplayName(),
            request.getBusinessName(),
            request.getAddress(),
            request.getAvatarUrl()
        );

        RiderProfileEntity riderProfile = new RiderProfileEntity();
        riderProfile.setUserId(user.getId());
        riderProfile.setDisplayName(request.getDisplayName());
        riderProfile.setPhone(request.getPhone());
        riderProfile.setOperationArea(request.getOperationArea().trim());
        riderProfile.setLicenseNumber(request.getLicenseNumber().trim());
        riderProfile.setVehicleType(request.getVehicleType().trim());
        riderProfile.setVehiclePlate(request.getVehiclePlate().trim());
        riderProfileRepository.save(riderProfile);

        saveAudit(actorUserId, "CREATE_RIDER", user.getId(), "Rider account created by admin");
        return mapper.toDetails(user, profile);
    }

    @Transactional
    public UserDetailsResponse createMarketplaceUserByAdmin(UUID actorUserId, CreateMarketplaceUserRequest request, UserRole targetRole) {
        authz.requireAdminOrSuperAdmin(actorUserId);
        if (targetRole != UserRole.BUYER && targetRole != UserRole.SELLER) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only BUYER or SELLER can be created via this endpoint");
        }

        UserEntity user = createUserRecord(
            request.getEmail(),
            request.getPhone(),
            request.getPassword(),
            targetRole,
            UserStatus.PENDING_VERIFICATION
        );

        ProfileEntity profile = createProfileRecord(
            user.getId(),
            request.getDisplayName(),
            request.getBusinessName(),
            request.getAddress(),
            request.getAvatarUrl()
        );

        saveAudit(actorUserId, "CREATE_" + targetRole.name() + "_BY_ADMIN", user.getId(), "Marketplace user created pending verification");
        return mapper.toDetails(user, profile);
    }

    @Transactional
    public UserRoleStatusUpdateResponse sendVerificationOtpForMarketplaceUser(UUID actorUserId, UUID targetUserId) {
        authz.requireAdminOrSuperAdmin(actorUserId);
        UserEntity target = userRepository.findById(targetUserId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Target user not found"));

        if (target.getRole() != UserRole.BUYER && target.getRole() != UserRole.SELLER) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only BUYER or SELLER can be verified via this endpoint");
        }

        if (target.getStatus() != UserStatus.PENDING_VERIFICATION) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "User is not in PENDING_VERIFICATION state");
        }

        String otp = otpService.generate(target.getEmail(), "ADMIN_USER_VERIFY");
        otpDeliveryService.sendRegistrationOtp(target.getEmail(), otp);
        saveAudit(actorUserId, "SEND_VERIFICATION_OTP", targetUserId, "Admin triggered verification OTP");

        return UserRoleStatusUpdateResponse.builder()
            .userId(targetUserId)
            .oldValue(target.getStatus().name())
            .newValue(target.getStatus().name())
            .updatedBy(actorUserId)
            .updatedAt(OffsetDateTime.now())
            .build();
    }

    @Transactional
    public UserRoleStatusUpdateResponse verifyMarketplaceUserByOtp(
        UUID actorUserId,
        UUID targetUserId,
        AdminVerificationConfirmRequest request
    ) {
        authz.requireAdminOrSuperAdmin(actorUserId);
        UserEntity target = userRepository.findById(targetUserId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Target user not found"));

        if (target.getRole() != UserRole.BUYER && target.getRole() != UserRole.SELLER) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only BUYER or SELLER can be verified via this endpoint");
        }

        if (target.getStatus() != UserStatus.PENDING_VERIFICATION) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "User is not in PENDING_VERIFICATION state");
        }

        otpService.verify(target.getEmail(), "ADMIN_USER_VERIFY", request.getOtp());

        String oldStatus = target.getStatus().name();
        target.setStatus(UserStatus.ACTIVE);
        userRepository.save(target);

        String reason = request.getReason() == null || request.getReason().isBlank()
            ? "Admin verified user by OTP"
            : request.getReason();
        saveAudit(actorUserId, "VERIFY_MARKETPLACE_USER", targetUserId, reason);

        try {
            otpDeliveryService.sendMarketplaceVerificationSuccessEmail(target.getEmail(), target.getRole().name());
        } catch (Exception ex) {
            log.warn("Marketplace verification confirmation email failed for {}", target.getEmail(), ex);
        }

        return UserRoleStatusUpdateResponse.builder()
            .userId(targetUserId)
            .oldValue(oldStatus)
            .newValue(target.getStatus().name())
            .updatedBy(actorUserId)
            .updatedAt(OffsetDateTime.now())
            .build();
    }

    private void validateRiderEnrollment(CreateEmployeeRequest request) {
        if (!isNotBlank(request.getDisplayName())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "displayName is required for rider enrollment");
        }
        if (!isNotBlank(request.getPhone())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "phone is required for rider enrollment");
        }
        if (!isNotBlank(request.getOperationArea())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "operationArea is required for rider enrollment");
        }
        if (!isNotBlank(request.getLicenseNumber())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "licenseNumber is required for rider enrollment");
        }
        if (!isNotBlank(request.getVehicleType())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "vehicleType is required for rider enrollment");
        }
        if (!isNotBlank(request.getVehiclePlate())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "vehiclePlate is required for rider enrollment");
        }
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

        String title = "Seller account approved";
        String body = "Your seller account is now active. You can log in and start transacting.";
        Map<String, String> payload = Map.of(
            "type", "SELLER_APPROVED",
            "userId", target.getId().toString()
        );

        InAppNotificationEntity inAppNotification = new InAppNotificationEntity();
        inAppNotification.setUserId(target.getId());
        inAppNotification.setTitle(title);
        inAppNotification.setBody(body);
        inAppNotification.setType("SELLER_APPROVED");
        inAppNotification.setStatus("UNREAD");
        inAppNotification.setReferenceId(target.getId());
        inAppNotification.setReferenceType("USER");
        inAppNotification.setPayloadJson(Map.of(
            "type", "SELLER_APPROVED",
            "userId", target.getId().toString()
        ));
        InAppNotificationEntity savedNotification = inAppNotificationRepository.save(inAppNotification);

        NotificationDeliveryLogEntity inAppLog = new NotificationDeliveryLogEntity();
        inAppLog.setNotificationId(savedNotification.getId());
        inAppLog.setUserId(target.getId());
        inAppLog.setChannel("IN_APP");
        inAppLog.setProvider("LOCAL");
        inAppLog.setStatus("SENT");
        inAppLog.setDeliveredAt(OffsetDateTime.now());
        notificationDeliveryLogRepository.save(inAppLog);

        try {
            PushSendResult pushResult = pushNotificationService.sendToUser(target.getId(), title, body, payload);

            NotificationDeliveryLogEntity pushLog = new NotificationDeliveryLogEntity();
            pushLog.setNotificationId(savedNotification.getId());
            pushLog.setUserId(target.getId());
            pushLog.setChannel("PUSH");
            pushLog.setProvider("FIREBASE");

            if (!pushResult.enabled() || pushResult.tokenCount() == 0) {
                pushLog.setStatus("DROPPED");
                pushLog.setErrorMessage(pushResult.message());
            } else if (pushResult.successCount() > 0) {
                pushLog.setStatus("SENT");
                pushLog.setDeliveredAt(OffsetDateTime.now());
                if (pushResult.failedCount() > 0) {
                    pushLog.setErrorMessage("Partial delivery: " + pushResult.message());
                }
            } else {
                pushLog.setStatus("FAILED");
                pushLog.setErrorMessage(pushResult.message());
            }

            notificationDeliveryLogRepository.save(pushLog);
        } catch (Exception ex) {
            log.warn("Seller approval push notification failed for {}", target.getId(), ex);

            NotificationDeliveryLogEntity pushLog = new NotificationDeliveryLogEntity();
            pushLog.setNotificationId(savedNotification.getId());
            pushLog.setUserId(target.getId());
            pushLog.setChannel("PUSH");
            pushLog.setProvider("FIREBASE");
            pushLog.setStatus("FAILED");
            pushLog.setErrorMessage(ex.getMessage());
            notificationDeliveryLogRepository.save(pushLog);
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

    private UserEntity createUserRecord(String email, String phone, String password, UserRole role, UserStatus status) {
        if (userRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already registered");
        }

        if (userRepository.existsByPhone(phone)) {
            throw new ApiException(HttpStatus.CONFLICT, "Phone number already registered");
        }

        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setPhone(phone);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);
        user.setStatus(status);
        return userRepository.save(user);
    }

    private ProfileEntity createProfileRecord(UUID userId, String displayName, String businessName, String address, String avatarUrl) {
        ProfileEntity profile = new ProfileEntity();
        profile.setUserId(userId);
        profile.setDisplayName(displayName);
        profile.setBusinessName(businessName);
        profile.setAddress(address);
        profile.setAvatarUrl(avatarUrl);
        return profileRepository.save(profile);
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

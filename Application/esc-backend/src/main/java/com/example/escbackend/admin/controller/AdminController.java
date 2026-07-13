package com.example.escbackend.admin.controller;

import com.example.escbackend.admin.dto.EscrowLedgerEntryAdminDto;
import com.example.escbackend.admin.dto.PaymentIntentAdminDto;
import com.example.escbackend.admin.dto.PayoutAdminDto;
import com.example.escbackend.admin.dto.PayoutReconciliationResultDto;
import com.example.escbackend.admin.service.AdminService;
import com.example.escbackend.audit.dto.AuditLogResponse;
import com.example.escbackend.audit.service.AuditLogService;
import com.example.escbackend.common.constants.UserRole;
import com.example.escbackend.user.dto.AdminVerificationConfirmRequest;
import com.example.escbackend.user.dto.CreateEmployeeRequest;
import com.example.escbackend.user.dto.CreateMarketplaceUserRequest;
import com.example.escbackend.user.dto.CreatePrivilegedEmployeeRequest;
import com.example.escbackend.user.dto.SellerApprovalRequest;
import com.example.escbackend.user.dto.UserDetailsResponse;
import com.example.escbackend.user.dto.UserRoleStatusUpdateResponse;
import com.example.escbackend.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminService adminService;
    private final AuditLogService auditLogService;
    private final UserService userService;

    public AdminController(AdminService adminService, AuditLogService auditLogService, UserService userService) {
        this.adminService = adminService;
        this.auditLogService = auditLogService;
        this.userService = userService;
    }

    @PostMapping("/users/create-admin")
    public ResponseEntity<UserDetailsResponse> createAdmin(
        @RequestHeader(value = "X-Actor-User-Id", required = false) UUID actorUserId,
        @Valid @RequestBody CreatePrivilegedEmployeeRequest request
    ) {
        return new ResponseEntity<>(userService.createPrivilegedEmployee(actorUserId, request, UserRole.ADMIN), HttpStatus.CREATED);
    }

    @PostMapping("/users/create-super-admin")
    public ResponseEntity<UserDetailsResponse> createSuperAdmin(
        @RequestHeader(value = "X-Actor-User-Id", required = false) UUID actorUserId,
        @Valid @RequestBody CreatePrivilegedEmployeeRequest request
    ) {
        return new ResponseEntity<>(userService.createPrivilegedEmployee(actorUserId, request, UserRole.SUPER_ADMIN), HttpStatus.CREATED);
    }

    @PostMapping("/users/create-rider")
    public ResponseEntity<UserDetailsResponse> createRider(
        @RequestHeader(value = "X-Actor-User-Id", required = false) UUID actorUserId,
        @Valid @RequestBody CreateEmployeeRequest request
    ) {
        return new ResponseEntity<>(userService.createRiderEmployee(actorUserId, request), HttpStatus.CREATED);
    }

    @PostMapping("/users/create-buyer")
    public ResponseEntity<UserDetailsResponse> createBuyer(
        @RequestHeader(value = "X-Actor-User-Id", required = false) UUID actorUserId,
        @Valid @RequestBody CreateMarketplaceUserRequest request
    ) {
        return new ResponseEntity<>(userService.createMarketplaceUserByAdmin(actorUserId, request, UserRole.BUYER), HttpStatus.CREATED);
    }

    @PostMapping("/users/create-seller")
    public ResponseEntity<UserDetailsResponse> createSeller(
        @RequestHeader(value = "X-Actor-User-Id", required = false) UUID actorUserId,
        @Valid @RequestBody CreateMarketplaceUserRequest request
    ) {
        return new ResponseEntity<>(userService.createMarketplaceUserByAdmin(actorUserId, request, UserRole.SELLER), HttpStatus.CREATED);
    }

    @PostMapping("/users/{id}/send-verification-otp")
    public ResponseEntity<UserRoleStatusUpdateResponse> sendVerificationOtp(
        @RequestHeader(value = "X-Actor-User-Id", required = false) UUID actorUserId,
        @PathVariable("id") UUID targetUserId
    ) {
        return ResponseEntity.ok(userService.sendVerificationOtpForMarketplaceUser(actorUserId, targetUserId));
    }

    @PostMapping("/users/{id}/verify-otp")
    public ResponseEntity<UserRoleStatusUpdateResponse> verifyOtp(
        @RequestHeader(value = "X-Actor-User-Id", required = false) UUID actorUserId,
        @PathVariable("id") UUID targetUserId,
        @Valid @RequestBody AdminVerificationConfirmRequest request
    ) {
        return ResponseEntity.ok(userService.verifyMarketplaceUserByOtp(actorUserId, targetUserId, request));
    }

    @PostMapping("/users/{id}/approve-seller")
    public UserRoleStatusUpdateResponse approveSeller(
        @PathVariable UUID id,
        @RequestHeader(value = "X-Actor-User-Id", required = false) UUID actorUserId,
        @RequestBody(required = false) SellerApprovalRequest request
    ) {
        return userService.approveSeller(id, actorUserId, request);
    }

    @GetMapping("/payment-intents")
    public List<PaymentIntentAdminDto> getAllPaymentIntents(
        @RequestHeader(value = "X-Actor-User-Id", required = false) UUID actorUserId
    ) {
        return adminService.getAllPaymentIntents(actorUserId);
    }

    @GetMapping("/payment-intents/{id}")
    public PaymentIntentAdminDto getPaymentIntentById(
        @RequestHeader(value = "X-Actor-User-Id", required = false) UUID actorUserId,
        @PathVariable UUID id
    ) {
        return adminService.getPaymentIntentById(actorUserId, id);
    }

    @GetMapping("/payouts")
    public List<PayoutAdminDto> getAllPayouts(
        @RequestHeader(value = "X-Actor-User-Id", required = false) UUID actorUserId
    ) {
        return adminService.getAllPayouts(actorUserId);
    }

    @GetMapping("/payouts/{id}")
    public PayoutAdminDto getPayoutById(
        @RequestHeader(value = "X-Actor-User-Id", required = false) UUID actorUserId,
        @PathVariable UUID id
    ) {
        return adminService.getPayoutById(actorUserId, id);
    }

    @GetMapping("/ledger-entries")
    public List<EscrowLedgerEntryAdminDto> getAllLedgerEntries(
        @RequestHeader(value = "X-Actor-User-Id", required = false) UUID actorUserId
    ) {
        return adminService.getAllLedgerEntries(actorUserId);
    }

    @GetMapping("/ledger-entries/{id}")
    public EscrowLedgerEntryAdminDto getLedgerEntryById(
        @RequestHeader(value = "X-Actor-User-Id", required = false) UUID actorUserId,
        @PathVariable UUID id
    ) {
        return adminService.getLedgerEntryById(actorUserId, id);
    }

    @PostMapping("/payouts/reconcile-stuck-processing")
    public PayoutReconciliationResultDto reconcileStuckProcessingPayouts(
        @RequestHeader(value = "X-Actor-User-Id", required = false) UUID actorUserId
    ) {
        return adminService.reconcileStuckProcessingPayouts(actorUserId);
    }

    @GetMapping("/audit-logs/all-logs")
    public Page<AuditLogResponse> getAllAuditLogsForAdmin(
        @RequestHeader(value = "X-Actor-User-Id", required = false) UUID actorUserId,
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return auditLogService.getAllAudits(actorUserId, pageable);
    }

    @GetMapping("/audit-logs/log/{id}")
    public AuditLogResponse getAuditLogByIdForAdmin(
        @PathVariable UUID id,
        @RequestHeader(value = "X-Actor-User-Id", required = false) UUID actorUserId
    ) {
        return auditLogService.getAuditById(actorUserId, id);
    }
}

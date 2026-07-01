package com.example.escbackend.admin.controller;

import com.example.escbackend.admin.dto.EscrowLedgerEntryAdminDto;
import com.example.escbackend.admin.dto.PaymentIntentAdminDto;
import com.example.escbackend.admin.dto.PayoutAdminDto;
import com.example.escbackend.admin.dto.PayoutReconciliationResultDto;
import com.example.escbackend.admin.service.AdminService;
import com.example.escbackend.audit.dto.AuditLogResponse;
import com.example.escbackend.audit.service.AuditLogService;
import com.example.escbackend.common.constants.UserRole;
import com.example.escbackend.user.dto.CreateEmployeeRequest;
import com.example.escbackend.user.dto.SellerApprovalRequest;
import com.example.escbackend.user.dto.UserDetailsResponse;
import com.example.escbackend.user.dto.UserRoleStatusUpdateResponse;
import com.example.escbackend.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
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

	@PostMapping("/users/employees/admin")
	public UserDetailsResponse createAdmin(
		@RequestHeader("X-Actor-User-Id") UUID actorUserId,
		@Valid @RequestBody CreateEmployeeRequest request
	) {
		return userService.createEmployee(actorUserId, request, UserRole.ADMIN);
	}

	@PostMapping("/users/employees/super-admin")
	public UserDetailsResponse createSuperAdmin(
		@RequestHeader("X-Actor-User-Id") UUID actorUserId,
		@Valid @RequestBody CreateEmployeeRequest request
	) {
		return userService.createEmployee(actorUserId, request, UserRole.SUPER_ADMIN);
	}

	@PostMapping("/users/employees/rider")
	public UserDetailsResponse createRider(
		@RequestHeader("X-Actor-User-Id") UUID actorUserId,
		@Valid @RequestBody CreateEmployeeRequest request
	) {
		return userService.createEmployee(actorUserId, request, UserRole.RIDER);
	}

	@PostMapping("/users/{id}/approve-seller")
	public UserRoleStatusUpdateResponse approveSeller(
		@PathVariable UUID id,
		@RequestHeader("X-Actor-User-Id") UUID actorUserId,
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

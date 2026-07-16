package com.example.escbackend.admin.service;


import com.example.escbackend.admin.dto.EscrowLedgerEntryAdminDto;
import com.example.escbackend.admin.dto.PaymentIntentAdminDto;
import com.example.escbackend.admin.dto.PayoutAdminDto;
import com.example.escbackend.admin.dto.PayoutReconciliationResultDto;
import com.example.escbackend.common.exception.ApiException;
import com.example.escbackend.payment.entity.EscrowLedgerEntryEntity;
import com.example.escbackend.payment.entity.PaymentIntentEntity;
import com.example.escbackend.payment.entity.PayoutEntity;
import com.example.escbackend.payment.dto.CallbackReplayResult;
import com.example.escbackend.payment.repository.EscrowLedgerEntryRepository;
import com.example.escbackend.payment.repository.PaymentIntentRepository;
import com.example.escbackend.payment.repository.PayoutRepository;
import com.example.escbackend.payment.service.PaymentService;
import com.example.escbackend.user.service.AdminAuthorizationService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AdminService {
	private final PaymentIntentRepository paymentIntentRepository;
	private final PayoutRepository payoutRepository;
 private final EscrowLedgerEntryRepository escrowLedgerEntryRepository;
	private final PaymentService paymentService;
	private final AdminAuthorizationService adminAuthorizationService;

	public AdminService(
		PaymentIntentRepository paymentIntentRepository,
		PayoutRepository payoutRepository,
	EscrowLedgerEntryRepository escrowLedgerEntryRepository,
		PaymentService paymentService,
		AdminAuthorizationService adminAuthorizationService
	) {
		this.paymentIntentRepository = paymentIntentRepository;
		this.payoutRepository = payoutRepository;
	this.escrowLedgerEntryRepository = escrowLedgerEntryRepository;
		this.paymentService = paymentService;
		this.adminAuthorizationService = adminAuthorizationService;
	}

	public List<PaymentIntentAdminDto> getAllPaymentIntents(UUID actorUserId) {
		adminAuthorizationService.requireAdminOrSuperAdmin(requireActorUserId(actorUserId));
		return paymentIntentRepository.findAll().stream()
			.map(this::toPaymentIntentDto)
			.toList();
	}

	public PaymentIntentAdminDto getPaymentIntentById(UUID actorUserId, UUID paymentIntentId) {
		adminAuthorizationService.requireAdminOrSuperAdmin(requireActorUserId(actorUserId));
		PaymentIntentEntity paymentIntent = paymentIntentRepository.findById(paymentIntentId)
			.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Payment intent not found"));
		return toPaymentIntentDto(paymentIntent);
	}

	public List<PayoutAdminDto> getAllPayouts(UUID actorUserId) {
		adminAuthorizationService.requireAdminOrSuperAdmin(requireActorUserId(actorUserId));
		return payoutRepository.findAll().stream()
			.map(this::toPayoutDto)
			.toList();
	}

	public PayoutAdminDto getPayoutById(UUID actorUserId, UUID payoutId) {
		adminAuthorizationService.requireAdminOrSuperAdmin(requireActorUserId(actorUserId));
		PayoutEntity payout = payoutRepository.findById(payoutId)
			.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Payout not found"));
		return toPayoutDto(payout);
	}

	 public List<EscrowLedgerEntryAdminDto> getAllLedgerEntries(UUID actorUserId) {
		adminAuthorizationService.requireAdminOrSuperAdmin(requireActorUserId(actorUserId));
		return escrowLedgerEntryRepository.findAll().stream()
		 .map(this::toLedgerDto)
		 .toList();
	 }

	 public EscrowLedgerEntryAdminDto getLedgerEntryById(UUID actorUserId, UUID ledgerEntryId) {
		adminAuthorizationService.requireAdminOrSuperAdmin(requireActorUserId(actorUserId));
		EscrowLedgerEntryEntity ledgerEntry = escrowLedgerEntryRepository.findById(ledgerEntryId)
		 .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Ledger entry not found"));
		return toLedgerDto(ledgerEntry);
	 }

	 public PayoutReconciliationResultDto reconcileStuckProcessingPayouts(UUID actorUserId) {
		UUID adminUserId = requireActorUserId(actorUserId);
		adminAuthorizationService.requireAdminOrSuperAdmin(adminUserId);
		int reconciledCount = paymentService.reconcileStuckProcessingPayouts(
				adminUserId,
				"Manual admin reconciliation: stale payout callback timeout"
		);
		return PayoutReconciliationResultDto.builder()
				.reconciledCount(reconciledCount)
				.message("Reconciliation completed")
				.build();
	 }

	public CallbackReplayResult replayUnmatchedB2cCallbacks(UUID actorUserId) {
		UUID adminUserId = requireActorUserId(actorUserId);
		adminAuthorizationService.requireAdminOrSuperAdmin(adminUserId);
		return paymentService.replayUnmatchedB2cCallbacks(adminUserId);
	}

	private UUID requireActorUserId(UUID actorUserId) {
		if (actorUserId == null) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, "Missing required header: X-Actor-User-Id");
		}
		return actorUserId;
	}

	private PaymentIntentAdminDto toPaymentIntentDto(PaymentIntentEntity paymentIntent) {
		UUID transactionId = paymentIntent.getTransaction() != null ? paymentIntent.getTransaction().getId() : null;
		UUID buyerId = paymentIntent.getBuyer() != null ? paymentIntent.getBuyer().getId() : null;
		UUID sellerId = paymentIntent.getSeller() != null ? paymentIntent.getSeller().getId() : null;

		return new PaymentIntentAdminDto(
			paymentIntent.getId(),
			transactionId,
			buyerId,
			sellerId,
			paymentIntent.getProvider(),
			paymentIntent.getProviderRef(),
			paymentIntent.getAmount(),
			paymentIntent.getCurrency(),
			paymentIntent.getPaymentMethod(),
			paymentIntent.getPhoneNumber(),
			paymentIntent.getStatus(),
			paymentIntent.getCheckoutRequestId(),
			paymentIntent.getMerchantRequestId(),
			paymentIntent.getMpesaReceiptNumber(),
			paymentIntent.getProviderResponseCode(),
			paymentIntent.getProviderResponseDescription(),
			paymentIntent.getPaidAt(),
			paymentIntent.getCreatedAt(),
			paymentIntent.getUpdatedAt()
		);
	}

	private PayoutAdminDto toPayoutDto(PayoutEntity payout) {
		UUID transactionId = payout.getTransaction() != null ? payout.getTransaction().getId() : null;
		UUID sellerId = payout.getSeller() != null ? payout.getSeller().getId() : null;

		return new PayoutAdminDto(
			payout.getId(),
			transactionId,
			sellerId,
			payout.getProvider(),
			payout.getAmount(),
			payout.getCurrency(),
			payout.getSellerPhoneNumber(),
			payout.getStatus(),
			payout.getConversationId(),
			payout.getOriginatorConversationId(),
			payout.getResultCode(),
			payout.getResultDescription(),
			payout.getPaidAt(),
			payout.getCreatedAt(),
			payout.getUpdatedAt()
		);
	}

		 private EscrowLedgerEntryAdminDto toLedgerDto(EscrowLedgerEntryEntity ledgerEntry) {
		  UUID transactionId = ledgerEntry.getTransaction() != null ? ledgerEntry.getTransaction().getId() : null;

		  return new EscrowLedgerEntryAdminDto(
		   ledgerEntry.getId(),
		   transactionId,
		   ledgerEntry.getEntryType(),
		   ledgerEntry.getDirection(),
		   ledgerEntry.getAmount(),
		   ledgerEntry.getCurrency(),
		   ledgerEntry.getReferenceId(),
		   ledgerEntry.getReferenceType(),
		   ledgerEntry.getCreatedAt()
		  );
		 }
    
}

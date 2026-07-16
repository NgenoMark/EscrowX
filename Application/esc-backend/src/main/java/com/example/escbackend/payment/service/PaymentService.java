package com.example.escbackend.payment.service;

import com.example.escbackend.common.constants.UserRole;
import com.example.escbackend.common.exception.ApiException;
import com.example.escbackend.escrow.entity.EscrowTransaction;
import com.example.escbackend.escrow.repository.EscrowRepository;
import com.example.escbackend.escrow.service.TransactionStatusHistoryService;
import com.example.escbackend.payment.dto.InitiateStkPushRequest;
import com.example.escbackend.payment.dto.InitiateStkPushResponse;
import com.example.escbackend.payment.dto.PaymentResponse;
import com.example.escbackend.payment.dto.PaymentIntentFinanceResponse;
import com.example.escbackend.payment.dto.PayoutFinanceResponse;
import com.example.escbackend.payment.dto.ReleasePayoutResponse;
import com.example.escbackend.payment.entity.PaymentCallbackEntity;
import com.example.escbackend.payment.entity.PaymentIntentEntity;
import com.example.escbackend.payment.entity.PayoutEntity;
import com.example.escbackend.payment.repository.PaymentCallbackRepository;
import com.example.escbackend.payment.repository.PaymentIntentRepository;
import com.example.escbackend.payment.repository.PayoutRepository;
import com.example.escbackend.notification.service.TransactionNotificationService;
import com.example.escbackend.user.entity.UserEntity;
import com.example.escbackend.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentService {
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final String PAYOUT_STATUS_PROCESSING = "PROCESSING";
    private static final String PAYOUT_STATUS_FAILED = "FAILED";

    private final EscrowRepository escrowRepository;
    private final PaymentIntentRepository paymentIntentRepository;
    private final PayoutRepository payoutRepository;
    private final PaymentCallbackRepository callbackRepository;
    private final MpesaDarajaService mpesaDarajaService;
    private final LedgerService ledgerService;
    private final TransactionStatusHistoryService transactionStatusHistoryService;
    private final UserRepository userRepository;
    private final TransactionNotificationService transactionNotificationService;

    @Value("${escrowx.mpesa.reconciliation.enabled:true}")
    private boolean reconciliationEnabled;

    @Value("${escrowx.mpesa.reconciliation.max-processing-minutes:15}")
    private long maxProcessingMinutes;

    public PaymentService(
            EscrowRepository escrowRepository,
            PaymentIntentRepository paymentIntentRepository,
            PayoutRepository payoutRepository,
            PaymentCallbackRepository callbackRepository,
            MpesaDarajaService mpesaDarajaService,
            LedgerService ledgerService,
            TransactionStatusHistoryService transactionStatusHistoryService,
                UserRepository userRepository,
                TransactionNotificationService transactionNotificationService
    ) {
        this.escrowRepository = escrowRepository;
        this.paymentIntentRepository = paymentIntentRepository;
        this.payoutRepository = payoutRepository;
        this.callbackRepository = callbackRepository;
        this.mpesaDarajaService = mpesaDarajaService;
        this.ledgerService = ledgerService;
        this.transactionStatusHistoryService = transactionStatusHistoryService;
        this.userRepository = userRepository;
        this.transactionNotificationService = transactionNotificationService;
    }

    @Transactional
    public InitiateStkPushResponse initiateStkPush(UUID escrowId, InitiateStkPushRequest request, UUID actorUserId) {
        EscrowTransaction transaction = getEscrowOrThrow(escrowId);
        assertStateIn(transaction, List.of("CREATED", "PENDING_PAYMENT"));

        PaymentIntentEntity payment = paymentIntentRepository.findByTransactionId(escrowId)
                .orElseGet(() -> newPaymentIntent(transaction, request.getPhoneNumber()));
        payment.setPhoneNumber(request.getPhoneNumber());
        payment.setStatus("INITIATED");
        payment = paymentIntentRepository.save(payment);

        MpesaDarajaService.StkPushResult result = mpesaDarajaService.initiateStkPush(
                request.getPhoneNumber(),
                toWholeShillings(payment.getAmount()),
                transaction.getReference(),
                "EscrowX payment " + transaction.getReference()
        );

        payment.setMerchantRequestId(result.merchantRequestId());
        payment.setCheckoutRequestId(result.checkoutRequestId());
        payment.setProviderResponseCode(result.responseCode());
        payment.setProviderResponseDescription(result.responseDescription());
        payment.setStatus("PENDING");
        paymentIntentRepository.save(payment);

        updateEscrowStatus(transaction, "PENDING_PAYMENT", actorUserId, "STK push initiated");
        notifyPaymentEvent(
            transaction,
            "PAYMENT_STK_INITIATED",
            "Payment prompt sent",
            "An STK payment prompt was sent for transaction " + transaction.getReference() + ".",
            actorUserId
        );

        return InitiateStkPushResponse.builder()
                .paymentId(payment.getId())
                .escrowId(transaction.getId())
                .status(payment.getStatus())
                .checkoutRequestId(payment.getCheckoutRequestId())
                .merchantRequestId(payment.getMerchantRequestId())
                .message(result.customerMessage())
                .build();
    }

    public PaymentResponse getPayment(UUID paymentId) {
        PaymentIntentEntity payment = paymentIntentRepository.findById(paymentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Payment not found"));
        return toPaymentResponse(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentIntentFinanceResponse> getMyPaymentIntents(UUID actorUserId) {
        UUID userId = requireActorUserId(actorUserId);
        return paymentIntentRepository.findByBuyerIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toPaymentIntentFinanceResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PayoutFinanceResponse> getMyPayouts(UUID actorUserId) {
        UUID userId = requireActorUserId(actorUserId);
        return payoutRepository.findBySellerIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toPayoutFinanceResponse)
                .toList();
    }

    @Transactional
    public void handleStkCallback(Map<String, Object> payload) {
        Map<String, Object> stkCallback = nestedMap(payload, "Body", "stkCallback");
        String checkoutRequestId = asString(stkCallback.get("CheckoutRequestID"));
        Integer resultCode = asInteger(stkCallback.get("ResultCode"));
        String resultDescription = asString(stkCallback.get("ResultDesc"));

        PaymentIntentEntity payment = paymentIntentRepository.findByCheckoutRequestId(checkoutRequestId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Payment not found for checkout request"));

        // Idempotency: If already processed, exit early to prevent duplicate ledger records
        if ("PAID".equals(payment.getStatus()) || "FAILED".equals(payment.getStatus())) {
            log.info("STK Push for checkoutRequestId={} has already been processed with status={}. Skipping.", checkoutRequestId, payment.getStatus());
            return;
        }

        savePaymentCallback(payment, null, "STK_CALLBACK", checkoutRequestId, payload);

        payment.setProviderResponseCode(resultCode == null ? null : resultCode.toString());
        payment.setProviderResponseDescription(resultDescription);

        if (resultCode != null && resultCode == 0) {
            Map<String, Object> metadata = nestedMap(stkCallback, "CallbackMetadata");
            payment.setMpesaReceiptNumber(asCallbackItem(metadata, "MpesaReceiptNumber"));
            payment.setPaidAt(OffsetDateTime.now());
            payment.setStatus("PAID");

            EscrowTransaction transaction = payment.getTransaction();
            updateEscrowStatus(transaction, "FUNDS_HELD", null, "STK callback success");
            ledgerService.recordHold(transaction, payment.getAmount(), payment.getId());
            notifyPaymentEvent(
                transaction,
                "PAYMENT_SUCCEEDED",
                "Payment received",
                "Payment was successful and funds are now held for transaction " + transaction.getReference() + ".",
                null
            );
        } else {
            payment.setStatus("FAILED");
            EscrowTransaction transaction = payment.getTransaction();
            if ("PENDING_PAYMENT".equals(transaction.getStatus())) {
                updateEscrowStatus(transaction, "PENDING_PAYMENT", null, "STK callback failed");
            }
            notifyPaymentEvent(
                transaction,
                "PAYMENT_FAILED",
                "Payment failed",
                "Payment failed for transaction " + transaction.getReference() + ". Please retry payment.",
                null
            );
        }

        paymentIntentRepository.save(payment);
    }

    @Transactional
    public ReleasePayoutResponse releaseToSeller(UUID escrowId, UUID actorUserId) {
        EscrowTransaction transaction = getEscrowOrThrow(escrowId);
        assertReleaseAuthorization(transaction, actorUserId);
        assertStateIn(transaction, List.of("RELEASE_PENDING", "RELEASE_FAILED"));

        PayoutEntity payout = payoutRepository.findByTransactionId(escrowId)
                .orElseGet(() -> newPayout(transaction));
        payout.setStatus("INITIATED");
        payout = payoutRepository.save(payout);

        updateEscrowStatus(transaction, "RELEASE_PROCESSING", actorUserId, "Payout release initiated");
        notifyPaymentEvent(
            transaction,
            "PAYOUT_RELEASE_INITIATED",
            "Payout release started",
            "Payout release has been initiated for transaction " + transaction.getReference() + ".",
            actorUserId
        );

        MpesaDarajaService.B2cResult result = mpesaDarajaService.initiateB2cPayout(
                payout.getSellerPhoneNumber(),
                toWholeShillings(payout.getAmount()),
                transaction.getReference(),
                "EscrowX seller payout " + transaction.getReference()
        );

        payout.setConversationId(result.conversationId());
        payout.setOriginatorConversationId(result.originatorConversationId());
        payout.setResultCode(result.responseCode());
        payout.setResultDescription(result.responseDescription());

        boolean acceptedForProcessing = "0".equals(result.responseCode())
                && (StringUtils.hasText(result.conversationId()) || StringUtils.hasText(result.originatorConversationId()));
        if (acceptedForProcessing) {
            payout.setStatus("PROCESSING");
        } else {
            log.warn(
                    "B2C payout request rejected by API Gateway. escrowId={}, payoutId={}, responseCode={}, responseDescription={}",
                    transaction.getId(),
                    payout.getId(),
                    result.responseCode(),
                    result.responseDescription()
            );
            payout.setStatus("FAILED");
            updateEscrowStatus(transaction, "RELEASE_FAILED", actorUserId, "Payout request rejected");
            notifyPaymentEvent(
                transaction,
                "PAYOUT_RELEASE_FAILED",
                "Payout release failed",
                "Payout release request was rejected for transaction " + transaction.getReference() + ".",
                actorUserId
            );
        }
        payoutRepository.save(payout);
        escrowRepository.save(transaction);

        return ReleasePayoutResponse.builder()
                .payoutId(payout.getId())
                .escrowId(transaction.getId())
                .status(payout.getStatus())
                .conversationId(payout.getConversationId())
                .originatorConversationId(payout.getOriginatorConversationId())
                .message(result.responseDescription())
                .build();
    }

    private void assertReleaseAuthorization(EscrowTransaction transaction, UUID actorUserId) {
        if (actorUserId == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Missing required header: X-Actor-User-Id");
        }

        if (transaction.getBuyer().getId().equals(actorUserId)) {
            return;
        }

        UserEntity actor = userRepository.findById(actorUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Actor user not found"));

        if (actor.getRole() != UserRole.ADMIN && actor.getRole() != UserRole.SUPER_ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only the buyer or ADMIN/SUPER_ADMIN can authorize payout release");
        }
    }

    @Scheduled(fixedDelayString = "${escrowx.mpesa.reconciliation.fixed-delay-ms:60000}")
    @Transactional
    public void reconcileStuckProcessingPayoutsScheduled() {
        if (!reconciliationEnabled) {
            return;
        }
        int reconciled = reconcileStuckProcessingPayouts(
                null,
                "Scheduled reconciliation: callback timeout guard"
        );
        if (reconciled > 0) {
            log.warn("Reconciled {} stale payouts that were stuck in PROCESSING.", reconciled);
        }
    }

    @Transactional
    public int reconcileStuckProcessingPayouts(UUID actorUserId, String reason) {
        OffsetDateTime cutoff = OffsetDateTime.now().minusMinutes(maxProcessingMinutes);
        List<PayoutEntity> stalePayouts = payoutRepository.findByStatusAndUpdatedAtBefore(PAYOUT_STATUS_PROCESSING, cutoff);

        if (stalePayouts.isEmpty()) {
            return 0;
        }

        int reconciledCount = 0;
        for (PayoutEntity payout : stalePayouts) {
            if (!PAYOUT_STATUS_PROCESSING.equals(payout.getStatus())) {
                continue;
            }

            Map<String, Object> syntheticPayload = new HashMap<>();
            syntheticPayload.put("source", "SYSTEM_RECONCILER");
            syntheticPayload.put("reason", reason);
            syntheticPayload.put("reconciledAt", OffsetDateTime.now().toString());
            syntheticPayload.put("conversationId", payout.getConversationId());
            syntheticPayload.put("originatorConversationId", payout.getOriginatorConversationId());

                String providerEventId = "RECON-" + payout.getId();

            savePaymentCallback(null, payout, "B2C_TIMEOUT_RECON", providerEventId, syntheticPayload);

            payout.setStatus(PAYOUT_STATUS_FAILED);
            payout.setResultDescription("No B2C callback received within " + maxProcessingMinutes + " minutes.");

            EscrowTransaction transaction = payout.getTransaction();
            updateEscrowStatus(transaction, "RELEASE_FAILED", actorUserId, reason);
            notifyPaymentEvent(
                transaction,
                "PAYOUT_RELEASE_FAILED",
                "Payout release failed",
                "Payout release failed for transaction " + transaction.getReference() + ". " + reason,
                actorUserId
            );

            payoutRepository.save(payout);
            escrowRepository.save(transaction);
            reconciledCount++;
        }

        return reconciledCount;
    }

    @Transactional
    public void handleB2cResult(Map<String, Object> payload) {
        log.info("Raw inbound B2C Result payload received: {}", payload);

        // Safely capture the main result body wrapper node
        Map<String, Object> result = nestedMap(payload, "Result");

        // Fallback check: If Sandbox nests it differently or maps via alternate keys
        if (result.isEmpty() && payload.containsKey("Result")) {
            Object rawResult = payload.get("Result");
            if (rawResult instanceof Map) {
                result = (Map<String, Object>) rawResult;
            }
        }

        if (result.isEmpty()) {
            log.error("CRITICAL: Malformed B2C Result callback structure. Missing actionable content parameters. Payload: {}", payload);
            throw new ApiException(HttpStatus.BAD_REQUEST, "Missing Result payload structure parsing targets");
        }

        String conversationId = asString(result.get("ConversationID"));
        String originatorConversationId = asString(result.get("OriginatorConversationID"));
        Integer resultCode = asInteger(result.get("ResultCode"));
        String resultDescription = asString(result.get("ResultDesc"));
        String mpesaTransactionId = asString(result.get("TransactionID"));

        log.info(
                "Parsed B2C Callback Metrics: conversationId={}, originatorConversationId={}, resultCode={}, description={}, mpesaTxId={}",
                conversationId,
                originatorConversationId,
                resultCode,
                resultDescription,
                mpesaTransactionId
        );

        // Flexible multi-tier database lookup sync execution
        Optional<PayoutEntity> payoutMatch = Optional.empty();
        if (StringUtils.hasText(originatorConversationId)) {
            payoutMatch = payoutRepository.findByOriginatorConversationId(originatorConversationId);
        }
        if (payoutMatch.isEmpty() && StringUtils.hasText(conversationId)) {
            payoutMatch = payoutRepository.findByConversationId(conversationId);
        }

        if (payoutMatch.isEmpty()) {
            log.error("CRITICAL AUDIT ERROR: Received authentic Safaricom B2C callback but could not find a matching Payout record in database! OriginatorID={}, ConversationID={}",
                    originatorConversationId, conversationId);
            throw new ApiException(HttpStatus.NOT_FOUND, "Associated target payout entry tracking index not found");
        }
        PayoutEntity payout = payoutMatch.get();

        // Idempotency: Prevent double accounting modifications from gateway retries
        if ("PAID".equals(payout.getStatus()) || "FAILED".equals(payout.getStatus())) {
            log.info("Payout ID {} has already completed processing operations with status={}. Terminating handler gracefully.", payout.getId(), payout.getStatus());
            return;
        }

        savePaymentCallback(null, payout, "B2C_RESULT", conversationId, payload);
        payout.setResultCode(resultCode == null ? null : resultCode.toString());
        payout.setResultDescription(resultDescription);

        EscrowTransaction transaction = payout.getTransaction();
        if (resultCode != null && resultCode == 0) {
            payout.setStatus("PAID");
            payout.setPaidAt(OffsetDateTime.now());
            updateEscrowStatus(transaction, "COMPLETED", null, "B2C callback success");

            log.info("B2C Verification Payout process successful for transaction reference {}. Dispatching ledger releases.", transaction.getReference());
            ledgerService.recordRelease(transaction, payout.getAmount(), payout.getId());
            notifyPaymentEvent(
                transaction,
                "PAYOUT_RELEASE_COMPLETED",
                "Payout completed",
                "Payout to seller completed for transaction " + transaction.getReference() + ".",
                null
            );
        } else {
            payout.setStatus("FAILED");
            updateEscrowStatus(transaction, "RELEASE_FAILED", null, "B2C callback failed");
            log.warn("M-Pesa Core Layer rejected payout request for Payout ID {} with Safaricom reason code details: {}", payout.getId(), resultDescription);
            notifyPaymentEvent(
                transaction,
                "PAYOUT_RELEASE_FAILED",
                "Payout failed",
                "Payout failed for transaction " + transaction.getReference() + ".",
                null
            );
        }

        payoutRepository.save(payout);
        escrowRepository.save(transaction);
    }

    @Transactional
    public void handleB2cTimeout(Map<String, Object> payload) {
        log.warn("Raw inbound B2C Timeout notification received: {}", payload);

        Map<String, Object> result = nestedMap(payload, "Result");
        if (result.isEmpty() && payload.containsKey("Result")) {
            Object rawResult = payload.get("Result");
            if (rawResult instanceof Map) {
                result = (Map<String, Object>) rawResult;
            }
        }

        String conversationId = asString(result.get("ConversationID"));
        String originatorConversationId = asString(result.get("OriginatorConversationID"));

        Optional<PayoutEntity> payoutMatch = Optional.empty();
        if (StringUtils.hasText(originatorConversationId)) {
            payoutMatch = payoutRepository.findByOriginatorConversationId(originatorConversationId);
        }
        if (payoutMatch.isEmpty() && StringUtils.hasText(conversationId)) {
            payoutMatch = payoutRepository.findByConversationId(conversationId);
        }

        if (payoutMatch.isEmpty()) {
            log.error("Failed to map incoming B2C timeout notification back to an active database entity. conversationId={}, originatorID={}", conversationId, originatorConversationId);
            throw new ApiException(HttpStatus.NOT_FOUND, "Target lookup context not available for timeout event parsing");
        }
        PayoutEntity payout = payoutMatch.get();

        if (PAYOUT_STATUS_FAILED.equals(payout.getStatus()) || "PAID".equals(payout.getStatus())) {
            log.info("Payout entry status index tracking target {} already marked settled. Terminating timeout task handler context.", payout.getId());
            return;
        }

        savePaymentCallback(null, payout, "B2C_TIMEOUT", conversationId, payload);
        payout.setStatus(PAYOUT_STATUS_FAILED);
        payout.setResultDescription("The B2C payout operational transaction sequence timed out inside Safaricom system queues.");

        EscrowTransaction transaction = payout.getTransaction();
        updateEscrowStatus(transaction, "RELEASE_FAILED", null, "B2C timeout");
        notifyPaymentEvent(
            transaction,
            "PAYOUT_RELEASE_TIMEOUT",
            "Payout timeout",
            "Payout timed out for transaction " + transaction.getReference() + ".",
            null
        );

        payoutRepository.save(payout);
        escrowRepository.save(transaction);
    }

    private void updateEscrowStatus(
            EscrowTransaction transaction,
            String newStatus,
            UUID changedBy,
            String reason
    ) {
        String fromStatus = transaction.getStatus();
        transaction.setStatus(newStatus);
        EscrowTransaction saved = escrowRepository.save(transaction);
        transactionStatusHistoryService.recordStatusChange(saved, fromStatus, newStatus, changedBy, reason);
    }

    private void notifyStatusChange(
        EscrowTransaction transaction,
        String fromStatus,
        String newStatus,
        UUID actorUserId,
        String reason
    ) {
        if (fromStatus != null && fromStatus.equalsIgnoreCase(newStatus)) {
            return;
        }

        Map<UUID, String> recipients = collectRecipients(transaction, actorUserId);
        for (Map.Entry<UUID, String> entry : recipients.entrySet()) {
            UUID recipientId = entry.getKey();
            String targetRole = entry.getValue();
            String title = recipientId.equals(actorUserId)
                ? "You updated transaction " + transaction.getReference()
                : "Transaction " + transaction.getReference() + " updated";
            String body = "Status changed from " + humanizeStatus(fromStatus) + " to " + humanizeStatus(newStatus) + ".";
            if (reason != null && !reason.isBlank()) {
                body = body + " " + reason;
            }

            safeNotify(
                recipientId,
                transaction,
                "TRANSACTION_STATUS_" + newStatus,
                title,
                body,
                newStatus,
                targetRole
            );
        }
    }

    private void notifyPaymentEvent(
        EscrowTransaction transaction,
        String type,
        String title,
        String body,
        UUID actorUserId
    ) {
        String status = transaction.getStatus();
        Map<UUID, String> recipients = collectRecipients(transaction, actorUserId);
        for (Map.Entry<UUID, String> entry : recipients.entrySet()) {
            safeNotify(
                entry.getKey(),
                transaction,
                type,
                title,
                body,
                status,
                entry.getValue()
            );
        }
    }

    private Map<UUID, String> collectRecipients(EscrowTransaction transaction, UUID actorUserId) {
        Map<UUID, String> recipients = new HashMap<>();
        recipients.put(transaction.getBuyer().getId(), "BUYER");
        recipients.put(transaction.getSeller().getId(), "SELLER");
        if (transaction.getRider() != null) {
            recipients.put(transaction.getRider().getId(), "RIDER");
        }
        if (actorUserId != null && !recipients.containsKey(actorUserId)) {
            UserEntity actor = userRepository.findById(actorUserId).orElse(null);
            recipients.put(actorUserId, actor == null ? "ADMIN" : actor.getRole().name());
        }
        return recipients;
    }

    private void safeNotify(
        UUID recipientId,
        EscrowTransaction transaction,
        String type,
        String title,
        String body,
        String status,
        String targetRole
    ) {
        try {
            transactionNotificationService.sendTransactionNotification(
                recipientId,
                transaction.getId(),
                type,
                title,
                body,
                status,
                targetRole
            );
        } catch (Exception ignored) {
            // Notifications must not block payment operations.
        }
    }

    private String humanizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "unknown";
        }
        return status.toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    private PaymentIntentEntity newPaymentIntent(EscrowTransaction transaction, String phoneNumber) {
        PaymentIntentEntity payment = new PaymentIntentEntity();
        payment.setTransaction(transaction);
        payment.setBuyer(transaction.getBuyer());
        payment.setSeller(transaction.getSeller());
        payment.setProvider("MPESA");
        payment.setPaymentMethod("STK_PUSH");
        payment.setProviderRef(transaction.getReference());
        payment.setAmount(transaction.getInitialDepositAmount());
        payment.setCurrency(transaction.getCurrency());
        payment.setPhoneNumber(phoneNumber);
        payment.setStatus("INITIATED");
        return payment;
    }

    private PayoutEntity newPayout(EscrowTransaction transaction) {
        PayoutEntity payout = new PayoutEntity();
        payout.setTransaction(transaction);
        payout.setSeller(transaction.getSeller());
        payout.setProvider("MPESA");
        payout.setAmount(transaction.getAmount());
        payout.setCurrency(transaction.getCurrency());
        payout.setSellerPhoneNumber(transaction.getSeller().getPhone());
        payout.setStatus("INITIATED");
        return payout;
    }

    private void savePaymentCallback(
            PaymentIntentEntity payment,
            PayoutEntity payout,
            String callbackType,
            String providerEventId,
            Map<String, Object> payload
    ) {
        if (StringUtils.hasText(providerEventId) && callbackRepository.findByProviderEventId(providerEventId).isPresent()) {
            log.info("Skipping duplicate callback persist for providerEventId={} callbackType={}", providerEventId, callbackType);
            return;
        }

        PaymentCallbackEntity callback = new PaymentCallbackEntity();
        callback.setPaymentIntent(payment);
        callback.setPayout(payout);
        callback.setCallbackType(callbackType);
        callback.setProviderEventId(providerEventId);
        callback.setRawPayload(payload);
        callback.setProcessedAt(OffsetDateTime.now());
        callbackRepository.save(callback);
    }

    private PaymentResponse toPaymentResponse(PaymentIntentEntity payment) {
        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .escrowId(payment.getTransaction().getId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .phoneNumber(payment.getPhoneNumber())
                .checkoutRequestId(payment.getCheckoutRequestId())
                .merchantRequestId(payment.getMerchantRequestId())
                .mpesaReceiptNumber(payment.getMpesaReceiptNumber())
                .paidAt(payment.getPaidAt())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }

    private EscrowTransaction getEscrowOrThrow(UUID escrowId) {
        return escrowRepository.findById(escrowId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Escrow transaction not found"));
    }

    private UUID requireActorUserId(UUID actorUserId) {
        if (actorUserId == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Missing required header: X-Actor-User-Id");
        }
        userRepository.findById(actorUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Actor user not found"));
        return actorUserId;
    }

    private PaymentIntentFinanceResponse toPaymentIntentFinanceResponse(PaymentIntentEntity payment) {
        return PaymentIntentFinanceResponse.builder()
                .paymentId(payment.getId())
                .transactionId(payment.getTransaction() == null ? null : payment.getTransaction().getId())
                .transactionReference(payment.getTransaction() == null ? null : payment.getTransaction().getReference())
                .buyerId(payment.getBuyer() == null ? null : payment.getBuyer().getId())
                .sellerId(payment.getSeller() == null ? null : payment.getSeller().getId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .paymentMethod(payment.getPaymentMethod())
                .phoneNumber(payment.getPhoneNumber())
                .mpesaReceiptNumber(payment.getMpesaReceiptNumber())
                .paidAt(payment.getPaidAt())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }

    private PayoutFinanceResponse toPayoutFinanceResponse(PayoutEntity payout) {
        return PayoutFinanceResponse.builder()
                .payoutId(payout.getId())
                .transactionId(payout.getTransaction() == null ? null : payout.getTransaction().getId())
                .transactionReference(payout.getTransaction() == null ? null : payout.getTransaction().getReference())
                .sellerId(payout.getSeller() == null ? null : payout.getSeller().getId())
                .amount(payout.getAmount())
                .currency(payout.getCurrency())
                .status(payout.getStatus())
                .conversationId(payout.getConversationId())
                .originatorConversationId(payout.getOriginatorConversationId())
                .resultCode(payout.getResultCode())
                .resultDescription(payout.getResultDescription())
                .paidAt(payout.getPaidAt())
                .createdAt(payout.getCreatedAt())
                .updatedAt(payout.getUpdatedAt())
                .build();
    }

    private void assertStateIn(EscrowTransaction transaction, List<String> statuses) {
        if (!statuses.contains(transaction.getStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid escrow status for payment action: " + transaction.getStatus());
        }
    }

    private int toWholeShillings(BigDecimal amount) {
        return amount.setScale(0, RoundingMode.HALF_UP).intValueExact();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> nestedMap(Map<String, Object> source, String... keys) {
        Object current = source;
        for (String key : keys) {
            if (!(current instanceof Map<?, ?> map)) {
                return Map.of();
            }
            current = map.get(key);
        }
        return current instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    @SuppressWarnings("unchecked")
    private String asCallbackItem(Map<String, Object> metadata, String name) {
        Object items = metadata.get("Item");
        if (!(items instanceof List<?> list)) {
            return null;
        }
        for (Object item : list) {
            if (item instanceof Map<?, ?> map && name.equals(map.get("Name"))) {
                Object value = map.get("Value");
                return value == null ? null : value.toString();
            }
        }
        return null;
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }

    private Integer asInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            log.warn("Failed to parse string value '{}' to Integer during callback processing", value);
            return null;
        }
    }
}
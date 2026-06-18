package com.example.escbackend.payment.service;

import com.example.escbackend.common.exception.ApiException;
import com.example.escbackend.escrow.entity.EscrowTransaction;
import com.example.escbackend.escrow.repository.EscrowRepository;
import com.example.escbackend.escrow.service.TransactionStatusHistoryService;
import com.example.escbackend.payment.dto.InitiateStkPushRequest;
import com.example.escbackend.payment.dto.InitiateStkPushResponse;
import com.example.escbackend.payment.dto.PaymentResponse;
import com.example.escbackend.payment.dto.ReleasePayoutResponse;
import com.example.escbackend.payment.entity.PaymentCallbackEntity;
import com.example.escbackend.payment.entity.PaymentIntentEntity;
import com.example.escbackend.payment.entity.PayoutEntity;
import com.example.escbackend.payment.repository.PaymentCallbackRepository;
import com.example.escbackend.payment.repository.PaymentIntentRepository;
import com.example.escbackend.payment.repository.PayoutRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentService {
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final EscrowRepository escrowRepository;
    private final PaymentIntentRepository paymentIntentRepository;
    private final PayoutRepository payoutRepository;
    private final PaymentCallbackRepository callbackRepository;
    private final MpesaDarajaService mpesaDarajaService;
    private final LedgerService ledgerService;
    private final TransactionStatusHistoryService transactionStatusHistoryService;

    public PaymentService(
            EscrowRepository escrowRepository,
            PaymentIntentRepository paymentIntentRepository,
            PayoutRepository payoutRepository,
            PaymentCallbackRepository callbackRepository,
            MpesaDarajaService mpesaDarajaService,
            LedgerService ledgerService,
            TransactionStatusHistoryService transactionStatusHistoryService
    ) {
        this.escrowRepository = escrowRepository;
        this.paymentIntentRepository = paymentIntentRepository;
        this.payoutRepository = payoutRepository;
        this.callbackRepository = callbackRepository;
        this.mpesaDarajaService = mpesaDarajaService;
        this.ledgerService = ledgerService;
        this.transactionStatusHistoryService = transactionStatusHistoryService;
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
        } else {
            payment.setStatus("FAILED");
            EscrowTransaction transaction = payment.getTransaction();
            if ("PENDING_PAYMENT".equals(transaction.getStatus())) {
                updateEscrowStatus(transaction, "PENDING_PAYMENT", null, "STK callback failed");
            }
        }

        paymentIntentRepository.save(payment);
    }

    @Transactional
    public ReleasePayoutResponse releaseToSeller(UUID escrowId, UUID actorUserId) {
        EscrowTransaction transaction = getEscrowOrThrow(escrowId);
        assertStateIn(transaction, List.of("RELEASE_PENDING", "RELEASE_FAILED"));

        PayoutEntity payout = payoutRepository.findByTransactionId(escrowId)
                .orElseGet(() -> newPayout(transaction));
        payout.setStatus("INITIATED");
        payout = payoutRepository.save(payout);

        updateEscrowStatus(transaction, "RELEASE_PROCESSING", actorUserId, "Payout release initiated");

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
        } else {
            payout.setStatus("FAILED");
            updateEscrowStatus(transaction, "RELEASE_FAILED", null, "B2C callback failed");
            log.warn("M-Pesa Core Layer rejected payout request for Payout ID {} with Safaricom reason code details: {}", payout.getId(), resultDescription);
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

        if ("FAILED".equals(payout.getStatus()) || "PAID".equals(payout.getStatus())) {
            log.info("Payout entry status index tracking target {} already marked settled. Terminating timeout task handler context.", payout.getId());
            return;
        }

        savePaymentCallback(null, payout, "B2C_TIMEOUT", conversationId, payload);
        payout.setStatus("FAILED");
        payout.setResultDescription("The B2C payout operational transaction sequence timed out inside Safaricom system queues.");

        EscrowTransaction transaction = payout.getTransaction();
        updateEscrowStatus(transaction, "RELEASE_FAILED", null, "B2C timeout");

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
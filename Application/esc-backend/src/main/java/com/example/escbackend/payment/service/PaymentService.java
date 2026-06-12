package com.example.escbackend.payment.service;

import com.example.escbackend.common.exception.ApiException;
import com.example.escbackend.escrow.entity.EscrowTransaction;
import com.example.escbackend.escrow.repository.EscrowRepository;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentService {
    private final EscrowRepository escrowRepository;
    private final PaymentIntentRepository paymentIntentRepository;
    private final PayoutRepository payoutRepository;
    private final PaymentCallbackRepository callbackRepository;
    private final MpesaDarajaService mpesaDarajaService;
    private final LedgerService ledgerService;

    public PaymentService(
        EscrowRepository escrowRepository,
        PaymentIntentRepository paymentIntentRepository,
        PayoutRepository payoutRepository,
        PaymentCallbackRepository callbackRepository,
        MpesaDarajaService mpesaDarajaService,
        LedgerService ledgerService
    ) {
        this.escrowRepository = escrowRepository;
        this.paymentIntentRepository = paymentIntentRepository;
        this.payoutRepository = payoutRepository;
        this.callbackRepository = callbackRepository;
        this.mpesaDarajaService = mpesaDarajaService;
        this.ledgerService = ledgerService;
    }

    @Transactional
    public InitiateStkPushResponse initiateStkPush(UUID escrowId, InitiateStkPushRequest request) {
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

        transaction.setStatus("PENDING_PAYMENT");
        escrowRepository.save(transaction);

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

        savePaymentCallback(payment, null, "STK_CALLBACK", checkoutRequestId, payload);

        payment.setProviderResponseCode(resultCode == null ? null : resultCode.toString());
        payment.setProviderResponseDescription(resultDescription);

        if (Integer.valueOf(0).equals(resultCode)) {
            Map<String, Object> metadata = nestedMap(stkCallback, "CallbackMetadata");
            payment.setMpesaReceiptNumber(asCallbackItem(metadata, "MpesaReceiptNumber"));
            payment.setPaidAt(OffsetDateTime.now());
            payment.setStatus("PAID");

            EscrowTransaction transaction = payment.getTransaction();
            transaction.setStatus("FUNDS_HELD");
            escrowRepository.save(transaction);
            ledgerService.recordHold(transaction, payment.getAmount(), payment.getId());
        } else {
            payment.setStatus("PAYMENT_FAILED");
        }

        paymentIntentRepository.save(payment);
    }

    @Transactional
    public ReleasePayoutResponse releaseToSeller(UUID escrowId) {
        EscrowTransaction transaction = getEscrowOrThrow(escrowId);
        assertStateIn(transaction, List.of("RELEASE_PENDING", "RELEASE_FAILED"));

        PayoutEntity payout = payoutRepository.findByTransactionId(escrowId)
            .orElseGet(() -> newPayout(transaction));
        payout.setStatus("INITIATED");
        payout = payoutRepository.save(payout);

        transaction.setStatus("RELEASE_PROCESSING");
        escrowRepository.save(transaction);

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
        payout.setStatus("PROCESSING");
        payoutRepository.save(payout);

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
        Map<String, Object> result = nestedMap(payload, "Result");
        String conversationId = asString(result.get("ConversationID"));
        String originatorConversationId = asString(result.get("OriginatorConversationID"));
        Integer resultCode = asInteger(result.get("ResultCode"));
        String resultDescription = asString(result.get("ResultDesc"));

        PayoutEntity payout = payoutRepository.findByConversationId(conversationId)
            .or(() -> payoutRepository.findByOriginatorConversationId(originatorConversationId))
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Payout not found for B2C result"));

        savePaymentCallback(null, payout, "B2C_RESULT", conversationId, payload);
        payout.setResultCode(resultCode == null ? null : resultCode.toString());
        payout.setResultDescription(resultDescription);

        EscrowTransaction transaction = payout.getTransaction();
        if (Integer.valueOf(0).equals(resultCode)) {
            payout.setStatus("PAID");
            payout.setPaidAt(OffsetDateTime.now());
            transaction.setStatus("COMPLETED");
            ledgerService.recordRelease(transaction, payout.getAmount(), payout.getId());
        } else {
            payout.setStatus("FAILED");
            transaction.setStatus("RELEASE_FAILED");
        }

        payoutRepository.save(payout);
        escrowRepository.save(transaction);
    }

    @Transactional
    public void handleB2cTimeout(Map<String, Object> payload) {
        Map<String, Object> result = nestedMap(payload, "Result");
        String conversationId = asString(result.get("ConversationID"));
        PayoutEntity payout = payoutRepository.findByConversationId(conversationId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Payout not found for B2C timeout"));

        savePaymentCallback(null, payout, "B2C_TIMEOUT", conversationId, payload);
        payout.setStatus("FAILED");
        payout.setResultDescription("B2C payout timed out");
        payout.getTransaction().setStatus("RELEASE_FAILED");
        payoutRepository.save(payout);
        escrowRepository.save(payout.getTransaction());
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
        return Integer.parseInt(value.toString());
    }
}

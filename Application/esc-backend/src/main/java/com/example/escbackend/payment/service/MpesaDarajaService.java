package com.example.escbackend.payment.service;

import com.example.escbackend.common.exception.ApiException;
import com.example.escbackend.payment.config.MpesaProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;
import static java.util.Map.entry;

@Service
public class MpesaDarajaService {
    private static final Logger log = LoggerFactory.getLogger(MpesaDarajaService.class);
    private static final DateTimeFormatter MPESA_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final MpesaProperties properties;
    private final RestClient restClient;

    public MpesaDarajaService(MpesaProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder().baseUrl(properties.getBaseUrl()).build();
    }

    public StkPushResult initiateStkPush(String phoneNumber, int amount, String accountReference, String description) {
        assertConfigured(properties.getConsumerKey(), "MPESA consumer key is not configured");
        assertConfigured(properties.getConsumerSecret(), "MPESA consumer secret is not configured");
        assertConfigured(properties.getShortCode(), "MPESA short code is not configured");
        assertConfigured(properties.getPassKey(), "MPESA pass key is not configured");
        assertConfigured(properties.getCallbackUrl(), "MPESA callback URL is not configured");

        String timestamp = LocalDateTime.now().format(MPESA_TIMESTAMP);
        String password = Base64.getEncoder()
            .encodeToString((properties.getShortCode() + properties.getPassKey() + timestamp).getBytes(StandardCharsets.UTF_8));

        Map<String, Object> body = Map.ofEntries(
            entry("BusinessShortCode", properties.getShortCode()),
            entry("Password", password),
            entry("Timestamp", timestamp),
            entry("TransactionType", "CustomerPayBillOnline"),
            entry("Amount", amount),
            entry("PartyA", toDarajaPhone(phoneNumber)),
            entry("PartyB", properties.getShortCode()),
            entry("PhoneNumber", toDarajaPhone(phoneNumber)),
            entry("CallBackURL", properties.getCallbackUrl()),
            entry("AccountReference", accountReference),
            entry("TransactionDesc", description)
        );

        Map<?, ?> response;
        try {
            response = restClient.post()
                .uri("/mpesa/stkpush/v1/processrequest")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken())
                .body(body)
                .retrieve()
                .body(Map.class);
        } catch (RestClientResponseException ex) {
            throw toDarajaApiException("STK push", ex);
        }

        if (response == null) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Empty M-Pesa STK response");
        }

        return new StkPushResult(
            asString(response.get("MerchantRequestID")),
            asString(response.get("CheckoutRequestID")),
            asString(response.get("ResponseCode")),
            asString(response.get("ResponseDescription")),
            asString(response.get("CustomerMessage"))
        );
    }

    public B2cResult initiateB2cPayout(String sellerPhoneNumber, int amount, String occasion, String remarks) {
        assertConfigured(properties.getInitiatorName(), "MPESA initiator name is not configured");
        assertConfigured(properties.getSecurityCredential(), "MPESA security credential is not configured");
        assertConfigured(properties.getB2cShortCode(), "MPESA B2C short code is not configured");
        assertConfigured(properties.getResultUrl(), "MPESA B2C result URL is not configured");
        assertConfigured(properties.getTimeoutUrl(), "MPESA B2C timeout URL is not configured");

        String normalizedSellerPhone = toDarajaMsisdn(sellerPhoneNumber);
        String commandId = resolveB2cCommandId();

        log.info(
            "MPESA_B2C_REQUEST commandId={} partyA={} partyBMasked={} amount={} occasion={} resultUrl={} timeoutUrl={}",
            commandId,
            properties.getB2cShortCode(),
            maskMsisdn(normalizedSellerPhone),
            amount,
            occasion,
            properties.getResultUrl(),
            properties.getTimeoutUrl()
        );

        Map<String, Object> body = Map.of(
            "InitiatorName", properties.getInitiatorName(),
            "SecurityCredential", properties.getSecurityCredential(),
            "CommandID", commandId,
            "Amount", amount,
            "PartyA", properties.getB2cShortCode(),
            "PartyB", normalizedSellerPhone,
            "Remarks", remarks,
            "QueueTimeOutURL", properties.getTimeoutUrl(),
            "ResultURL", properties.getResultUrl(),
            "Occasion", occasion
        );

        Map<?, ?> response;
        try {
            response = restClient.post()
                .uri("/mpesa/b2c/v1/paymentrequest")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken())
                .body(body)
                .retrieve()
                .body(Map.class);
        } catch (RestClientResponseException ex) {
            throw toDarajaApiException("B2C payout", ex);
        }

        if (response == null) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Empty M-Pesa B2C response");
        }

        log.info(
            "MPESA_B2C_RESPONSE commandId={} responseCode={} responseDescription={} conversationId={} originatorConversationId={}",
            commandId,
            asString(response.get("ResponseCode")),
            asString(response.get("ResponseDescription")),
            asString(response.get("ConversationID")),
            asString(response.get("OriginatorConversationID"))
        );

        return new B2cResult(
            asString(response.get("ConversationID")),
            asString(response.get("OriginatorConversationID")),
            asString(response.get("ResponseCode")),
            asString(response.get("ResponseDescription"))
        );
    }

    private String accessToken() {
        String basic = Base64.getEncoder()
            .encodeToString((properties.getConsumerKey() + ":" + properties.getConsumerSecret()).getBytes(StandardCharsets.UTF_8));
        Map<?, ?> response;
        try {
            response = restClient.get()
                .uri("/oauth/v1/generate?grant_type=client_credentials")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + basic)
                .retrieve()
                .body(Map.class);
        } catch (RestClientResponseException ex) {
            throw toDarajaApiException("OAuth token", ex);
        }

        String token = response == null ? null : asString(response.get("access_token"));
        if (!StringUtils.hasText(token)) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Could not obtain M-Pesa access token");
        }
        return token;
    }

    private String toDarajaPhone(String phoneNumber) {
        return phoneNumber.replace("+", "");
    }

    private String toDarajaMsisdn(String phoneNumber) {
        if (!StringUtils.hasText(phoneNumber)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Seller phone number is missing for B2C payout");
        }

        String digits = phoneNumber.trim().replace("+", "").replaceAll("\\s+", "");
        if (digits.startsWith("254") && digits.length() == 12) {
            return digits;
        }
        if ((digits.startsWith("07") || digits.startsWith("01")) && digits.length() == 10) {
            return "254" + digits.substring(1);
        }
        if (digits.startsWith("7") && digits.length() == 9) {
            return "254" + digits;
        }
        if (digits.startsWith("1") && digits.length() == 9) {
            return "254" + digits;
        }

        throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported seller phone format for B2C payout: " + phoneNumber);
    }

    private String resolveB2cCommandId() {
        String configured = properties.getB2cCommandId();
        return StringUtils.hasText(configured) ? configured.trim() : "BusinessPayment";
    }

    private String maskMsisdn(String msisdn) {
        if (!StringUtils.hasText(msisdn) || msisdn.length() < 4) {
            return "****";
        }
        int keepStart = Math.min(3, msisdn.length());
        int keepEnd = Math.min(2, msisdn.length() - keepStart);
        String start = msisdn.substring(0, keepStart);
        String end = msisdn.substring(msisdn.length() - keepEnd);
        return start + "******" + end;
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }

    private void assertConfigured(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private ApiException toDarajaApiException(String operation, RestClientResponseException ex) {
        String responseBody = ex.getResponseBodyAsString();
        String payload = StringUtils.hasText(responseBody) ? responseBody : "no response body";
        StringBuilder message = new StringBuilder()
            .append("M-Pesa ")
            .append(operation)
            .append(" request failed: HTTP ")
            .append(ex.getStatusCode().value())
            .append(" ")
            .append(ex.getStatusText())
            .append(". Daraja response: ")
            .append(payload);

        if (ex.getStatusCode().value() == 400 && !StringUtils.hasText(responseBody)) {
            message.append(". This commonly means initiator/security credential/short code mismatch.");
        }

        return new ApiException(HttpStatus.BAD_GATEWAY, message.toString());
    }

    public record StkPushResult(
        String merchantRequestId,
        String checkoutRequestId,
        String responseCode,
        String responseDescription,
        String customerMessage
    ) {
    }

    public record B2cResult(
        String conversationId,
        String originatorConversationId,
        String responseCode,
        String responseDescription
    ) {
    }
}

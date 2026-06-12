package com.example.escbackend.payment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "escrowx.mpesa")
public class MpesaProperties {
    private String baseUrl = "https://sandbox.safaricom.co.ke";
    private String consumerKey;
    private String consumerSecret;
    private String shortCode;
    private String b2cShortCode;
    private String passKey;
    private String callbackUrl;
    private String initiatorName;
    private String securityCredential;
    private String resultUrl;
    private String timeoutUrl;
}

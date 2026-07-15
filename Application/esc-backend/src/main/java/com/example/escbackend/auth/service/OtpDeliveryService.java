package com.example.escbackend.auth.service;

public interface OtpDeliveryService {
    void sendRegistrationOtp(String email, String otp);

    void sendPasswordResetOtp(String email, String otp);

    void sendUpdatePasswordAndPhoneOtp(String email , String otp);

    void sendSellerAcknowledgmentEmail(String email);

    void sendAdminApprovalEmail(String email);

    void sendMarketplaceVerificationSuccessEmail(String email, String role);
}

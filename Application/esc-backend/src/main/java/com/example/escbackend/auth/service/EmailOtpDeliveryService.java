package com.example.escbackend.auth.service;

import com.example.escbackend.common.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailOtpDeliveryService implements OtpDeliveryService {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public EmailOtpDeliveryService(
        JavaMailSender mailSender,
        @Value("${escrowx.otp.email.from:no-reply@escrowx.local}") String fromAddress
    ) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Override
    public void sendRegistrationOtp(String email, String otp) {
        sendOtp(email, "EscrowX account verification code", "Your EscrowX verification code is: " + otp);
    }

    @Override
    public void sendPasswordResetOtp(String email, String otp) {
        sendOtp(email, "EscrowX password reset code", "Your EscrowX password reset code is: " + otp);
    }

    @Override
    public void sendUpdatePasswordAndPhoneOtp(String email, String otp){
        sendOtp(email , "EscrowX update password and phone code" , "Your EscrowX update password and phone code is: " + otp);
    }

    private void sendOtp(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body + "\n\nThis code expires in 10 minutes.");
            mailSender.send(message);
        } catch (MailException ex) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Failed to send OTP email");
        }
    }

    @Override
    public void sendSellerAcknowledgmentEmail(String email) {
        sendMail(
            email,
            "EscrowX seller registration acknowledgment",
            "Your seller account has been verified and is now awaiting admin approval."
                + "\n\nWe will notify you once your account is approved and activated for login."
        );
    }

    private void sendMail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (MailException ex) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Failed to send email");
        }
    }
}

package com.example.escbackend.auth.service;

import com.example.escbackend.common.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {

    private final Random random = new Random();
    private final Map<String, OtpRecord> store = new ConcurrentHashMap<>();

    public String generate(String phone, String purpose) {
        String code = String.format("%06d", random.nextInt(1_000_000));
        store.put(phone + ":" + purpose, new OtpRecord(code, OffsetDateTime.now().plusMinutes(10)));
        return code;
    }

    public void verify(String phone, String purpose, String otp) {
        OtpRecord record = store.get(phone + ":" + purpose);
        if (record == null || record.expiresAt().isBefore(OffsetDateTime.now()) || !record.code().equals(otp)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid or expired OTP");
        }
        store.remove(phone + ":" + purpose);
    }

    private record OtpRecord(String code, OffsetDateTime expiresAt) {
    }
}

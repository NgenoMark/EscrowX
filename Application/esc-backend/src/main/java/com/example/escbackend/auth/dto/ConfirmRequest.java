package com.example.escbackend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConfirmRequest {

    @NotBlank
    @Email
    @Size(max = 255)
    private String email;

    @NotBlank
    @Pattern(regexp = "^\\d{6}$", message = "otp must be 6 digits")
    private String otp;
}

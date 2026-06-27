package com.example.escbackend.auth.dto;

import com.example.escbackend.common.constants.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank
    @Pattern(regexp = "^\\+2547\\d{8}$", message = "phone must match +2547XXXXXXXX")
    private String phone;

    @NotBlank
    @Email
    @Size(max = 255)
    private String email;

    @NotBlank
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,64}$",
        message = "password must be 8-64 chars with upper, lower, digit, special"
    )
    private String password;

    @Size(max = 150)
    private String displayName;

    @Size(max = 150)
    private String businessName;

    private UserRole role;
}

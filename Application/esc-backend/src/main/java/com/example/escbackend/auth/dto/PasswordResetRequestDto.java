package com.example.escbackend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordResetRequestDto {

    @NotBlank
    @Pattern(regexp = "^\\+2547\\d{8}$", message = "phone must match +2547XXXXXXXX")
    private String phone;
}

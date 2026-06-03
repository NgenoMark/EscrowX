package com.example.escbackend.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InitiateStkPushRequest {
    @NotBlank
    @Pattern(regexp = "^\\+2547\\d{8}$", message = "phoneNumber must match +2547XXXXXXXX")
    private String phoneNumber;
}

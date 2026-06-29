package com.example.escbackend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordResetRequestDto {

//    @NotBlank
//    @Pattern(regexp = "^\\+2547\\d{8}$", message = "phone must match +2547XXXXXXXX")
//    private String phone;

    @NotBlank
    @Pattern(
            regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$",
            message = "email must be a valid format like user@example.com"
    )
    private String email;

}

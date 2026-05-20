package com.example.escbackend.user.dto;

import com.example.escbackend.common.constants.UserRole;
import jakarta.validation.constraints.*;
import lombok.*;


@Getter
@Setter
public class UserRoleUpdateRequest {

    @NotNull
    private UserRole role;

    @NotBlank
    @Size(min=5,max=200)
    private String reason;

}

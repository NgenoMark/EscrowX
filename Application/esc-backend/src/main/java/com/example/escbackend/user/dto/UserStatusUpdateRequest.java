package com.example.escbackend.user.dto;


import com.example.escbackend.common.constants.UserStatus;
import jakarta.validation.constraints.*;
import lombok.*;


@Getter
@Setter
public class UserStatusUpdateRequest {

    @NotNull
    private UserStatus status;

    @NotBlank
    @Size(min=5,max=200)
    private String reason;

}

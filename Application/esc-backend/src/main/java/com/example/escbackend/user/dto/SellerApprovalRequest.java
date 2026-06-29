package com.example.escbackend.user.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SellerApprovalRequest {

    @Size(max = 200)
    private String reason;
}

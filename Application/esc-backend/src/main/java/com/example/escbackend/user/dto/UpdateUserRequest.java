package com.example.escbackend.user.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateUserRequest {

    private String email;
    private String phone;
    private String passwordHash;
    private String avatarUrl;
    private String displayName;
    private String businessName;
    private String address;
}

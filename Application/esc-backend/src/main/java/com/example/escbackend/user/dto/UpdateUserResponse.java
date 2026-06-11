package com.example.escbackend.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateUserResponse {

    private UUID id;
    private String email;
    private String phone;
    private String passwordHash;
    private String avatarUrl;
    private String displayName;
    private String businessName;
    private String address;
    private OffsetDateTime updatedAt;
}

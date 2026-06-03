package com.example.escbackend.user.dto;

import com.example.escbackend.common.constants.BlackListStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private UUID id;
    private String email;
    private String phone;
    private String role;
    private String status;
    private BlackListStatus blacklistStatus;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}

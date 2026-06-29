package com.example.escbackend.user.dto;


import com.example.escbackend.common.constants.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;


@Getter
@Builder


public class UserDetailsResponse {
        private UUID id;
        private String phone;
        private String email;
        private UserRole role;
        private UserStatus status;
        private BlackListStatus blacklistStatus;
        private String address;
        private String avatarUrl;
        private String displayName;
        private String businessName;
        private OffsetDateTime createdAt;
}

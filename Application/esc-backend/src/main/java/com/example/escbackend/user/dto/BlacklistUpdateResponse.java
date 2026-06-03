package com.example.escbackend.user.dto;

import com.example.escbackend.common.constants.BlackListStatus;
import com.example.escbackend.common.constants.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlacklistUpdateResponse {

    private UUID userId;
    private UserStatus userStatus;       // e.g., Updated to 'BLACKLISTED' or left as 'SUSPENDED'
    private BlackListStatus blacklistStatus; // The updated high-performance flag state

    // Details from the user_blacklists metadata log
    private UUID blacklistRecordId;
    private String blacklistType;
    private String reason;
    private UUID blacklistedBy;          // The Admin's User ID who executed the action
    private OffsetDateTime expiresAt;
    private OffsetDateTime updatedAt;
}
package com.example.escbackend.user.dto;

import com.example.escbackend.common.constants.BlackListStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlacklistUpdateRequest {

    @NotNull(message = "Blacklist status flag is required")
    private BlackListStatus blacklistStatus; // e.g., PERMANENTLY_BANNED, TEMPORARILY_MUTED

    @NotBlank(message = "Blacklist type configuration is required")
    private String blacklistType; // e.g., PERMANENT, TEMPORARY, INVESTIGATION

    @NotBlank(message = "A detailed reason must be provided for audit tracking")
    private String reason;

    private String evidenceSummary; // Optional summary or links to evidence logs

    private OffsetDateTime expiresAt; // Nullable (null implies a permanent/indefinite ban)
}

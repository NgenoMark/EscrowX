package com.example.escbackend.user.service;

import com.example.escbackend.user.dto.UpdateUserResponse;
import com.example.escbackend.user.dto.User;
import com.example.escbackend.user.dto.UserDetailsResponse;
import com.example.escbackend.user.entity.ProfileEntity;
import com.example.escbackend.user.entity.UserEntity;
import org.springframework.stereotype.Service;

@Service
public class UserMapperService {

    /**
     * Maps a basic UserEntity to a standard User DTO.
     */
    public User toSimple(UserEntity entity) {
        if (entity == null) {
            return null;
        }

        return User.builder()
                .id(entity.getId())
                .phone(entity.getPhone())
                .email(entity.getEmail())
                .role(entity.getRole() != null ? entity.getRole().name() : null)
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .blacklistStatus(entity.getBlacklistStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * Maps a combined UserEntity and ProfileEntity into a complete UserDetailsResponse DTO.
     * Includes null-safety checks to prevent NullPointerExceptions if a profile doesn't exist.
     */
    public UserDetailsResponse toDetails(UserEntity user, ProfileEntity profile) {
        if (user == null) {
            return null;
        }

        return UserDetailsResponse.builder()
                .id(user.getId())
                .phone(user.getPhone())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus())
                .blacklistStatus(user.getBlacklistStatus())
                .createdAt(user.getCreatedAt())
                // Safe profile mapping with null-checks
                .address(profile != null ? profile.getAddress() : null)
                .avatarUrl(profile != null ? profile.getAvatarUrl() : null)
                .displayName(profile != null ? profile.getDisplayName() : null)
                .businessName(profile != null ? profile.getBusinessName() : null)
                .build();
    }


    public UpdateUserResponse toUpdateUserResponse(UserEntity user, ProfileEntity profile){
        if (user == null){
            return null;
        }

        return UpdateUserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .phone(user.getPhone())
                .passwordHash(user.getPasswordHash())
                .address(profile != null ? profile.getAddress() : null)
                .displayName(profile != null ? profile.getDisplayName() : null)
                .businessName(profile != null ? profile.getBusinessName() : null)
                .avatarUrl(profile != null ? profile.getAvatarUrl() : null)
                .updatedAt(profile != null ? profile.getUpdatedAt() : user.getUpdatedAt())                .build();

    }
}
package com.example.escbackend.user.service;

import com.example.escbackend.user.dto.User;
import com.example.escbackend.user.dto.UserDetailsResponse;
import com.example.escbackend.user.entity.ProfileEntity;
import com.example.escbackend.user.entity.UserEntity;
import org.springframework.stereotype.Service;

@Service
public class UserMapperService {

    public User toSimple(User entity) { return entity; }

    public User toSimple(UserEntity entity) {
        return User.builder()
            .id(entity.getId())
            .phone(entity.getPhone())
            .email(entity.getEmail())
            .role(entity.getRole().name())
            .status(entity.getStatus().name())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }

    public UserDetailsResponse toDetails(UserEntity user, ProfileEntity profile) {
        return UserDetailsResponse.builder()
            .id(user.getId())
            .phone(user.getPhone())
            .email(user.getEmail())
            .role(user.getRole())
            .status(user.getStatus())
            .displayName(profile != null ? profile.getDisplayName() : null)
            .businessName(profile != null ? profile.getBusinessName() : null)
            .createdAt(user.getCreatedAt())
            .build();
    }
}

package com.example.escbackend.user.service;

import com.example.escbackend.common.constants.UserRole;
import com.example.escbackend.common.exception.ApiException;
import com.example.escbackend.user.entity.UserEntity;
import com.example.escbackend.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AdminAuthorizationService {

    private final UserRepository userRepository;

    public AdminAuthorizationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserEntity requireAdminOrSuperAdmin(UUID actorId) {
        UserEntity actor = userRepository.findById(actorId)
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Actor user not found"));

        if (actor.getRole() != UserRole.ADMIN && actor.getRole() != UserRole.SUPER_ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only ADMIN or SUPER_ADMIN can perform this action");
        }
        return actor;
    }

    public UserEntity requireSuperAdmin(UUID actorId) {
        UserEntity actor = userRepository.findById(actorId)
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Actor user not found"));

        if (actor.getRole() != UserRole.SUPER_ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only SUPER_ADMIN can perform this action");
        }
        return actor;
    }
}

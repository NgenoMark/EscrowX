package com.example.escbackend.user.repository;

import com.example.escbackend.common.constants.BlackListStatus;
import com.example.escbackend.common.constants.UserRole;
import com.example.escbackend.common.constants.UserStatus;
import com.example.escbackend.user.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByPhone(String phone);

    Optional<UserEntity> findByEmail(String email);

    boolean existsByPhone(String phone);

    boolean existsByEmail(String email);

    Page<UserEntity> findByPhoneContainingAndRoleAndStatus(String phone, UserRole role, UserStatus status, Pageable pageable);

    Page<UserEntity> findByPhoneContaining(String phone, Pageable pageable);

    Page<UserEntity> findByRole(UserRole role, Pageable pageable);

    Page<UserEntity> findByStatus(UserStatus status, Pageable pageable);

    boolean existsByEmailAndBlacklistStatusNot(String email, BlackListStatus blacklistStatus);

    boolean existsByPhoneAndBlacklistStatusNot(String phone, BlackListStatus blacklistStatus);
}

package com.example.escbackend.user.repository;

import com.example.escbackend.user.entity.UserBlacklistEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface UserBlacklistRepository extends JpaRepository<UserBlacklistEntity, UUID> {
    Optional<UserBlacklistEntity> findByUserId(UUID userId);
}
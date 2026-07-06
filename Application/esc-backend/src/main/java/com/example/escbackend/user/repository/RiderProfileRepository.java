package com.example.escbackend.user.repository;

import com.example.escbackend.user.entity.RiderProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RiderProfileRepository extends JpaRepository<RiderProfileEntity, UUID> {
}

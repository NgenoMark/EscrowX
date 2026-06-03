package com.example.escbackend.payment.repository;

import com.example.escbackend.payment.entity.EscrowLedgerEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EscrowLedgerEntryRepository extends JpaRepository<EscrowLedgerEntryEntity, UUID> {
}

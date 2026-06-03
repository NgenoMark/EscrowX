package com.example.escbackend.escrow.repository;

import com.example.escbackend.escrow.entity.EscrowTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;

import java.util.UUID;

public interface EscrowRepository extends JpaRepository<EscrowTransaction, UUID>, JpaSpecificationExecutor<EscrowTransaction> {


    List <EscrowTransaction> findBySellerId(UUID sellerId);

    List <EscrowTransaction> findByBuyerId(UUID buyerId);

}

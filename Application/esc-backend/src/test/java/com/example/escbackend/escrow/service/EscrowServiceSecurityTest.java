package com.example.escbackend.escrow.service;

import com.example.escbackend.audit.repository.AuditLogRepository;
import com.example.escbackend.common.constants.UserRole;
import com.example.escbackend.common.exception.ApiException;
import com.example.escbackend.escrow.entity.DeliveryAssignmentEntity;
import com.example.escbackend.escrow.entity.EscrowTransaction;
import com.example.escbackend.escrow.repository.DeliveryAssignmentRepository;
import com.example.escbackend.escrow.repository.EscrowRepository;
import com.example.escbackend.user.entity.UserEntity;
import com.example.escbackend.user.repository.UserRepository;
import com.example.escbackend.user.service.AdminAuthorizationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EscrowServiceSecurityTest {

    @Mock
    private EscrowRepository escrowRepository;

    @Mock
    private DeliveryAssignmentRepository deliveryAssignmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private TransactionStatusHistoryService transactionStatusHistoryService;

    @Mock
    private AdminAuthorizationService adminAuthorizationService;

    @InjectMocks
    private EscrowService escrowService;

    @Test
    void riderAcceptDelivery_rejectsNonRiderActor() {
        UUID transactionId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        UUID buyerId = UUID.randomUUID();
        UUID assignedRiderId = UUID.randomUUID();

        EscrowTransaction tx = buildTransaction(transactionId, buyerId, sellerId, assignedRiderId, "SELLER_ACCEPTED");

        UserEntity sellerActor = new UserEntity();
        sellerActor.setId(assignedRiderId);
        sellerActor.setRole(UserRole.SELLER);

        when(escrowRepository.findById(transactionId)).thenReturn(Optional.of(tx));
        when(userRepository.findById(assignedRiderId)).thenReturn(Optional.of(sellerActor));

        assertThatThrownBy(() -> escrowService.riderAcceptDelivery(transactionId, assignedRiderId))
            .isInstanceOf(ApiException.class)
            .extracting(ex -> ((ApiException) ex).getStatus())
            .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void getDeliveryAssignmentHistory_rejectsUnauthorizedViewer() {
        UUID transactionId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        UUID buyerId = UUID.randomUUID();
        UUID riderId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();

        EscrowTransaction tx = buildTransaction(transactionId, buyerId, sellerId, riderId, "IN_DELIVERY");

        UserEntity stranger = new UserEntity();
        stranger.setId(strangerId);
        stranger.setRole(UserRole.BUYER);

        when(escrowRepository.findById(transactionId)).thenReturn(Optional.of(tx));
        when(userRepository.findById(strangerId)).thenReturn(Optional.of(stranger));

        assertThatThrownBy(() -> escrowService.getDeliveryAssignmentHistory(transactionId, strangerId))
            .isInstanceOf(ApiException.class)
            .extracting(ex -> ((ApiException) ex).getStatus())
            .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void getDeliveryAssignmentHistory_allowsAssignedRider() {
        UUID transactionId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        UUID buyerId = UUID.randomUUID();
        UUID riderId = UUID.randomUUID();

        EscrowTransaction tx = buildTransaction(transactionId, buyerId, sellerId, riderId, "IN_DELIVERY");

        DeliveryAssignmentEntity assignment = new DeliveryAssignmentEntity();
        assignment.setId(UUID.randomUUID());
        assignment.setTransactionId(transactionId);
        assignment.setRiderUserId(riderId);
        assignment.setStatus("IN_TRANSIT");
        assignment.setCreatedAt(OffsetDateTime.now());
        assignment.setUpdatedAt(OffsetDateTime.now());

        when(escrowRepository.findById(transactionId)).thenReturn(Optional.of(tx));
        when(deliveryAssignmentRepository.findByTransactionIdOrderByCreatedAtDesc(transactionId))
            .thenReturn(java.util.List.of(assignment));
        when(deliveryAssignmentRepository.findTopByTransactionIdAndStatusInOrderByCreatedAtDesc(
            org.mockito.ArgumentMatchers.eq(transactionId),
            org.mockito.ArgumentMatchers.anyCollection()
        )).thenReturn(Optional.of(assignment));

        var response = escrowService.getDeliveryAssignmentHistory(transactionId, riderId);

        assertThat(response.getTransactionId()).isEqualTo(transactionId);
        assertThat(response.getCurrentActiveAssignmentId()).isEqualTo(assignment.getId());
        assertThat(response.getAssignments()).hasSize(1);
    }

    private EscrowTransaction buildTransaction(UUID transactionId, UUID buyerId, UUID sellerId, UUID riderId, String status) {
        EscrowTransaction tx = new EscrowTransaction();
        tx.setId(transactionId);
        tx.setReference("ESC-REF");
        tx.setTitle("Phone");
        tx.setProductDescription("Device");
        tx.setAmount(BigDecimal.valueOf(100));
        tx.setDeliveryAddress("Nairobi");
        tx.setInitialDepositAmount(BigDecimal.valueOf(100));
        tx.setCurrency("KES");
        tx.setStatus(status);
        tx.setCreatedAt(OffsetDateTime.now().minusDays(1));
        tx.setUpdatedAt(OffsetDateTime.now());

        UserEntity buyer = new UserEntity();
        buyer.setId(buyerId);
        buyer.setRole(UserRole.BUYER);
        tx.setBuyer(buyer);

        UserEntity seller = new UserEntity();
        seller.setId(sellerId);
        seller.setRole(UserRole.SELLER);
        tx.setSeller(seller);

        UserEntity rider = new UserEntity();
        rider.setId(riderId);
        rider.setRole(UserRole.RIDER);
        tx.setRider(rider);

        return tx;
    }
}

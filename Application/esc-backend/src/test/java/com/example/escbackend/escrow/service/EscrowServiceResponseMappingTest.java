package com.example.escbackend.escrow.service;

import com.example.escbackend.escrow.dto.EscrowResponse;
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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EscrowServiceResponseMappingTest {

    @Mock
    private EscrowRepository escrowRepository;

    @Mock
    private DeliveryAssignmentRepository deliveryAssignmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionStatusHistoryService transactionStatusHistoryService;

    @Mock
    private AdminAuthorizationService adminAuthorizationService;

    @InjectMocks
    private EscrowService escrowService;

    @Test
    void getById_includesLatestDeliveryAssignmentFieldsInResponse() {
        UUID transactionId = UUID.randomUUID();
        UUID buyerId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        UUID riderId = UUID.randomUUID();

        EscrowTransaction tx = new EscrowTransaction();
        tx.setId(transactionId);
        tx.setReference("ESC-REF-1");
        tx.setTitle("Phone");
        tx.setProductDescription("Device");
        tx.setAmount(BigDecimal.valueOf(1200));
        tx.setDeliveryAddress("Nairobi");
        tx.setInitialDepositAmount(BigDecimal.valueOf(1200));
        tx.setCurrency("KES");
        tx.setStatus("IN_DELIVERY");
        tx.setCreatedAt(OffsetDateTime.now().minusDays(1));
        tx.setUpdatedAt(OffsetDateTime.now());

        UserEntity buyer = new UserEntity();
        buyer.setId(buyerId);
        tx.setBuyer(buyer);

        UserEntity seller = new UserEntity();
        seller.setId(sellerId);
        tx.setSeller(seller);

        UserEntity rider = new UserEntity();
        rider.setId(riderId);
        tx.setRider(rider);

        DeliveryAssignmentEntity assignment = new DeliveryAssignmentEntity();
        assignment.setTransactionId(transactionId);
        assignment.setRiderUserId(riderId);
        assignment.setStatus("IN_TRANSIT");
        assignment.setPickedUpAt(OffsetDateTime.now().minusHours(4));
        assignment.setArrivedAtBuyerAt(OffsetDateTime.now().minusHours(2));

        when(escrowRepository.findById(transactionId)).thenReturn(Optional.of(tx));
        when(deliveryAssignmentRepository.findTopByTransactionIdOrderByCreatedAtDesc(transactionId))
            .thenReturn(Optional.of(assignment));

        EscrowResponse response = escrowService.getById(transactionId);

        assertThat(response.getId()).isEqualTo(transactionId);
        assertThat(response.getBuyerId()).isEqualTo(buyerId);
        assertThat(response.getSellerId()).isEqualTo(sellerId);
        assertThat(response.getRiderId()).isEqualTo(riderId);
        assertThat(response.getRiderAssignmentStatus()).isEqualTo("IN_TRANSIT");
        assertThat(response.getRiderPickedUpAt()).isEqualTo(assignment.getPickedUpAt());
        assertThat(response.getRiderArrivedAtBuyerAt()).isEqualTo(assignment.getArrivedAtBuyerAt());
    }
}

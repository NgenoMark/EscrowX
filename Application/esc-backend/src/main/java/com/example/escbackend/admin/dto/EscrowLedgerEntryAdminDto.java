package com.example.escbackend.admin.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class EscrowLedgerEntryAdminDto {
        private UUID id;
        private UUID transactionId;
        private String entryType;
        private String direction;
        private BigDecimal amount;
        private String currency;
        private UUID referenceId;
        private String referenceType;
        private OffsetDateTime createdAt;

        public EscrowLedgerEntryAdminDto() {
        }

        public EscrowLedgerEntryAdminDto(
                        UUID id,
                        UUID transactionId,
                        String entryType,
                        String direction,
                        BigDecimal amount,
                        String currency,
                        UUID referenceId,
                        String referenceType,
                        OffsetDateTime createdAt
        ) {
                this.id = id;
                this.transactionId = transactionId;
                this.entryType = entryType;
                this.direction = direction;
                this.amount = amount;
                this.currency = currency;
                this.referenceId = referenceId;
                this.referenceType = referenceType;
                this.createdAt = createdAt;
        }

        public UUID getId() {
                return id;
        }

        public void setId(UUID id) {
                this.id = id;
        }

        public UUID getTransactionId() {
                return transactionId;
        }

        public void setTransactionId(UUID transactionId) {
                this.transactionId = transactionId;
        }

        public String getEntryType() {
                return entryType;
        }

        public void setEntryType(String entryType) {
                this.entryType = entryType;
        }

        public String getDirection() {
                return direction;
        }

        public void setDirection(String direction) {
                this.direction = direction;
        }

        public BigDecimal getAmount() {
                return amount;
        }

        public void setAmount(BigDecimal amount) {
                this.amount = amount;
        }

        public String getCurrency() {
                return currency;
        }

        public void setCurrency(String currency) {
                this.currency = currency;
        }

        public UUID getReferenceId() {
                return referenceId;
        }

        public void setReferenceId(UUID referenceId) {
                this.referenceId = referenceId;
        }

        public String getReferenceType() {
                return referenceType;
        }

        public void setReferenceType(String referenceType) {
                this.referenceType = referenceType;
        }

        public OffsetDateTime getCreatedAt() {
                return createdAt;
        }

        public void setCreatedAt(OffsetDateTime createdAt) {
                this.createdAt = createdAt;
        }
}

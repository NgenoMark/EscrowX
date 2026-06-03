package com.example.escbackend.payment.service;

import com.example.escbackend.escrow.entity.EscrowTransaction;
import com.example.escbackend.payment.entity.EscrowLedgerEntryEntity;
import com.example.escbackend.payment.repository.EscrowLedgerEntryRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class LedgerService {
    private final EscrowLedgerEntryRepository ledgerRepository;

    public LedgerService(EscrowLedgerEntryRepository ledgerRepository) {
        this.ledgerRepository = ledgerRepository;
    }

    public void recordHold(EscrowTransaction transaction, BigDecimal amount, UUID paymentId) {
        record(transaction, "HOLD", "CREDIT", amount, paymentId, "PAYMENT");
    }

    public void recordRelease(EscrowTransaction transaction, BigDecimal amount, UUID payoutId) {
        record(transaction, "RELEASE", "DEBIT", amount, payoutId, "PAYOUT");
    }

    private void record(
        EscrowTransaction transaction,
        String entryType,
        String direction,
        BigDecimal amount,
        UUID referenceId,
        String referenceType
    ) {
        EscrowLedgerEntryEntity entry = new EscrowLedgerEntryEntity();
        entry.setTransaction(transaction);
        entry.setEntryType(entryType);
        entry.setDirection(direction);
        entry.setAmount(amount);
        entry.setCurrency(transaction.getCurrency());
        entry.setReferenceId(referenceId);
        entry.setReferenceType(referenceType);
        ledgerRepository.save(entry);
    }
}

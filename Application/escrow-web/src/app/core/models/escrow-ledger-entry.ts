// src/app/core/models/escrow-ledger-entry.ts
export interface EscrowLedgerEntry {
  id: string;
  transactionId: string;
  entryType: 'HOLD' | 'RELEASE' | 'REFUND' | 'FEE';
  direction: 'CREDIT' | 'DEBIT';
  amount: number;
  currency: string;
  referenceId: string | null;
  referenceType: string | null;
  createdAt: string;
}
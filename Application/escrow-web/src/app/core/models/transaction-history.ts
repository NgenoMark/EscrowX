export interface TransactionStatusHistory {
  id: string;
  transactionId: string;
  fromStatus: string | null;
  toStatus: string;
  changedBy: string; // user ID
  reason: string | null;
  createdAt: string;
}
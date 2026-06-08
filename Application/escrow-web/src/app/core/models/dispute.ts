export type DisputeStatus =
  | 'OPEN'
  | 'PENDING'
  | 'UNDER_REVIEW'
  | 'ACTION_REQUIRED'
  | 'RESOLVED'
  | 'REJECTED'
  | 'ESCALATED';

export interface Dispute {
  id: string;
  txId: string;
  transactionId?: string;
  raisedById?: string;
  raisedBy: string;
  raisedByRole: 'BUYER' | 'SELLER';
  against: string;
  assignedAdminId?: string;
  category?: 'NON_DELIVERY' | 'DEFECTIVE_ITEM' | 'NOT_AS_DESCRIBED' | 'NON_PAYMENT' | 'OTHER';
  reason: string;
  description?: string;
  evidence: string[];
  status: DisputeStatus;
  amount: number;
  createdAt: string;
  updatedAt?: string;
  resolvedAt?: string;
  resolution?: 'REFUND_BUYER' | 'RELEASE_SELLER' | 'PARTIAL_SETTLEMENT';
  partialAmount?: number;
  adminNotes?: string;
}

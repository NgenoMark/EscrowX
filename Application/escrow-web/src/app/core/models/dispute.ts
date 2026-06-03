export interface Dispute {
  id: string;
  txId: string;
  raisedBy: string;
  raisedByRole: 'BUYER' | 'SELLER';
  against: string;
  reason: string;
  description?: string;
  evidence: string[];
  status: 'PENDING' | 'UNDER_REVIEW' | 'RESOLVED' | 'ESCALATED';
  amount: number;
  createdAt: string;
  resolvedAt?: string;
  resolution?: 'REFUND_BUYER' | 'RELEASE_SELLER' | 'PARTIAL_SETTLEMENT';
  partialAmount?: number;
  adminNotes?: string;
}
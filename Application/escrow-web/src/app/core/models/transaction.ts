export interface Transaction {
  id: string;
  buyer: string;
  buyerId?: number;
  seller: string;
  sellerId?: number;
  amount: number;
  status: 'FUNDS_HELD' | 'COMPLETED' | 'DISPUTED' | 'REFUNDED' | 'CANCELLED';
  created: string;
  completedAt?: string;
  autoReleaseDate?: string;
  description?: string;
  deliveryTimeline?: string;
  disputeId?: string;
}
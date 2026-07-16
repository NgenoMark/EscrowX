export type EscrowTransactionStatus =
  | 'CREATED'
  | 'PENDING_PAYMENT'
  | 'FUNDS_HELD'
  | 'SELLER_ACCEPTED'
  | 'IN_DELIVERY'
  | 'SELLER_DELIVERED'
  | 'BUYER_CONFIRMED_DELIVERED'
  | 'RELEASE_PENDING'
  | 'RELEASE_PROCESSING'
  | 'RELEASE_FAILED'
  | 'COMPLETED'
  | 'DISPUTED'
  | 'REFUND_PENDING'
  | 'REFUND_PROCESSING'
  | 'REFUNDED'
  | 'CANCELLED'
  | 'EXPIRED';

export interface Transaction {
  id: string;
  reference?: string;
  buyer: string;
  buyerId?: string;
  seller: string;
  sellerId?: string;
  title?: string;
  productDescription?: string;
  amount: number;
  initialDepositAmount?: number;
  currency?: string;
  status: EscrowTransactionStatus;
  created: string;
  createdAt?: string;
  updatedAt?: string;
  completedAt?: string;
  deliveryDueAt?: string;
  autoReleaseDate?: string;
  autoReleaseAt?: string;
  description?: string;
  deliveryTimeline?: string;
  disputeId?: string;
  riderId?: string;
  riderName?: string;
}

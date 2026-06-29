export interface PaymentIntent {
  id: string;
  transactionId: string;
  buyerId: string;
  sellerId: string;
  provider: 'MPESA';
  providerRef: string;
  amount: number;
  currency: string;
  paymentMethod: string;
  phoneNumber: string;
  status: 'INITIATED' | 'PENDING' | 'PAID' | 'FAILED' | 'CANCELLED';
  checkoutRequestId: string;
  merchantRequestId: string;
  mpesaReceiptNumber: string;
  providerResponseCode: string;
  providerResponseDescription: string;
  paidAt: string | null;
  createdAt: string;
  updatedAt: string;
}
import { Injectable } from '@angular/core';
import { of } from 'rxjs';
import { Dispute } from '../models/dispute';
import { Transaction } from '../models/transaction';
import { User } from '../models/user';
import type { AuditLog } from './data';

@Injectable({
  providedIn: 'root'
})
export class MockDataService {
  getTransactions() {
    return of<Transaction[]>([
      { id: '2c9d1c0a-0001-4000-9000-000000000001', reference: 'ESC-TX1001', buyer: 'John Kamau', buyerId: '11111111-1111-4111-8111-111111111111', seller: 'Mercy Achieng', sellerId: '22222222-2222-4222-8222-222222222222', title: 'Laptop purchase', productDescription: 'Used business laptop', amount: 12500, initialDepositAmount: 12500, currency: 'KES', status: 'FUNDS_HELD', created: '2025-05-28', createdAt: '2025-05-28T10:00:00Z', autoReleaseDate: '2025-06-04', autoReleaseAt: '2025-06-04T10:00:00Z' },
      { id: '2c9d1c0a-0002-4000-9000-000000000002', reference: 'ESC-TX1002', buyer: 'Zahra Hassan', buyerId: '33333333-3333-4333-8333-333333333333', seller: 'Samuel Njoroge', sellerId: '44444444-4444-4444-8444-444444444444', title: 'Phone sale', productDescription: 'Smartphone', amount: 4500, initialDepositAmount: 4500, currency: 'KES', status: 'COMPLETED', created: '2025-05-27', createdAt: '2025-05-27T09:30:00Z' },
      { id: '2c9d1c0a-0003-4000-9000-000000000003', reference: 'ESC-TX1003', buyer: 'Peter Omondi', buyerId: '55555555-5555-4555-8555-555555555555', seller: 'Faith Wanjiku', sellerId: '66666666-6666-4666-8666-666666666666', title: 'Laptop order', productDescription: 'Laptop with accessories', amount: 23000, initialDepositAmount: 23000, currency: 'KES', status: 'DISPUTED', created: '2025-05-26', createdAt: '2025-05-26T14:15:00Z', disputeId: '7d9e1d0a-0101-4000-9000-000000000101' },
      { id: '2c9d1c0a-0004-4000-9000-000000000004', reference: 'ESC-TX1004', buyer: 'Grace Adhiambo', buyerId: '77777777-7777-4777-8777-777777777777', seller: 'James Mwangi', sellerId: '88888888-8888-4888-8888-888888888888', title: 'Design work', productDescription: 'Logo package', amount: 6750, initialDepositAmount: 6750, currency: 'KES', status: 'FUNDS_HELD', created: '2025-05-29', createdAt: '2025-05-29T08:20:00Z', autoReleaseDate: '2025-06-05', autoReleaseAt: '2025-06-05T08:20:00Z' },
      { id: '2c9d1c0a-0005-4000-9000-000000000005', reference: 'ESC-TX1005', buyer: 'John Kamau', buyerId: '11111111-1111-4111-8111-111111111111', seller: 'Sharon Atieno', sellerId: '99999999-9999-4999-8999-999999999999', title: 'Inventory payment', productDescription: 'Shop inventory', amount: 18800, initialDepositAmount: 18800, currency: 'KES', status: 'COMPLETED', created: '2025-05-25', createdAt: '2025-05-25T11:45:00Z' },
      { id: '2c9d1c0a-0006-4000-9000-000000000006', reference: 'ESC-TX1006', buyer: 'Brian Odhiambo', buyerId: 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa', seller: 'Peter Omondi', sellerId: '55555555-5555-4555-8555-555555555555', title: 'Phone purchase', productDescription: 'Second-hand phone', amount: 9200, initialDepositAmount: 9200, currency: 'KES', status: 'DISPUTED', created: '2025-05-24', createdAt: '2025-05-24T16:10:00Z', disputeId: '7d9e1d0a-0102-4000-9000-000000000102' },
      { id: '2c9d1c0a-0007-4000-9000-000000000007', reference: 'ESC-TX1007', buyer: 'Linet Wambui', buyerId: 'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb', seller: 'Samuel Njoroge', sellerId: '44444444-4444-4444-8444-444444444444', title: 'Wholesale order', productDescription: 'Bulk electronics', amount: 33000, initialDepositAmount: 33000, currency: 'KES', status: 'FUNDS_HELD', created: '2025-05-30', createdAt: '2025-05-30T12:00:00Z', autoReleaseDate: '2025-06-06', autoReleaseAt: '2025-06-06T12:00:00Z' }
    ]);
  }

  getUsers() {
    return of<User[]>([
      { id: '11111111-1111-4111-8111-111111111111', phone: '+254712345678', email: 'john@example.com', role: 'BUYER', status: 'ACTIVE', blacklistStatus: 'NOT_BLACKLISTED', displayName: 'John Kamau', businessName: null, createdAt: '2025-01-15T10:00:00Z' },
      { id: '22222222-2222-4222-8222-222222222222', phone: '+254722987654', email: 'mercy@seller.com', role: 'SELLER', status: 'ACTIVE', blacklistStatus: 'NOT_BLACKLISTED', displayName: 'Mercy Achieng', businessName: 'Mercy Traders', createdAt: '2025-02-10T09:30:00Z' },
      { id: '55555555-5555-4555-8555-555555555555', phone: '+254799887766', email: 'peter@fraud.com', role: 'SELLER', status: 'BLACKLISTED', blacklistStatus: 'PERMANENTLY_BANNED', displayName: 'Peter Omondi', businessName: 'Peter Supplies', createdAt: '2025-01-20T14:15:00Z' },
      { id: '33333333-3333-4333-8333-333333333333', phone: '+254700112233', email: 'zahra@buyer.com', role: 'BUYER', status: 'PENDING_VERIFICATION', blacklistStatus: 'NOT_BLACKLISTED', displayName: 'Zahra Hassan', businessName: null, createdAt: '2025-03-05T11:45:00Z' },
      { id: '44444444-4444-4444-8444-444444444444', phone: '+254744556677', email: 'samuel@shop.com', role: 'SELLER', status: 'ACTIVE', blacklistStatus: 'NOT_BLACKLISTED', displayName: 'Samuel Njoroge', businessName: 'Samuel Shop', createdAt: '2025-02-20T08:20:00Z' },
      { id: '77777777-7777-4777-8777-777777777777', phone: '+254711223344', email: 'grace@designs.com', role: 'SELLER', status: 'ACTIVE', blacklistStatus: 'UNDER_INVESTIGATION', displayName: 'Grace Adhiambo', businessName: 'Grace Designs', createdAt: '2025-03-15T16:10:00Z' }
    ]);
  }

  getDisputes() {
    return of<Dispute[]>([
      { id: '7d9e1d0a-0101-4000-9000-000000000101', txId: '2c9d1c0a-0003-4000-9000-000000000003', transactionId: '2c9d1c0a-0003-4000-9000-000000000003', raisedById: '55555555-5555-4555-8555-555555555555', raisedBy: 'Peter Omondi', raisedByRole: 'BUYER', against: 'Faith Wanjiku', category: 'NON_DELIVERY', reason: 'Item never delivered after payment', description: 'I paid KES 23,000 for a laptop on May 20th.', evidence: ['screenshot_wa.png', 'payment_receipt.jpg'], status: 'PENDING', amount: 23000, createdAt: '2025-05-26' },
      { id: '7d9e1d0a-0102-4000-9000-000000000102', txId: '2c9d1c0a-0006-4000-9000-000000000006', transactionId: '2c9d1c0a-0006-4000-9000-000000000006', raisedById: 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa', raisedBy: 'Brian Odhiambo', raisedByRole: 'BUYER', against: 'Peter Omondi', category: 'NOT_AS_DESCRIBED', reason: 'Received counterfeit phone', description: 'The phone I received is clearly counterfeit.', evidence: ['counterfeit_photo.jpg'], status: 'PENDING', amount: 9200, createdAt: '2025-05-24' },
      { id: '7d9e1d0a-0103-4000-9000-000000000103', txId: '2c9d1c0a-0001-4000-9000-000000000001', transactionId: '2c9d1c0a-0001-4000-9000-000000000001', raisedById: '22222222-2222-4222-8222-222222222222', raisedBy: 'Mercy Achieng', raisedByRole: 'SELLER', against: 'John Kamau', category: 'OTHER', reason: 'Buyer confirmed then disputed', description: 'The buyer confirmed receipt then opened a dispute.', evidence: ['delivery_proof.png'], status: 'UNDER_REVIEW', amount: 12500, createdAt: '2025-05-28' }
    ]);
  }

  getAuditLogs() {
    return of<AuditLog[]>([
      { timestamp: new Date().toISOString().replace('T', ' ').substring(0, 19), admin: 'admin@escrowx.com', action: 'SUSPEND_USER', target: 'Peter Omondi', details: 'Suspended user Peter Omondi' },
      { timestamp: new Date(Date.now() - 86400000).toISOString().replace('T', ' ').substring(0, 19), admin: 'admin@escrowx.com', action: 'ACTIVATE_USER', target: 'Mercy Achieng', details: 'Activated user Mercy Achieng' },
      { timestamp: new Date(Date.now() - 172800000).toISOString().replace('T', ' ').substring(0, 19), admin: 'admin@escrowx.com', action: 'BLACKLIST_USER', target: 'Peter Omondi', details: 'Blacklisted user Peter Omondi' },
      { timestamp: new Date(Date.now() - 259200000).toISOString().replace('T', ' ').substring(0, 19), admin: 'admin@escrowx.com', action: 'FORCE_RELEASE', target: 'ESC-TX1005', details: 'Released KES 18,800 to seller' },
      { timestamp: new Date(Date.now() - 345600000).toISOString().replace('T', ' ').substring(0, 19), admin: 'admin@escrowx.com', action: 'RESOLVE_DISPUTE', target: 'DSP-101', details: 'Resolved: Refunded KES 23,000 to buyer' }
    ]);
  }
}

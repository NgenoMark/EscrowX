// src/app/core/services/mock-data.service.ts
import { Injectable } from '@angular/core';
import { of, Observable, throwError } from 'rxjs';
import { Dispute } from '../models/dispute';
import { Transaction } from '../models/transaction';
import { User } from '../models/user';
import { Rider, RiderProfile, RiderWithStats, DeliveryAssignmentStatus } from '../models/rider';
import type { AuditLog } from './data';
import { ApiEscrowTransaction, ApiUserDetails, PageResponse } from './api.service';

@Injectable({
  providedIn: 'root'
})
export class MockDataService {
  // Store assigned riders per transaction with delivery assignment details
  private assignedRiders = new Map<string, { 
    riderId: string; 
    riderName: string; 
    assignedAt: string;
    assignmentId: string;
    status: DeliveryAssignmentStatus;
  }>();

  // Store transactions with rider assignments
  private transactions: Transaction[] = [
    { 
      id: '2c9d1c0a-0001-4000-9000-000000000001', 
      reference: 'ESC-TX1001', 
      buyer: 'John Kamau', 
      buyerId: '11111111-1111-4111-8111-111111111111', 
      seller: 'Mercy Achieng', 
      sellerId: '22222222-2222-4222-8222-222222222222', 
      title: 'Laptop purchase', 
      productDescription: 'Used business laptop', 
      amount: 12500, 
      initialDepositAmount: 12500, 
      currency: 'KES', 
      status: 'FUNDS_HELD', 
      created: '2025-05-28', 
      createdAt: '2025-05-28T10:00:00Z', 
      autoReleaseDate: '2025-06-04', 
      autoReleaseAt: '2025-06-04T10:00:00Z',
      riderId: '99999999-9999-4999-8999-999999999999',
      riderName: 'Sharon Atieno'
    },
    { 
      id: '2c9d1c0a-0002-4000-9000-000000000002', 
      reference: 'ESC-TX1002', 
      buyer: 'Zahra Hassan', 
      buyerId: '33333333-3333-4333-8333-333333333333', 
      seller: 'Samuel Njoroge', 
      sellerId: '44444444-4444-4444-8444-444444444444', 
      title: 'Phone sale', 
      productDescription: 'Smartphone', 
      amount: 4500, 
      initialDepositAmount: 4500, 
      currency: 'KES', 
      status: 'COMPLETED', 
      created: '2025-05-27', 
      createdAt: '2025-05-27T09:30:00Z',
      riderId: 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa',
      riderName: 'David Ochieng'
    },
    { 
      id: '2c9d1c0a-0003-4000-9000-000000000003', 
      reference: 'ESC-TX1003', 
      buyer: 'Peter Omondi', 
      buyerId: '55555555-5555-4555-8555-555555555555', 
      seller: 'Faith Wanjiku', 
      sellerId: '66666666-6666-4666-8666-666666666666', 
      title: 'Laptop order', 
      productDescription: 'Laptop with accessories', 
      amount: 23000, 
      initialDepositAmount: 23000, 
      currency: 'KES', 
      status: 'DISPUTED', 
      created: '2025-05-26', 
      createdAt: '2025-05-26T14:15:00Z', 
      disputeId: '7d9e1d0a-0101-4000-9000-000000000101' 
    },
    { 
      id: '2c9d1c0a-0004-4000-9000-000000000004', 
      reference: 'ESC-TX1004', 
      buyer: 'Grace Adhiambo', 
      buyerId: '77777777-7777-4777-8777-777777777777', 
      seller: 'James Mwangi', 
      sellerId: '88888888-8888-4888-8888-888888888888', 
      title: 'Design work', 
      productDescription: 'Logo package', 
      amount: 6750, 
      initialDepositAmount: 6750, 
      currency: 'KES', 
      status: 'FUNDS_HELD', 
      created: '2025-05-29', 
      createdAt: '2025-05-29T08:20:00Z', 
      autoReleaseDate: '2025-06-05', 
      autoReleaseAt: '2025-06-05T08:20:00Z' 
    },
    { 
      id: '2c9d1c0a-0005-4000-9000-000000000005', 
      reference: 'ESC-TX1005', 
      buyer: 'John Kamau', 
      buyerId: '11111111-1111-4111-8111-111111111111', 
      seller: 'Sharon Atieno', 
      sellerId: '99999999-9999-4999-8999-999999999999', 
      title: 'Inventory payment', 
      productDescription: 'Shop inventory', 
      amount: 18800, 
      initialDepositAmount: 18800, 
      currency: 'KES', 
      status: 'COMPLETED', 
      created: '2025-05-25', 
      createdAt: '2025-05-25T11:45:00Z' 
    },
    { 
      id: '2c9d1c0a-0006-4000-9000-000000000006', 
      reference: 'ESC-TX1006', 
      buyer: 'Brian Odhiambo', 
      buyerId: 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa', 
      seller: 'Peter Omondi', 
      sellerId: '55555555-5555-4555-8555-555555555555', 
      title: 'Phone purchase', 
      productDescription: 'Second-hand phone', 
      amount: 9200, 
      initialDepositAmount: 9200, 
      currency: 'KES', 
      status: 'DISPUTED', 
      created: '2025-05-24', 
      createdAt: '2025-05-24T16:10:00Z', 
      disputeId: '7d9e1d0a-0102-4000-9000-000000000102' 
    },
    { 
      id: '2c9d1c0a-0007-4000-9000-000000000007', 
      reference: 'ESC-TX1007', 
      buyer: 'Linet Wambui', 
      buyerId: 'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb', 
      seller: 'Samuel Njoroge', 
      sellerId: '44444444-4444-4444-8444-444444444444', 
      title: 'Wholesale order', 
      productDescription: 'Bulk electronics', 
      amount: 33000, 
      initialDepositAmount: 33000, 
      currency: 'KES', 
      status: 'FUNDS_HELD', 
      created: '2025-05-30', 
      createdAt: '2025-05-30T12:00:00Z', 
      autoReleaseDate: '2025-06-06', 
      autoReleaseAt: '2025-06-06T12:00:00Z' 
    }
  ];

  // Store users (including riders)
  private users: (User | Rider)[] = [
    { 
      id: '11111111-1111-4111-8111-111111111111', 
      phone: '+254712345678', 
      email: 'john@example.com', 
      role: 'BUYER', 
      status: 'ACTIVE', 
      blacklistStatus: 'NOT_BLACKLISTED', 
      displayName: 'John Kamau', 
      businessName: null, 
      createdAt: '2025-01-15T10:00:00Z' 
    },
    { 
      id: '22222222-2222-4222-8222-222222222222', 
      phone: '+254722987654', 
      email: 'mercy@seller.com', 
      role: 'SELLER', 
      status: 'ACTIVE', 
      blacklistStatus: 'NOT_BLACKLISTED', 
      displayName: 'Mercy Achieng', 
      businessName: 'Mercy Traders', 
      createdAt: '2025-02-10T09:30:00Z' 
    },
    { 
      id: '55555555-5555-4555-8555-555555555555', 
      phone: '+254799887766', 
      email: 'peter@fraud.com', 
      role: 'SELLER', 
      status: 'BLACKLISTED', 
      blacklistStatus: 'PERMANENTLY_BANNED', 
      displayName: 'Peter Omondi', 
      businessName: 'Peter Supplies', 
      createdAt: '2025-01-20T14:15:00Z' 
    },
    { 
      id: '33333333-3333-4333-8333-333333333333', 
      phone: '+254700112233', 
      email: 'zahra@buyer.com', 
      role: 'BUYER', 
      status: 'PENDING_VERIFICATION', 
      blacklistStatus: 'NOT_BLACKLISTED', 
      displayName: 'Zahra Hassan', 
      businessName: null, 
      createdAt: '2025-03-05T11:45:00Z' 
    },
    { 
      id: '44444444-4444-4444-8444-444444444444', 
      phone: '+254744556677', 
      email: 'samuel@shop.com', 
      role: 'SELLER', 
      status: 'ACTIVE', 
      blacklistStatus: 'NOT_BLACKLISTED', 
      displayName: 'Samuel Njoroge', 
      businessName: 'Samuel Shop', 
      createdAt: '2025-02-20T08:20:00Z' 
    },
    { 
      id: '77777777-7777-4777-8777-777777777777', 
      phone: '+254711223344', 
      email: 'grace@designs.com', 
      role: 'SELLER', 
      status: 'ACTIVE', 
      blacklistStatus: 'UNDER_INVESTIGATION', 
      displayName: 'Grace Adhiambo', 
      businessName: 'Grace Designs', 
      createdAt: '2025-03-15T16:10:00Z' 
    },
    // ===== RIDERS with backend-aligned fields =====
    { 
      id: '99999999-9999-4999-8999-999999999999', 
      phone: '+254712345678', 
      email: 'sharon@rider.com', 
      role: 'RIDER', 
      status: 'ACTIVE', 
      blacklistStatus: 'NOT_BLACKLISTED', 
      displayName: 'Sharon Atieno', 
      businessName: null, 
      createdAt: '2025-04-01T08:00:00Z',
      updatedAt: '2025-06-01T10:00:00Z',
      vehicleType: 'Motorcycle',
      vehiclePlate: 'KCA 123A',
      rating: 4.8,
      totalDeliveries: 45,
      isActive: true,
      profileImage: '',
      riderStatus: 'AVAILABLE',
      operationArea: 'Nairobi CBD',
      licenseNumber: 'DL12345678'
    } as Rider,
    { 
      id: 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa', 
      phone: '+254798765432', 
      email: 'david@rider.com', 
      role: 'RIDER', 
      status: 'ACTIVE', 
      blacklistStatus: 'NOT_BLACKLISTED', 
      displayName: 'David Ochieng', 
      businessName: null, 
      createdAt: '2025-04-15T09:00:00Z',
      updatedAt: '2025-06-02T11:00:00Z',
      vehicleType: 'Motorcycle',
      vehiclePlate: 'KCB 456B',
      rating: 4.6,
      totalDeliveries: 32,
      isActive: true,
      profileImage: '',
      riderStatus: 'BUSY',
      operationArea: 'Westlands',
      licenseNumber: 'DL23456789'
    } as Rider,
    { 
      id: 'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb', 
      phone: '+254723456789', 
      email: 'mary@rider.com', 
      role: 'RIDER', 
      status: 'ACTIVE', 
      blacklistStatus: 'NOT_BLACKLISTED', 
      displayName: 'Mary Wanjiru', 
      businessName: null, 
      createdAt: '2025-05-01T07:30:00Z',
      updatedAt: '2025-05-30T14:00:00Z',
      vehicleType: 'Bicycle',
      vehiclePlate: 'KCC 789C',
      rating: 4.9,
      totalDeliveries: 18,
      isActive: true,
      profileImage: '',
      riderStatus: 'AVAILABLE',
      operationArea: 'Kilimani',
      licenseNumber: 'DL34567890'
    } as Rider,
    { 
      id: 'cccccccc-cccc-4ccc-8ccc-cccccccccccc', 
      phone: '+254734567890', 
      email: 'james@rider.com', 
      role: 'RIDER', 
      status: 'SUSPENDED', 
      blacklistStatus: 'TEMPORARILY_MUTED', 
      displayName: 'James Mwangi', 
      businessName: null, 
      createdAt: '2025-05-10T10:00:00Z',
      updatedAt: '2025-05-25T08:00:00Z',
      vehicleType: 'Motorcycle',
      vehiclePlate: 'KCD 321D',
      rating: 3.2,
      totalDeliveries: 12,
      isActive: false,
      profileImage: '',
      riderStatus: 'SUSPENDED',
      operationArea: 'Eastlands',
      licenseNumber: 'DL45678901'
    } as Rider,
    { 
      id: 'dddddddd-dddd-4ddd-8ddd-dddddddddddd', 
      phone: '+254745678901', 
      email: 'sarah@rider.com', 
      role: 'RIDER', 
      status: 'ACTIVE', 
      blacklistStatus: 'NOT_BLACKLISTED', 
      displayName: 'Sarah Akinyi', 
      businessName: null, 
      createdAt: '2025-05-20T11:00:00Z',
      updatedAt: '2025-06-03T09:00:00Z',
      vehicleType: 'Motorcycle',
      vehiclePlate: 'KCE 654E',
      rating: 4.7,
      totalDeliveries: 8,
      isActive: true,
      profileImage: '',
      riderStatus: 'OFFLINE',
      operationArea: 'Karen',
      licenseNumber: 'DL56789012'
    } as Rider
  ];

  // Store disputes
  private disputes: Dispute[] = [
    {
      id: '7d9e1d0a-0101-4000-9000-000000000101',
      transactionId: '2c9d1c0a-0003-4000-9000-000000000003',
      transactionReference: 'ESC-TX1003',
      raisedById: '55555555-5555-4555-8555-555555555555',
      raisedByName: 'Peter Omondi',
      category: 'NON_DELIVERY',
      description: 'Item never delivered after payment. I paid KES 23,000 for a laptop on May 20th.',
      status: 'PENDING',
      assignedAdminId: null,
      resolution: null,
      resolvedAt: null,
      evidenceUrls: [
        'https://example.com/evidence/screenshot_wa.png',
        'https://example.com/evidence/payment_receipt.jpg'
      ],
      amount: 23000,
      createdAt: '2025-05-26T14:15:00Z',
      updatedAt: '2025-05-26T14:15:00Z',
      against: 'Faith Wanjiku'
    },
    {
      id: '7d9e1d0a-0102-4000-9000-000000000102',
      transactionId: '2c9d1c0a-0006-4000-9000-000000000006',
      transactionReference: 'ESC-TX1006',
      raisedById: 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa',
      raisedByName: 'Brian Odhiambo',
      category: 'NOT_AS_DESCRIBED',
      description: 'Received counterfeit phone. The phone I received is clearly counterfeit.',
      status: 'PENDING',
      assignedAdminId: null,
      resolution: null,
      resolvedAt: null,
      evidenceUrls: [
        'https://example.com/evidence/counterfeit_photo.jpg'
      ],
      amount: 9200,
      createdAt: '2025-05-24T16:10:00Z',
      updatedAt: '2025-05-24T16:10:00Z',
      against: 'Peter Omondi'
    },
    {
      id: '7d9e1d0a-0103-4000-9000-000000000103',
      transactionId: '2c9d1c0a-0001-4000-9000-000000000001',
      transactionReference: 'ESC-TX1001',
      raisedById: '22222222-2222-4222-8222-222222222222',
      raisedByName: 'Mercy Achieng',
      category: 'OTHER',
      description: 'Buyer confirmed then disputed. The buyer confirmed receipt then opened a dispute.',
      status: 'UNDER_REVIEW',
      assignedAdminId: null,
      resolution: null,
      resolvedAt: null,
      evidenceUrls: [
        'https://example.com/evidence/delivery_proof.png'
      ],
      amount: 12500,
      createdAt: '2025-05-28T10:00:00Z',
      updatedAt: '2025-05-28T10:00:00Z',
      against: 'John Kamau'
    }
  ];

  // Store audit logs
  private auditLogs: AuditLog[] = [
    { timestamp: new Date().toISOString().replace('T', ' ').substring(0, 19), admin: 'admin@escrowx.com', action: 'SUSPEND_USER', target: 'Peter Omondi', details: 'Suspended user Peter Omondi' },
    { timestamp: new Date(Date.now() - 86400000).toISOString().replace('T', ' ').substring(0, 19), admin: 'admin@escrowx.com', action: 'ACTIVATE_USER', target: 'Mercy Achieng', details: 'Activated user Mercy Achieng' },
    { timestamp: new Date(Date.now() - 172800000).toISOString().replace('T', ' ').substring(0, 19), admin: 'admin@escrowx.com', action: 'BLACKLIST_USER', target: 'Peter Omondi', details: 'Blacklisted user Peter Omondi' },
    { timestamp: new Date(Date.now() - 259200000).toISOString().replace('T', ' ').substring(0, 19), admin: 'admin@escrowx.com', action: 'FORCE_RELEASE', target: 'ESC-TX1005', details: 'Released KES 18,800 to seller' },
    { timestamp: new Date(Date.now() - 345600000).toISOString().replace('T', ' ').substring(0, 19), admin: 'admin@escrowx.com', action: 'RESOLVE_DISPUTE', target: 'DSP-101', details: 'Resolved: Refunded KES 23,000 to buyer' },
    { timestamp: new Date(Date.now() - 432000000).toISOString().replace('T', ' ').substring(0, 19), admin: 'admin@escrowx.com', action: 'ASSIGN_RIDER', target: 'ESC-TX1001', details: 'Assigned rider Sharon Atieno to transaction' }
  ];

  // Store delivery assignments
  private deliveryAssignments: any[] = [
    {
      id: 'da-001',
      transactionId: '2c9d1c0a-0001-4000-9000-000000000001',
      riderUserId: '99999999-9999-4999-8999-999999999999',
      assignedByUserId: 'admin-001',
      status: 'ASSIGNED',
      pickupAddress: 'Nairobi CBD, Kenyatta Ave',
      dropoffAddress: 'Westlands, Waiyaki Way',
      pickupDueAt: new Date(Date.now() + 3600000).toISOString(),
      pickedUpAt: null,
      arrivedAtBuyerAt: null,
      deliveredAt: null,
      riderMarkedDeliveredAt: null,
      sellerConfirmedDeliveredAt: null,
      buyerConfirmedDeliveredAt: null,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    },
    {
      id: 'da-002',
      transactionId: '2c9d1c0a-0002-4000-9000-000000000002',
      riderUserId: 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa',
      assignedByUserId: 'admin-001',
      status: 'DELIVERED_TO_BUYER',
      pickupAddress: 'Kilimani, Argwings Kodhek',
      dropoffAddress: 'Karen, Dagoretti Road',
      pickupDueAt: new Date(Date.now() - 86400000).toISOString(),
      pickedUpAt: new Date(Date.now() - 43200000).toISOString(),
      arrivedAtBuyerAt: new Date(Date.now() - 21600000).toISOString(),
      deliveredAt: new Date(Date.now() - 10800000).toISOString(),
      riderMarkedDeliveredAt: new Date(Date.now() - 14400000).toISOString(),
      sellerConfirmedDeliveredAt: new Date(Date.now() - 12600000).toISOString(),
      buyerConfirmedDeliveredAt: new Date(Date.now() - 10800000).toISOString(),
      createdAt: new Date(Date.now() - 172800000).toISOString(),
      updatedAt: new Date(Date.now() - 10800000).toISOString()
    }
  ];

  constructor() {
    // Initialize with some assignments
    this.assignedRiders.set('2c9d1c0a-0001-4000-9000-000000000001', {
      riderId: '99999999-9999-4999-8999-999999999999',
      riderName: 'Sharon Atieno',
      assignedAt: new Date().toISOString(),
      assignmentId: 'da-001',
      status: 'ASSIGNED'
    });
  }

  getTransactions() {
    return of<Transaction[]>(this.transactions);
  }

  getUsers() {
    return of<User[]>(this.users);
  }

  getDisputes() {
    return of<Dispute[]>(this.disputes);
  }

  getAuditLogs() {
    return of<AuditLog[]>(this.auditLogs);
  }

  // ===== RIDER-SPECIFIC METHODS =====

  /**
   * Get available riders (active, not blacklisted)
   */
  getAvailableRiders(): Observable<PageResponse<ApiUserDetails>> {
    const availableRiders = this.users.filter((r: any) => 
      r.role === 'RIDER' && 
      r.status === 'ACTIVE' &&
      r.blacklistStatus !== 'PERMANENTLY_BANNED'
    );

    // Map to ApiUserDetails format
    const mappedRiders: ApiUserDetails[] = availableRiders.map((rider: Rider) => ({
      id: rider.id,
      phone: rider.phone,
      email: rider.email,
      role: rider.role,
      status: rider.status,
      blacklistStatus: rider.blacklistStatus,
      displayName: rider.displayName,
      businessName: rider.businessName,
      avatarUrl: rider.avatarUrl || null,
      createdAt: rider.createdAt,
      updatedAt: rider.updatedAt || null
    }));

    return of({
      content: mappedRiders,
      totalElements: mappedRiders.length,
      totalPages: 1,
      size: mappedRiders.length,
      number: 0,
      first: true,
      last: true
    });
  }

  /**
   * Get riders with delivery stats
   */
  getRidersWithStats(): Observable<RiderWithStats[]> {
    const riders = this.users.filter((r: any) => r.role === 'RIDER') as Rider[];
    
    const ridersWithStats: RiderWithStats[] = riders.map((rider: Rider) => {
      const activeDeliveries = this.transactions.filter((tx: any) => 
        tx.riderId === rider.id && 
        ['FUNDS_HELD', 'ASSIGNED', 'ACCEPTED', 'PICKED_UP', 'IN_TRANSIT', 'ARRIVED_AT_BUYER'].includes(tx.status)
      ).length;
      
      return {
        ...rider,
        activeDeliveries,
        maxDeliveries: 4,
        isAvailable: activeDeliveries < 4 && rider.status === 'ACTIVE',
        riderStatus: rider.riderStatus || 'OFFLINE',
        vehicleType: rider.vehicleType || '',
        vehiclePlate: rider.vehiclePlate || ''
      };
    });

    return of(ridersWithStats);
  }

  /**
   * Get rider profile with stats
   */
  getRiderProfile(userId: string) {
    const rider = this.users.find((r: any) => r.id === userId && r.role === 'RIDER') as Rider;
    
    if (!rider) {
      return throwError(() => new Error('Rider not found'));
    }

    // Calculate deliveries
    const deliveries = this.transactions.filter((tx: any) => tx.riderId === userId);
    const completedDeliveries = deliveries.filter((tx: any) => tx.status === 'COMPLETED');

    const riderProfile: RiderProfile = {
      userId: rider.id,
      displayName: rider.displayName,
      phone: rider.phone,
      operationArea: (rider as any).operationArea || 'N/A',
      licenseNumber: (rider as any).licenseNumber || 'N/A',
      vehicleType: rider.vehicleType || 'Motorcycle',
      vehiclePlate: rider.vehiclePlate || 'N/A',
      riderStatus: (rider as any).riderStatus || 'OFFLINE',
      createdAt: rider.createdAt,
      updatedAt: rider.updatedAt || rider.createdAt
    };

    return of({
      success: true,
      message: 'Rider profile retrieved',
      data: riderProfile,
      timestamp: new Date().toISOString()
    });
  }

  /**
   * Get delivery assignments for a transaction
   */
  getDeliveryAssignments(transactionId: string) {
    const assignments = this.deliveryAssignments.filter(a => a.transactionId === transactionId);
    return of({
      success: true,
      message: 'Delivery assignments retrieved',
      data: assignments,
      timestamp: new Date().toISOString()
    });
  }

  /**
   * Assign a rider to a transaction (aligned with backend)
   */
  assignRider(transactionId: string, riderId: string): Observable<any> {
    const rider = this.users.find((r: any) => r.id === riderId && r.role === 'RIDER') as Rider;
    
    if (!rider) {
      return throwError(() => new Error('Rider not found'));
    }

    if (rider.status === 'SUSPENDED' || rider.status === 'BLACKLISTED') {
      return throwError(() => new Error('Rider is not available'));
    }

    // Check if transaction exists
    const transaction = this.transactions.find(tx => tx.id === transactionId);
    if (!transaction) {
      return throwError(() => new Error('Transaction not found'));
    }

    // Check if rider is already assigned to too many active deliveries (max 4)
    const activeDeliveries = this.transactions.filter((tx: any) => 
      tx.riderId === riderId && 
      ['FUNDS_HELD', 'ASSIGNED', 'ACCEPTED', 'PICKED_UP', 'IN_TRANSIT', 'ARRIVED_AT_BUYER'].includes(tx.status)
    );

    if (activeDeliveries.length >= 4) {
      return throwError(() => new Error('Rider is at capacity (max 4 concurrent deliveries)'));
    }

    // Generate assignment ID
    const assignmentId = `da-${Date.now()}`;
    
    // Store the assignment
    this.assignedRiders.set(transactionId, {
      riderId: riderId,
      riderName: rider.displayName,
      assignedAt: new Date().toISOString(),
      assignmentId: assignmentId,
      status: 'ASSIGNED'
    });

    // Create delivery assignment record
    const assignment = {
      id: assignmentId,
      transactionId: transactionId,
      riderUserId: riderId,
      assignedByUserId: 'admin-001',
      status: 'ASSIGNED',
      pickupAddress: 'Nairobi CBD, Kenyatta Ave',
      dropoffAddress: 'Westlands, Waiyaki Way',
      pickupDueAt: new Date(Date.now() + 3600000).toISOString(),
      pickedUpAt: null,
      arrivedAtBuyerAt: null,
      deliveredAt: null,
      riderMarkedDeliveredAt: null,
      sellerConfirmedDeliveredAt: null,
      buyerConfirmedDeliveredAt: null,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    };
    
    this.deliveryAssignments.push(assignment);

    // Update the transaction with rider ID and name
    const txIndex = this.transactions.findIndex(tx => tx.id === transactionId);
    if (txIndex !== -1) {
      this.transactions[txIndex] = {
        ...this.transactions[txIndex],
        riderId: riderId,
        riderName: rider.displayName
      };
    }

    return of({
      success: true,
      message: `Rider ${rider.displayName} assigned successfully`,
      data: {
        assignmentId: assignmentId,
        transactionId: transactionId,
        riderId: riderId,
        riderName: rider.displayName,
        status: 'ASSIGNED' as DeliveryAssignmentStatus,
        assignedAt: new Date().toISOString()
      },
      timestamp: new Date().toISOString()
    });
  }

  /**
   * Update delivery assignment status
   */
  updateAssignmentStatus(assignmentId: string, status: DeliveryAssignmentStatus) {
    const assignment = this.deliveryAssignments.find(a => a.id === assignmentId);
    if (!assignment) {
      return throwError(() => new Error('Assignment not found'));
    }

    assignment.status = status;
    assignment.updatedAt = new Date().toISOString();

    // Update transaction status based on assignment status
    const transaction = this.transactions.find(tx => tx.id === assignment.transactionId);
    if (transaction) {
      const txIndex = this.transactions.findIndex(tx => tx.id === assignment.transactionId);
      if (txIndex !== -1) {
        let newStatus = transaction.status;
        switch (status) {
          case 'PICKED_UP':
            newStatus = 'IN_DELIVERY';
            break;
          case 'DELIVERED_TO_BUYER':
            newStatus = 'COMPLETED';
            break;
          case 'CANCELLED':
          case 'FAILED':
            newStatus = 'CANCELLED';
            break;
          default:
            break;
        }
        this.transactions[txIndex] = {
          ...this.transactions[txIndex],
          status: newStatus
        };
      }
    }

    return of({
      success: true,
      message: `Assignment status updated to ${status}`,
      data: assignment,
      timestamp: new Date().toISOString()
    });
  }

  /**
   * Get all riders (for admin list)
   */
  getRiders(params?: { page?: number; size?: number; search?: string }): Observable<PageResponse<ApiUserDetails>> {
    let riders = this.users.filter((r: any) => r.role === 'RIDER');

    // Apply search filter
    if (params?.search) {
      const search = params.search.toLowerCase();
      riders = riders.filter((r: Rider) => 
        r.displayName.toLowerCase().includes(search) ||
        r.email.toLowerCase().includes(search) ||
        r.phone.includes(search)
      );
    }

    // Apply pagination
    const page = params?.page || 0;
    const size = params?.size || 20;
    const start = page * size;
    const end = start + size;
    const paginatedRiders = riders.slice(start, end);

    // Map to ApiUserDetails format
    const mappedRiders: ApiUserDetails[] = paginatedRiders.map((rider: Rider) => ({
      id: rider.id,
      phone: rider.phone,
      email: rider.email,
      role: rider.role,
      status: rider.status,
      blacklistStatus: rider.blacklistStatus,
      displayName: rider.displayName,
      businessName: rider.businessName,
      avatarUrl: rider.avatarUrl || null,
      createdAt: rider.createdAt,
      updatedAt: rider.updatedAt || null
    }));

    return of({
      content: mappedRiders,
      totalElements: riders.length,
      totalPages: Math.ceil(riders.length / size),
      size: size,
      number: page,
      first: page === 0,
      last: end >= riders.length
    });
  }

  /**
   * Get delivery assignments for a rider
   */
  getRiderDeliveries(riderId: string) {
    const assignments = this.deliveryAssignments.filter(a => a.riderUserId === riderId);
    return of({
      success: true,
      message: 'Rider deliveries retrieved',
      data: assignments,
      timestamp: new Date().toISOString()
    });
  }
}
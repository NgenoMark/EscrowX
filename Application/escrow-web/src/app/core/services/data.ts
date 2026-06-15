import { Injectable, inject, signal } from '@angular/core';
import { catchError, forkJoin, of } from 'rxjs';
import { Transaction, EscrowTransactionStatus } from '../models/transaction';
import { User } from '../models/user';
import { Dispute } from '../models/dispute';
import { AppEnvironmentService } from '../config/app-environment';
import { ApiEscrowTransaction, ApiService, ApiUserDetails } from './api.service';
import { MockDataService } from './mock-data.service';
import { NotificationService } from './notifications';

export interface AuditLog {
  timestamp: string;
  admin: string;
  action: string;
  target: string;
  details: string;
}

@Injectable({
  providedIn: 'root'
})
export class DataService {
  private appEnvironment = inject(AppEnvironmentService);
  private notificationService = inject(NotificationService);
  private apiService = inject(ApiService);
  private mockDataService = inject(MockDataService);

  private transactionsSignal = signal<Transaction[]>([]);
  private usersSignal = signal<User[]>([]);
  private disputesSignal = signal<Dispute[]>([]);
  private auditLogsSignal = signal<AuditLog[]>([]);

  readonly transactions = this.transactionsSignal.asReadonly();
  readonly users = this.usersSignal.asReadonly();
  readonly disputes = this.disputesSignal.asReadonly();
  readonly auditLogs = this.auditLogsSignal.asReadonly();

  constructor() {
    this.loadConfiguredData();
  }

  private loadConfiguredData(): void {
    if (this.appEnvironment.useMockData) {
      this.loadMockDataFromService();
      return;
    }

    this.loadApiData();
  }

  private loadMockDataFromService(): void {
    this.mockDataService.getTransactions().subscribe(transactions => this.transactionsSignal.set(transactions));
    this.mockDataService.getUsers().subscribe(users => this.usersSignal.set(users));
    this.mockDataService.getDisputes().subscribe(disputes => this.disputesSignal.set(disputes));
    this.mockDataService.getAuditLogs().subscribe(logs => this.auditLogsSignal.set(logs));
  }

  private loadApiData(): void {
    forkJoin({
      usersPage: this.apiService.getUsers({ size: 500 }).pipe(catchError(() => of(null))),
      transactionsPage: this.apiService.getTransactions({ size: 500 }).pipe(catchError(() => of(null))),
      disputesResponse: this.apiService.getDisputes({ limit: 500 }).pipe(catchError(() => of(null))),
      auditResponse: this.apiService.getAuditLogs({ limit: 500 }).pipe(catchError(() => of(null)))
    }).subscribe({
      next: ({ usersPage, transactionsPage, disputesResponse, auditResponse }) => {
        const users = usersPage?.content.map(user => this.mapApiUser(user)) ?? [];
        this.usersSignal.set(users);
        this.transactionsSignal.set(transactionsPage?.content.map(transaction => this.mapApiTransaction(transaction, users)) ?? []);
        this.disputesSignal.set(this.extractApiList(disputesResponse).map(dispute => this.mapApiDispute(dispute, users)));
        this.auditLogsSignal.set(this.extractApiList(auditResponse).map(log => this.mapApiAuditLog(log, users)));

        if (!usersPage || !transactionsPage || !disputesResponse || !auditResponse) {
          this.notificationService.add('Partial API Data', 'One or more admin API endpoints could not be loaded.', 'warning');
        }
      },
      error: () => {
        this.notificationService.add('API Unavailable', 'Could not load data from the configured API URL.', 'danger');
      }
    });
  }

  private extractApiList(response: unknown): any[] {
    if (!response) return [];
    const value = response as { data?: unknown; content?: unknown };
    if (Array.isArray(value)) return value;
    if (Array.isArray(value.content)) return value.content;
    if (Array.isArray(value.data)) return value.data;
    if (value.data && Array.isArray((value.data as { content?: unknown }).content)) {
      return (value.data as { content: any[] }).content;
    }
    return [];
  }

  private mapApiUser(user: ApiUserDetails): User {
    return {
      id: user.id,
      phone: user.phone,
      email: user.email,
      role: user.role as 'BUYER' | 'SELLER' | 'ADMIN' | 'SUPER_ADMIN',
      status: user.status as 'PENDING_VERIFICATION' | 'ACTIVE' | 'SUSPENDED' | 'BLACKLISTED',
      blacklistStatus: (user.blacklistStatus as 'NOT_BLACKLISTED' | 'TEMPORARILY_MUTED' | 'PERMANENTLY_BANNED' | 'UNDER_INVESTIGATION') ?? 'NOT_BLACKLISTED',
      displayName: user.displayName || user.email || user.phone,
      businessName: user.businessName ?? null,
      avatarUrl: user.avatarUrl ?? null,
      createdAt: user.createdAt,
      updatedAt: user.updatedAt ?? undefined
    };
  }

  private mapApiTransaction(transaction: ApiEscrowTransaction, users: User[]): Transaction {
    const buyer = users.find(user => user.id === transaction.buyerId);
    const seller = users.find(user => user.id === transaction.sellerId);

    return {
      id: transaction.id,
      reference: transaction.reference,
      buyerId: transaction.buyerId,
      sellerId: transaction.sellerId,
      buyer: buyer?.displayName || transaction.buyerId,
      seller: seller?.displayName || transaction.sellerId,
      title: transaction.title,
      productDescription: transaction.productDescription ?? undefined,
      description: transaction.productDescription ?? transaction.title,
      amount: Number(transaction.amount),
      initialDepositAmount: transaction.initialDepositAmount == null ? undefined : Number(transaction.initialDepositAmount),
      currency: transaction.currency,
      status: transaction.status as EscrowTransactionStatus,
      created: transaction.createdAt,
      createdAt: transaction.createdAt,
      updatedAt: transaction.updatedAt ?? undefined,
      deliveryDueAt: transaction.deliveryDueAt ?? undefined,
      autoReleaseAt: transaction.autoReleaseAt ?? undefined,
      autoReleaseDate: transaction.autoReleaseAt ?? undefined
    };
  }

  private mapApiDispute(dispute: any, users: User[]): Dispute {
    const transactionId = dispute.transactionId ?? dispute.transaction_id ?? dispute.txId ?? '';
    const raisedById = dispute.raisedBy ?? dispute.raised_by ?? dispute.raisedById ?? '';
    const raisedByUser = users.find(user => user.id === raisedById);
    const transaction = this.transactionsSignal().find(tx => tx.id === transactionId);
    const category = dispute.category ?? 'OTHER';

    return {
      id: String(dispute.id ?? ''),
      txId: String(transactionId),
      transactionId: String(transactionId),
      raisedById: String(raisedById),
      raisedBy: raisedByUser?.displayName ?? String(raisedById || 'Unknown user'),
      raisedByRole: raisedByUser?.role === 'SELLER' ? 'SELLER' : 'BUYER',
      against: transaction?.seller ?? transaction?.buyer ?? 'Unknown party',
      assignedAdminId: dispute.assignedAdminId ?? dispute.assigned_admin_id,
      category,
      reason: dispute.reason ?? category,
      description: dispute.description ?? '',
      evidence: dispute.evidence ?? [],
      status: dispute.status === 'OPEN' ? 'PENDING' : dispute.status,
      amount: Number(dispute.amount ?? transaction?.amount ?? 0),
      createdAt: dispute.createdAt ?? dispute.created_at ?? '',
      updatedAt: dispute.updatedAt ?? dispute.updated_at,
      resolvedAt: dispute.resolvedAt ?? dispute.resolved_at,
      resolution: dispute.resolution
    };
  }

  private mapApiAuditLog(log: any, users: User[]): AuditLog {
    const actorId = log.actorUserId ?? log.actor_user_id;
    const actor = users.find(user => user.id === actorId);
    const entityType = log.entityType ?? log.entity_type ?? '';
    const entityId = log.entityId ?? log.entity_id ?? '';
    const metadata = log.metadata ? JSON.stringify(log.metadata) : '';

    return {
      timestamp: log.createdAt ?? log.created_at ?? log.timestamp ?? '',
      admin: actor?.email ?? actorId ?? 'system',
      action: log.action ?? 'UNKNOWN_ACTION',
      target: [entityType, entityId].filter(Boolean).join(': ') || 'system',
      details: metadata || log.details || ''
    };
  }

  private initializeSampleAuditLogs(): void {
    if (this.auditLogsSignal().length === 0) {
      const sampleLogs: AuditLog[] = [
        { timestamp: new Date().toISOString().replace('T', ' ').substring(0, 19), admin: 'admin@escrowx.com', action: 'SUSPEND_USER', target: 'Peter Omondi', details: 'Suspended user Peter Omondi' },
        { timestamp: new Date(Date.now() - 86400000).toISOString().replace('T', ' ').substring(0, 19), admin: 'admin@escrowx.com', action: 'ACTIVATE_USER', target: 'Mercy Achieng', details: 'Activated user Mercy Achieng' },
        { timestamp: new Date(Date.now() - 172800000).toISOString().replace('T', ' ').substring(0, 19), admin: 'admin@escrowx.com', action: 'BLACKLIST_USER', target: 'Peter Omondi', details: 'Blacklisted user Peter Omondi' },
        { timestamp: new Date(Date.now() - 259200000).toISOString().replace('T', ' ').substring(0, 19), admin: 'admin@escrowx.com', action: 'FORCE_RELEASE', target: 'ESC-TX1005', details: 'Released KES 18,800 to seller' },
        { timestamp: new Date(Date.now() - 345600000).toISOString().replace('T', ' ').substring(0, 19), admin: 'admin@escrowx.com', action: 'RESOLVE_DISPUTE', target: 'DSP-101', details: 'Resolved: Refunded KES 23,000 to buyer' }
      ];
      this.auditLogsSignal.set(sampleLogs);
    }
  }

  private loadMockData() {
    // Mock transactions
    this.transactionsSignal.set([
      { id: "ESC-TX1001", buyer: "John Kamau", seller: "Mercy Achieng", amount: 12500, status: "FUNDS_HELD", created: "2025-05-28", autoReleaseDate: "2025-06-04" },
      { id: "ESC-TX1002", buyer: "Zahra Hassan", seller: "Samuel Njoroge", amount: 4500, status: "COMPLETED", created: "2025-05-27" },
      { id: "ESC-TX1003", buyer: "Peter Omondi", seller: "Faith Wanjiku", amount: 23000, status: "DISPUTED", created: "2025-05-26" },
      { id: "ESC-TX1004", buyer: "Grace Adhiambo", seller: "James Mwangi", amount: 6750, status: "FUNDS_HELD", created: "2025-05-29", autoReleaseDate: "2025-06-05" },
      { id: "ESC-TX1005", buyer: "John Kamau", seller: "Sharon Atieno", amount: 18800, status: "COMPLETED", created: "2025-05-25" },
      { id: "ESC-TX1006", buyer: "Brian Odhiambo", seller: "Peter Omondi", amount: 9200, status: "DISPUTED", created: "2025-05-24" },
      { id: "ESC-TX1007", buyer: "Linet Wambui", seller: "Samuel Njoroge", amount: 33000, status: "FUNDS_HELD", created: "2025-05-30", autoReleaseDate: "2025-06-06" }
    ]);

    // Mock users strictly following the User interface
    this.usersSignal.set([
      {
        id: "1",
        phone: "+254712345678",
        email: "john@example.com",
        role: "BUYER",
        status: "ACTIVE",
        blacklistStatus: "NOT_BLACKLISTED",
        displayName: "John Kamau",
        businessName: null,
        createdAt: "2025-01-15T10:00:00Z"
      },
      {
        id: "2",
        phone: "+254722987654",
        email: "mercy@seller.com",
        role: "SELLER",
        status: "ACTIVE",
        blacklistStatus: "NOT_BLACKLISTED",
        displayName: "Mercy Achieng",
        businessName: "Mercy Traders",
        createdAt: "2025-02-10T09:30:00Z"
      },
      {
        id: "3",
        phone: "+254799887766",
        email: "peter@fraud.com",
        role: "SELLER",
        status: "BLACKLISTED",
        blacklistStatus: "PERMANENTLY_BANNED",
        displayName: "Peter Omondi",
        businessName: "Peter Supplies",
        createdAt: "2025-01-20T14:15:00Z"
      },
      {
        id: "4",
        phone: "+254700112233",
        email: "zahra@buyer.com",
        role: "BUYER",
        status: "PENDING_VERIFICATION",
        blacklistStatus: "NOT_BLACKLISTED",
        displayName: "Zahra Hassan",
        businessName: null,
        createdAt: "2025-03-05T11:45:00Z"
      },
      {
        id: "5",
        phone: "+254744556677",
        email: "samuel@shop.com",
        role: "SELLER",
        status: "ACTIVE",
        blacklistStatus: "NOT_BLACKLISTED",
        displayName: "Samuel Njoroge",
        businessName: "Samuel Shop",
        createdAt: "2025-02-20T08:20:00Z"
      },
      {
        id: "6",
        phone: "+254711223344",
        email: "grace@designs.com",
        role: "SELLER",
        status: "ACTIVE",
        blacklistStatus: "UNDER_INVESTIGATION",
        displayName: "Grace Adhiambo",
        businessName: "Grace Designs",
        createdAt: "2025-03-15T16:10:00Z"
      }
    ]);

    // Mock disputes
    this.disputesSignal.set([
      {
        id: "DSP-101",
        txId: "ESC-TX1003",
        raisedBy: "Peter Omondi",
        raisedByRole: "BUYER",
        against: "Faith Wanjiku",
        reason: "Item never delivered after payment",
        description: "I paid KES 23,000 for a laptop on May 20th.",
        evidence: ["screenshot_wa.png", "payment_receipt.jpg"],
        status: "PENDING",
        amount: 23000,
        createdAt: "2025-05-26"
      },
      {
        id: "DSP-102",
        txId: "ESC-TX1006",
        raisedBy: "Brian Odhiambo",
        raisedByRole: "BUYER",
        against: "Peter Omondi",
        reason: "Received counterfeit phone",
        description: "The phone I received is clearly counterfeit.",
        evidence: ["counterfeit_photo.jpg"],
        status: "PENDING",
        amount: 9200,
        createdAt: "2025-05-24"
      },
      {
        id: "DSP-103",
        txId: "ESC-TX1001",
        raisedBy: "Mercy Achieng",
        raisedByRole: "SELLER",
        against: "John Kamau",
        reason: "Buyer confirmed then disputed",
        description: "The buyer confirmed receipt then opened a dispute.",
        evidence: ["delivery_proof.png"],
        status: "UNDER_REVIEW",
        amount: 12500,
        createdAt: "2025-05-28"
      }
    ]);
  }

  // ========== DASHBOARD HELPER METHODS ==========

  getTotalVolume(): number {
    return this.transactionsSignal().reduce((sum, tx) => sum + tx.amount, 0);
  }

  getPlatformFees(): number {
    return this.getTotalVolume() * 0.025;
  }

  getActiveEscrows(): number {
    return this.transactionsSignal().filter(tx => tx.status === 'FUNDS_HELD').length;
  }

  getOpenDisputes(): number {
    return this.disputesSignal().filter(d => d.status !== 'RESOLVED').length;
  }

  getRecentTransactions(limit: number = 5): Transaction[] {
    return this.transactionsSignal().slice(0, limit);
  }

  // ========== CHART DATA METHODS ==========

  getWeeklyVolume(): number[] {
    return [520000, 684000, 760000, 945000];
  }

  getWeeklyLabels(): string[] {
    return ['Week 1', 'Week 2', 'Week 3', 'Week 4'];
  }

  getDisputeTrend(): number[] {
    return [2, 4, 3, 5, 4, 3];
  }

  getDisputeLabels(): string[] {
    return ['Wk22', 'Wk23', 'Wk24', 'Wk25', 'Wk26', 'This week'];
  }

  getTransactionVolumeByDay(days: number = 30): { labels: string[], data: number[] } {
    const labels: string[] = [];
    const data: number[] = [];
    for (let i = days - 1; i >= 0; i--) {
      const date = new Date();
      date.setDate(date.getDate() - i);
      labels.push(date.toLocaleDateString('en-KE', { month: 'short', day: 'numeric' }));
      data.push(Math.floor(Math.random() * 50000) + 10000);
    }
    return { labels, data };
  }

  getDisputeTrendByWeek(weeks: number = 12): { labels: string[], data: number[] } {
    const labels: string[] = [];
    const data: number[] = [];
    for (let i = weeks - 1; i >= 0; i--) {
      labels.push(`W${Math.floor(i / 4) + 1}`);
      data.push(Math.floor(Math.random() * 8) + 1);
    }
    return { labels, data };
  }

  getUserGrowth(): { labels: string[], data: number[] } {
    return {
      labels: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun'],
      data: [45, 78, 120, 189, 245, 312]
    };
  }

  getTransactionStatusDistribution(): { labels: string[], data: number[] } {
    const stats = this.getTransactionStats();
    return {
      labels: ['Completed', 'Funds Held', 'Disputed', 'Refunded', 'Cancelled'],
      data: [stats.completed, stats.fundsHeld, stats.disputed, stats.refunded, stats.cancelled || 0]
    };
  }

  getTopSellers(limit: number = 5): { name: string, volume: number, transactions: number }[] {
    const sellers = new Map<string, { volume: number, transactions: number }>();

    this.transactionsSignal().forEach(tx => {
      const current = sellers.get(tx.seller) || { volume: 0, transactions: 0 };
      current.volume += tx.amount;
      current.transactions += 1;
      sellers.set(tx.seller, current);
    });

    return Array.from(sellers.entries())
      .map(([name, data]) => ({ name, volume: data.volume, transactions: data.transactions }))
      .sort((a, b) => b.volume - a.volume)
      .slice(0, limit);
  }

  // ========== USER MANAGEMENT METHODS ==========

  suspendUser(userId: string): void {
    const users = this.usersSignal();
    const user = users.find(u => u.id === userId);
    if (user && user.status === 'ACTIVE') {
      user.status = 'SUSPENDED';
      this.usersSignal.set([...users]);
      this.addAuditLog('SUSPEND_USER', user.email, `Suspended user ${user.displayName}`);
      this.notificationService.add('User Suspended', `${user.displayName} has been suspended`, 'warning');
    }
  }

  activateUser(userId: string): void {
    const users = this.usersSignal();
    const user = users.find(u => u.id === userId);
    if (user && user.status === 'SUSPENDED' && user.blacklistStatus !== 'PERMANENTLY_BANNED') {
      user.status = 'ACTIVE';
      this.usersSignal.set([...users]);
      this.addAuditLog('ACTIVATE_USER', user.email, `Activated user ${user.displayName}`);
      this.notificationService.add('User Activated', `${user.displayName} has been activated`, 'success');
    }
  }

  blacklistUser(userId: string): void {
    const users = this.usersSignal();
    const user = users.find(u => u.id === userId);
    if (user && user.blacklistStatus !== 'PERMANENTLY_BANNED') {
      user.blacklistStatus = 'PERMANENTLY_BANNED';
      user.status = 'BLACKLISTED';
      this.usersSignal.set([...users]);
      this.addAuditLog('BLACKLIST_USER', user.email, `Blacklisted user ${user.displayName}`);
      this.notificationService.add('User Blacklisted', `${user.displayName} has been permanently banned`, 'danger');
    }
  }

  removeBlacklist(userId: string): void {
    const users = this.usersSignal();
    const user = users.find(u => u.id === userId);
    if (user && user.blacklistStatus === 'PERMANENTLY_BANNED') {
      user.blacklistStatus = 'NOT_BLACKLISTED';
      user.status = 'PENDING_VERIFICATION';
      this.usersSignal.set([...users]);
      this.addAuditLog('REMOVE_BLACKLIST', user.email, `Removed blacklist for ${user.displayName}`);
      this.notificationService.add('Blacklist Removed', `${user.displayName} has been removed from blacklist`, 'info');
    }
  }

  getFilteredUsers(searchTerm: string = ''): User[] {
    const users = this.usersSignal();
    if (!searchTerm) return users;
    const term = searchTerm.toLowerCase();
    return users.filter(user =>
      user.displayName.toLowerCase().includes(term) ||
      user.phone.includes(term) ||
      user.email.toLowerCase().includes(term)
    );
  }

  getUserStats() {
    const users = this.usersSignal();
    return {
      total: users.length,
      active: users.filter(u => u.status === 'ACTIVE').length,
      suspended: users.filter(u => u.status === 'SUSPENDED').length,
      phoneVerified: users.filter(u => u.status === 'ACTIVE' && u.blacklistStatus !== 'PERMANENTLY_BANNED').length,
      kycApproved: 0, // Not implemented in base User interface
      blacklisted: users.filter(u => u.blacklistStatus === 'PERMANENTLY_BANNED').length
    };
  }

  // ========== TRANSACTION MANAGEMENT METHODS ==========

  forceReleaseFunds(transactionId: string): void {
    const transactions = this.transactionsSignal();
    const transaction = transactions.find(t => t.id === transactionId);

    if (transaction && transaction.status === 'FUNDS_HELD') {
      transaction.status = 'COMPLETED';
      transaction.completedAt = new Date().toISOString().split('T')[0];
      this.transactionsSignal.set([...transactions]);
      this.addAuditLog('FORCE_RELEASE', transactionId, `Released KES ${transaction.amount.toLocaleString()} to seller`);
      this.notificationService.add('Funds Released', `Released KES ${transaction.amount.toLocaleString()} to ${transaction.seller}`, 'success');
    }
  }

  forceRefund(transactionId: string): void {
    const transactions = this.transactionsSignal();
    const transaction = transactions.find(t => t.id === transactionId);

    if (transaction && (transaction.status === 'FUNDS_HELD' || transaction.status === 'DISPUTED')) {
      transaction.status = 'REFUNDED';
      transaction.completedAt = new Date().toISOString().split('T')[0];
      this.transactionsSignal.set([...transactions]);
      this.addAuditLog('FORCE_REFUND', transactionId, `Refunded KES ${transaction.amount.toLocaleString()} to buyer`);
      this.notificationService.add('Funds Refunded', `Refunded KES ${transaction.amount.toLocaleString()} to ${transaction.buyer}`, 'warning');
    }
  }

  cancelTransaction(transactionId: string): void {
    const transactions = this.transactionsSignal();
    const transaction = transactions.find(t => t.id === transactionId);

    if (transaction && transaction.status === 'FUNDS_HELD') {
      transaction.status = 'CANCELLED';
      this.transactionsSignal.set([...transactions]);
      this.addAuditLog('CANCEL_TRANSACTION', transactionId, `Transaction cancelled by admin`);
      this.notificationService.add('Transaction Cancelled', `Transaction ${transactionId} has been cancelled`, 'danger');
    }
  }

  getTransactionById(transactionId: string): Transaction | undefined {
    return this.transactionsSignal().find(t => t.id === transactionId);
  }

  getFilteredTransactions(statusFilter: string = 'all', searchTerm: string = ''): Transaction[] {
    let filtered = this.transactionsSignal();
    if (statusFilter !== 'all') {
      filtered = filtered.filter(t => t.status === statusFilter);
    }
    if (searchTerm) {
      const term = searchTerm.toLowerCase();
      filtered = filtered.filter(t =>
        t.id.toLowerCase().includes(term) ||
        t.buyer.toLowerCase().includes(term) ||
        t.seller.toLowerCase().includes(term)
      );
    }
    return filtered;
  }

  getTransactionStats() {
    const transactions = this.transactionsSignal();
    return {
      total: transactions.length,
      fundsHeld: transactions.filter(t => t.status === 'FUNDS_HELD').length,
      completed: transactions.filter(t => t.status === 'COMPLETED').length,
      disputed: transactions.filter(t => t.status === 'DISPUTED').length,
      refunded: transactions.filter(t => t.status === 'REFUNDED').length,
      cancelled: transactions.filter(t => t.status === 'CANCELLED').length,
      totalVolume: this.getTotalVolume(),
      totalFees: this.getPlatformFees()
    };
  }

  // ========== DISPUTE MANAGEMENT METHODS ==========

  getDisputeById(disputeId: string): Dispute | undefined {
    return this.disputesSignal().find(d => d.id === disputeId);
  }

  getFilteredDisputes(statusFilter: string = 'all'): Dispute[] {
    if (statusFilter === 'all') {
      return this.disputesSignal();
    }
    return this.disputesSignal().filter(d => d.status === statusFilter);
  }

  getDisputeStats() {
    const disputes = this.disputesSignal();
    return {
      total: disputes.length,
      pending: disputes.filter(d => d.status === 'PENDING').length,
      underReview: disputes.filter(d => d.status === 'UNDER_REVIEW').length,
      resolved: disputes.filter(d => d.status === 'RESOLVED').length,
      escalated: disputes.filter(d => d.status === 'ESCALATED').length,
      totalAmountInDispute: disputes.reduce((sum, d) => sum + d.amount, 0)
    };
  }

  resolveDisputeRefundBuyer(disputeId: string): void {
    const disputes = this.disputesSignal();
    const dispute = disputes.find(d => d.id === disputeId);

    if (dispute && dispute.status !== 'RESOLVED') {
      dispute.status = 'RESOLVED';
      dispute.resolvedAt = new Date().toISOString().split('T')[0];
      dispute.resolution = 'REFUND_BUYER';

      const transactions = this.transactionsSignal();
      const transaction = transactions.find(t => t.id === dispute.txId);
      if (transaction && transaction.status !== 'COMPLETED') {
        transaction.status = 'REFUNDED';
        transaction.completedAt = new Date().toISOString().split('T')[0];
        this.transactionsSignal.set([...transactions]);
      }

      this.disputesSignal.set([...disputes]);
      this.addAuditLog('RESOLVE_DISPUTE', disputeId, `Resolved: Refunded KES ${dispute.amount.toLocaleString()} to buyer`);
      this.notificationService.add('Dispute Resolved', `Refunded KES ${dispute.amount.toLocaleString()} to buyer`, 'success');
    }
  }

  resolveDisputeReleaseSeller(disputeId: string): void {
    const disputes = this.disputesSignal();
    const dispute = disputes.find(d => d.id === disputeId);

    if (dispute && dispute.status !== 'RESOLVED') {
      dispute.status = 'RESOLVED';
      dispute.resolvedAt = new Date().toISOString().split('T')[0];
      dispute.resolution = 'RELEASE_SELLER';

      const transactions = this.transactionsSignal();
      const transaction = transactions.find(t => t.id === dispute.txId);
      if (transaction && transaction.status !== 'COMPLETED') {
        transaction.status = 'COMPLETED';
        transaction.completedAt = new Date().toISOString().split('T')[0];
        this.transactionsSignal.set([...transactions]);
      }

      this.disputesSignal.set([...disputes]);
      this.addAuditLog('RESOLVE_DISPUTE', disputeId, `Resolved: Released KES ${dispute.amount.toLocaleString()} to seller`);
      this.notificationService.add('Dispute Resolved', `Released KES ${dispute.amount.toLocaleString()} to seller`, 'success');
    }
  }

  resolveDisputePartial(disputeId: string, partialAmount: number, notes: string): void {
    const disputes = this.disputesSignal();
    const dispute = disputes.find(d => d.id === disputeId);

    if (dispute && dispute.status !== 'RESOLVED' && partialAmount > 0 && partialAmount < dispute.amount) {
      dispute.status = 'RESOLVED';
      dispute.resolvedAt = new Date().toISOString().split('T')[0];
      dispute.resolution = 'PARTIAL_SETTLEMENT';
      dispute.partialAmount = partialAmount;
      dispute.adminNotes = notes;

      const transactions = this.transactionsSignal();
      const transaction = transactions.find(t => t.id === dispute.txId);
      if (transaction) {
        transaction.status = 'COMPLETED';
        transaction.completedAt = new Date().toISOString().split('T')[0];
        this.transactionsSignal.set([...transactions]);
      }

      this.disputesSignal.set([...disputes]);
      this.addAuditLog('RESOLVE_DISPUTE', disputeId, `Resolved: Partial settlement of KES ${partialAmount.toLocaleString()}`);
      this.notificationService.add('Dispute Resolved', `Partial settlement of KES ${partialAmount.toLocaleString()}`, 'info');
    }
  }

  updateDisputeStatus(disputeId: string, status: 'PENDING' | 'UNDER_REVIEW' | 'ESCALATED'): void {
    const disputes = this.disputesSignal();
    const dispute = disputes.find(d => d.id === disputeId);

    if (dispute) {
      dispute.status = status;
      this.disputesSignal.set([...disputes]);
      this.addAuditLog('UPDATE_DISPUTE_STATUS', disputeId, `Status changed to ${status}`);
      this.notificationService.add('Dispute Updated', `Dispute ${disputeId} status changed to ${status}`, 'info');
    }
  }

  addAdminNote(disputeId: string, note: string): void {
    const disputes = this.disputesSignal();
    const dispute = disputes.find(d => d.id === disputeId);

    if (dispute) {
      const timestamp = new Date().toLocaleString();
      dispute.adminNotes = dispute.adminNotes
        ? `${dispute.adminNotes}\n[${timestamp}] ${note}`
        : `[${timestamp}] ${note}`;
      this.disputesSignal.set([...disputes]);
      this.addAuditLog('ADD_ADMIN_NOTE', disputeId, note);
    }
  }

  // ========== AUDIT LOG METHODS ==========

  private addAuditLog(action: string, target: string, details: string): void {
    const newLog: AuditLog = {
      timestamp: new Date().toISOString().replace('T', ' ').substring(0, 19),
      admin: 'admin@escrowx.com',
      action: action,
      target: target,
      details: details
    };
    const currentLogs = this.auditLogsSignal();
    this.auditLogsSignal.set([newLog, ...currentLogs].slice(0, 500));
  }

  getAuditLogs(): AuditLog[] {
    return this.auditLogsSignal();
  }

  getFilteredAuditLogs(actionFilter: string = 'all', searchTerm: string = ''): AuditLog[] {
    let filtered = this.auditLogsSignal();

    if (actionFilter !== 'all') {
      filtered = filtered.filter(log => log.action === actionFilter);
    }

    if (searchTerm) {
      const term = searchTerm.toLowerCase();
      filtered = filtered.filter(log =>
        log.action.toLowerCase().includes(term) ||
        log.target.toLowerCase().includes(term) ||
        log.details.toLowerCase().includes(term) ||
        log.admin.toLowerCase().includes(term)
      );
    }

    return filtered;
  }

  getAuditLogStats(): { total: number; uniqueActions: number; mostRecent: string; actionBreakdown: { action: string; count: number }[] } {
    const logs = this.auditLogsSignal();
    const actionCounts = new Map<string, number>();

    logs.forEach(log => {
      actionCounts.set(log.action, (actionCounts.get(log.action) || 0) + 1);
    });

    return {
      total: logs.length,
      uniqueActions: actionCounts.size,
      mostRecent: logs[0]?.timestamp || 'N/A',
      actionBreakdown: Array.from(actionCounts.entries()).map(([action, count]) => ({ action, count }))
    };
  }

  clearAuditLogs(): void {
    this.auditLogsSignal.set([]);
    this.addAuditLog('CLEAR_AUDIT_LOGS', 'system', 'All audit logs cleared by admin');
    this.notificationService.add('Audit Logs Cleared', 'All audit logs have been cleared', 'warning');
  }
}

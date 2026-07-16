// src/app/core/services/data.ts
import { Injectable, inject, signal } from '@angular/core';
import { catchError, forkJoin, of, firstValueFrom } from 'rxjs';
import { Transaction, EscrowTransactionStatus } from '../models/transaction';
import { User } from '../models/user';
import { Dispute } from '../models/dispute';
import { AppEnvironmentService } from '../config/app-environment';
import { ApiEscrowTransaction, ApiService, ApiUserDetails, PageResponse } from './api.service';
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

  // --- Loading State ---
  private loadingSignal = signal<boolean>(false);
  readonly isLoading = this.loadingSignal.asReadonly();

  // --- Data Signals ---
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

  // ================================================================
  // INITIAL DATA LOAD
  // ================================================================
  private loadConfiguredData(): void {
    if (this.appEnvironment.useMockData) {
      this.loadMockData();
      return;
    }
    this.loadApiData();
  }

  private loadMockData(): void {
    this.loadingSignal.set(true);
    setTimeout(() => {
      this.mockDataService.getTransactions().subscribe(tx => this.transactionsSignal.set(tx));
      this.mockDataService.getUsers().subscribe(users => this.usersSignal.set(users));
      this.mockDataService.getDisputes().subscribe(disputes => this.disputesSignal.set(disputes));
      this.mockDataService.getAuditLogs().subscribe(logs => this.auditLogsSignal.set(logs));
      setTimeout(() => this.loadingSignal.set(false), 300);
    }, 300);
  }

  // ================================================================
  // LOAD API DATA – with robust extraction and logging
  // ================================================================
  private async loadApiData(): Promise<void> {
    this.loadingSignal.set(true);

    try {
      // Fetch all data in parallel using the correct endpoints
      const [usersPage, transactionsPage, disputesPage, auditResponse] = await Promise.all([
        firstValueFrom(this.apiService.getUsers().pipe(catchError(() => of(null)))),
        firstValueFrom(this.apiService.getTransactions().pipe(catchError(() => of(null)))),
        firstValueFrom(this.apiService.getDisputes().pipe(catchError(() => of(null)))),
        firstValueFrom(this.apiService.getAuditLogs().pipe(catchError(() => of(null))))
      ]);

      console.log('🔍 disputesPage:', disputesPage);

      // Extract arrays (disputesPage is already a PageResponse with content)
      const usersArray = this.extractData(usersPage);
      const transactionsArray = this.extractData(transactionsPage);
      const disputesArray = this.extractData(disputesPage); // content from PageResponse
      const auditArray = this.extractData(auditResponse);

      console.log('🔍 Extracted disputesArray:', disputesArray);
      if (disputesArray.length) {
        console.log('🔍 First dispute raw summary:', disputesArray[0]);
      }

      // Map users
      const users = usersArray.map(u => this.mapApiUser(u));
      this.usersSignal.set(users);

      // Map transactions (needs users)
      const transactions = transactionsArray.map(tx => this.mapApiTransaction(tx, users));
      this.transactionsSignal.set(transactions);

      // Map disputes (summary) – these will be enriched later when selected
      const disputes = disputesArray.map(d => this.mapApiDisputeSummary(d, users, transactions));
      this.disputesSignal.set(disputes);
      console.log('Disputes signal', this.disputesSignal());

      // Map audit logs
      const auditLogs = auditArray.map(log => this.mapApiAuditLog(log, users));
      this.auditLogsSignal.set(auditLogs);

      // Notify if any endpoint failed
      if (!usersPage || !transactionsPage || !disputesPage || !auditResponse) {
        this.notificationService.add('Partial API Data', 'Some data could not be loaded.', 'warning');
      }
    } catch (error) {
      console.error('API Load Error:', error);
      this.notificationService.add('API Unavailable', 'Could not load data.', 'danger');
    } finally {
      this.loadingSignal.set(false);
    }
  }

  // ================================================================
  // ROBUST extractData – handles PageResponse, ApiResponse, etc.
  // ================================================================
  private extractData(response: any): any[] {
    if (!response) return [];
    if (Array.isArray(response)) return response;

    // If it's a PageResponse, take content
    if (response.content && Array.isArray(response.content)) return response.content;

    // If it's an ApiResponse with data array
    if (response.data && Array.isArray(response.data)) return response.data;

    // If it's an object with a data property that is a PageResponse
    if (response.data && response.data.content && Array.isArray(response.data.content)) {
      return response.data.content;
    }

    // If it's a plain object with a content property
    if (response.content && Array.isArray(response.content)) return response.content;

    // Last resort: look for any property that is an array of objects with 'id'
    if (typeof response === 'object' && response !== null) {
      for (const key of Object.keys(response)) {
        const value = response[key];
        if (Array.isArray(value) && value.length > 0 && value[0]?.id) {
          console.log(`🔍 extractData: found array in property "${key}"`);
          return value;
        }
      }
    }

    return [];
  }

  // ================================================================
  // MAPPERS
  // ================================================================
  // services/data.ts - Update mappers

  private mapApiUser(user: ApiUserDetails): User {
    return {
      id: user.id,
      phone: user.phone,
      email: user.email,
      role: user.role as 'BUYER' | 'SELLER' | 'ADMIN' | 'SUPER_ADMIN',
      status: user.status as 'PENDING_VERIFICATION' | 'PENDING_ADMIN_APPROVAL' | 'ACTIVE' | 'SUSPENDED' | 'BLACKLISTED',
      blacklistStatus: (user.blacklistStatus as any) ?? 'NOT_BLACKLISTED',
      displayName: user.displayName || user.email || user.phone,
      businessName: user.businessName ?? null,
      avatarUrl: user.avatarUrl ?? null,
      createdAt: user.createdAt,
      updatedAt: user.updatedAt ?? undefined
    };
  }

  // Add helper methods
  getPendingUsers(): User[] {
    return this.usersSignal().filter(u => u.status === 'PENDING_VERIFICATION');
  }

  getPendingSellers(): User[] {
    return this.usersSignal().filter(u => u.status === 'PENDING_ADMIN_APPROVAL');
  }

  private mapApiTransaction(tx: ApiEscrowTransaction, users: User[]): Transaction {
    const buyer = users.find(u => u.id === tx.buyerId);
    const seller = users.find(u => u.id === tx.sellerId);
    return {
      id: tx.id,
      reference: tx.reference,
      buyerId: tx.buyerId,
      sellerId: tx.sellerId,
      buyer: buyer?.displayName || tx.buyerId,
      seller: seller?.displayName || tx.sellerId,
      title: tx.title,
      productDescription: tx.productDescription ?? undefined,
      description: tx.productDescription ?? tx.title,
      amount: Number(tx.amount),
      initialDepositAmount: tx.initialDepositAmount == null ? undefined : Number(tx.initialDepositAmount),
      currency: tx.currency,
      status: tx.status as EscrowTransactionStatus,
      created: tx.createdAt,
      createdAt: tx.createdAt,
      updatedAt: tx.updatedAt ?? undefined,
      deliveryDueAt: tx.deliveryDueAt ?? undefined,
      autoReleaseAt: tx.autoReleaseAt ?? undefined,
      autoReleaseDate: tx.autoReleaseAt ?? undefined
    };
  }

  /**
   * Map a dispute summary (from list endpoint) – minimal fields.
   * This will be enriched later with full details.
   */
  private mapApiDisputeSummary(raw: any, users: User[], transactions: Transaction[]): Dispute {
    const transactionId = raw.transactionId ?? '';
    const transaction = transactions.find(t => t.id === transactionId);
    const amount = Number(raw.amount ?? transaction?.amount ?? 0);

    return {
      id: raw.id ?? '',
      transactionId,
      transactionReference: raw.transactionReference ?? '',
      raisedById: '', // not available in summary
      raisedByName: 'Loading...', // placeholder
      category: raw.category ?? 'OTHER',
      description: '',
      status: raw.status ?? 'OPEN',
      assignedAdminId: raw.assignedAdminId ?? null,
      resolution: null,
      resolvedAt: null,
      evidenceUrls: [],
      amount,
      createdAt: raw.createdAt ?? '',
      updatedAt: raw.updatedAt ?? '',
      against: 'Unknown', // placeholder
      adminNotes: '',
      partialAmount: undefined
    };
  }

  /**
   * Map full dispute detail (from /disputes/{id}).
   */
  private mapApiDisputeFull(raw: any, users: User[], transactions: Transaction[]): Dispute {
    console.log('🧾 mapApiDisputeFull raw:', raw);

    const id = raw.id ?? '';
    const transactionId = raw.transactionId ?? '';
    const transactionReference = raw.transactionReference ?? '';
    const raisedById = raw.raisedById ?? '';
    const raisedByName = raw.raisedByName ?? 'Unknown user';
    const category = raw.category ?? 'OTHER';
    const description = raw.description ?? '';
    const status = raw.status ?? 'OPEN';
    const assignedAdminId = raw.assignedAdminId ?? null;
    const resolution = raw.resolution ?? null;
    const resolvedAt = raw.resolvedAt ?? null;
    const createdAt = raw.createdAt ?? '';
    const updatedAt = raw.updatedAt ?? '';
    const evidenceUrls = Array.isArray(raw.evidenceUrls) ? raw.evidenceUrls : [];

    const transaction = transactions.find(t => t.id === transactionId);
    let against = 'Unknown party';
    if (transaction) {
      const user = users.find(u => u.id === raisedById);
      if (user?.id === transaction.buyerId) against = transaction.seller;
      else if (user?.id === transaction.sellerId) against = transaction.buyer;
    }

    const amount = Number(raw.amount ?? transaction?.amount ?? 0);

    return {
      id,
      transactionId,
      transactionReference,
      raisedById,
      raisedByName,
      category,
      description,
      status,
      assignedAdminId,
      resolution,
      resolvedAt,
      evidenceUrls,
      amount,
      createdAt,
      updatedAt,
      against,
      adminNotes: '',
      partialAmount: undefined
    };
  }

  private mapApiAuditLog(log: any, users: User[]): AuditLog {
    const actorId = log.actorUserId ?? log.actor_user_id;
    const actor = users.find(u => u.id === actorId);
    return {
      timestamp: log.createdAt ?? log.created_at ?? log.timestamp ?? '',
      admin: actor?.email ?? actorId ?? 'system',
      action: log.action ?? 'UNKNOWN_ACTION',
      target: log.entityType ?? log.entity_type ?? 'system',
      details: log.metadata ? JSON.stringify(log.metadata) : log.details || ''
    };
  }

  // ================================================================
  // PUBLIC API – FETCH FULL DISPUTE
  // ================================================================

  /**
   * Fetch the full dispute details by ID and update the cached list.
   * Returns the enriched Dispute object.
   */
  async fetchFullDispute(disputeId: string): Promise<Dispute | null> {
    try {
      const response = await firstValueFrom(
        this.apiService.getDisputeById(disputeId).pipe(catchError(() => of(null)))
      );
      if (!response) return null;

      // The response might be the object directly or wrapped in 'data'
      const raw = response.data ?? response;
      const users = this.usersSignal();
      const transactions = this.transactionsSignal();
      const full = this.mapApiDisputeFull(raw, users, transactions);

      // Update the cached disputes list
      const current = this.disputesSignal();
      const index = current.findIndex(d => d.id === disputeId);
      if (index !== -1) {
        current[index] = full;
        this.disputesSignal.set([...current]);
      }

      return full;
    } catch (error) {
      console.error('Error fetching full dispute:', error);
      return null;
    }
  }

  // ================================================================
  // ALL OTHER PUBLIC METHODS (unchanged)
  // ================================================================

  // ---- DASHBOARD HELPERS ----
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

  // ---- USER MANAGEMENT ----
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
      kycApproved: 0,
      blacklisted: users.filter(u => u.blacklistStatus === 'PERMANENTLY_BANNED').length
    };
  }

  // ---- TRANSACTION MANAGEMENT ----
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

  // ---- DISPUTE MANAGEMENT ----
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
      const transaction = this.transactionsSignal().find(t => t.id === dispute.transactionId);
      if (transaction && transaction.status !== 'COMPLETED') {
        transaction.status = 'REFUNDED';
        transaction.completedAt = new Date().toISOString().split('T')[0];
        this.transactionsSignal.set([...this.transactionsSignal()]);
      }
      this.disputesSignal.set([...disputes]);
      this.addAuditLog('RESOLVE_DISPUTE', disputeId, `Refunded KES ${dispute.amount.toLocaleString()} to buyer`);
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
      const transaction = this.transactionsSignal().find(t => t.id === dispute.transactionId);
      if (transaction && transaction.status !== 'COMPLETED') {
        transaction.status = 'COMPLETED';
        transaction.completedAt = new Date().toISOString().split('T')[0];
        this.transactionsSignal.set([...this.transactionsSignal()]);
      }
      this.disputesSignal.set([...disputes]);
      this.addAuditLog('RESOLVE_DISPUTE', disputeId, `Released KES ${dispute.amount.toLocaleString()} to seller`);
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
      const transaction = this.transactionsSignal().find(t => t.id === dispute.transactionId);
      if (transaction) {
        transaction.status = 'COMPLETED';
        transaction.completedAt = new Date().toISOString().split('T')[0];
        this.transactionsSignal.set([...this.transactionsSignal()]);
      }
      this.disputesSignal.set([...disputes]);
      this.addAuditLog('RESOLVE_DISPUTE', disputeId, `Partial settlement of KES ${partialAmount.toLocaleString()}`);
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

  // ---- AUDIT LOG ----
  private addAuditLog(action: string, target: string, details: string): void {
    const newLog: AuditLog = {
      timestamp: new Date().toISOString().replace('T', ' ').substring(0, 19),
      admin: 'admin@escrowx.com',
      action,
      target,
      details
    };
    this.auditLogsSignal.set([newLog, ...this.auditLogsSignal()].slice(0, 500));
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

  getAuditLogStats() {
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
    this.addAuditLog('CLEAR_AUDIT_LOGS', 'system', 'All audit logs cleared');
    this.notificationService.add('Audit Logs Cleared', 'All audit logs have been cleared', 'warning');
  }

  // ---- CHART DATA ----
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

  getTransactionVolumeByDay(days: number = 30): { labels: string[]; data: number[] } {
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

  getDisputeTrendByWeek(weeks: number = 12): { labels: string[]; data: number[] } {
    const labels: string[] = [];
    const data: number[] = [];
    for (let i = weeks - 1; i >= 0; i--) {
      labels.push(`W${Math.floor(i / 4) + 1}`);
      data.push(Math.floor(Math.random() * 8) + 1);
    }
    return { labels, data };
  }

  getUserGrowth(): { labels: string[]; data: number[] } {
    return {
      labels: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun'],
      data: [45, 78, 120, 189, 245, 312]
    };
  }

  getTransactionStatusDistribution(): { labels: string[]; data: number[] } {
    const stats = this.getTransactionStats();
    return {
      labels: ['Completed', 'Funds Held', 'Disputed', 'Refunded', 'Cancelled'],
      data: [stats.completed, stats.fundsHeld, stats.disputed, stats.refunded, stats.cancelled || 0]
    };
  }

  getTopSellers(limit: number = 5): { name: string; volume: number; transactions: number }[] {
    const sellers = new Map<string, { volume: number; transactions: number }>();
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

  // ================================================================
  // WAIT FOR DATA – Added for components that need to wait for data
  // ================================================================

  /**
   * Check if data has been loaded (either from mock or API)
   */
  hasData(): boolean {
    return this.transactionsSignal().length > 0 || this.usersSignal().length > 0;
  }

  /**
   * Wait for data to be loaded (useful for components that need data immediately)
   */
  async waitForData(): Promise<void> {
    // If data is already loaded, return immediately
    if (this.hasData()) {
      return;
    }

    // If data is loading, wait for it
    return new Promise((resolve) => {
      const checkInterval = setInterval(() => {
        if (this.hasData()) {
          clearInterval(checkInterval);
          resolve();
        }
      }, 100);

      // Timeout after 10 seconds to prevent infinite waiting
      setTimeout(() => {
        clearInterval(checkInterval);
        resolve();
      }, 10000);
    });
  }
}
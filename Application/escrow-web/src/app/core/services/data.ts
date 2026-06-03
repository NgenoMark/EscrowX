import { Injectable, inject, signal } from '@angular/core';
import { Transaction } from '../models/transaction';
import { User } from '../models/user';
import { Dispute } from '../models/dispute';
import { NotificationService } from './notifications';

// Define AuditLog interface
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
  private notificationService = inject(NotificationService);

  // Using signals for reactive state management
  private transactionsSignal = signal<Transaction[]>([]);
  private usersSignal = signal<User[]>([]);
  private disputesSignal = signal<Dispute[]>([]);
  
  // Audit logs
  private auditLogsSignal = signal<AuditLog[]>([]);

  // Expose readonly signals to components
  readonly transactions = this.transactionsSignal.asReadonly();
  readonly users = this.usersSignal.asReadonly();
  readonly disputes = this.disputesSignal.asReadonly();
  readonly auditLogs = this.auditLogsSignal.asReadonly();

  constructor() {
    this.loadMockData();
    this.initializeSampleAuditLogs();
  }

  private initializeSampleAuditLogs(): void {
    // Add sample audit logs if empty
    if (this.auditLogsSignal().length === 0) {
      const sampleLogs: AuditLog[] = [
        { timestamp: new Date().toISOString().replace('T', ' ').substring(0, 19), admin: 'admin@escrowx.com', action: 'APPROVE_KYC', target: 'Mercy Achieng', details: 'Approved KYC for seller Mercy Achieng' },
        { timestamp: new Date(Date.now() - 86400000).toISOString().replace('T', ' ').substring(0, 19), admin: 'admin@escrowx.com', action: 'FORCE_RELEASE', target: 'ESC-TX1005', details: 'Released KES 18,800 to seller' },
        { timestamp: new Date(Date.now() - 172800000).toISOString().replace('T', ' ').substring(0, 19), admin: 'admin@escrowx.com', action: 'SUSPEND_USER', target: 'Peter Omondi', details: 'Suspended user Peter Omondi' },
        { timestamp: new Date(Date.now() - 259200000).toISOString().replace('T', ' ').substring(0, 19), admin: 'admin@escrowx.com', action: 'RESOLVE_DISPUTE', target: 'DSP-101', details: 'Resolved: Refunded KES 23,000 to buyer' },
        { timestamp: new Date(Date.now() - 345600000).toISOString().replace('T', ' ').substring(0, 19), admin: 'admin@escrowx.com', action: 'BLACKLIST_USER', target: 'peter@fraud.com', details: 'Blacklisted user Peter Omondi' }
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

    // Mock users with OTP verification
    this.usersSignal.set([
      {
        id: 1, name: "John Kamau", phone: "+254712345678", email: "john@example.com",
        role: "BUYER", status: "ACTIVE", isPhoneVerified: true, isEmailVerified: true,
        kycStatus: "APPROVED", blacklisted: false, registrationDate: "2025-01-15",
        totalTransactions: 12, totalVolume: 78500
      },
      {
        id: 2, name: "Mercy Achieng", phone: "+254722987654", email: "mercy@seller.com",
        role: "SELLER", status: "ACTIVE", isPhoneVerified: true, isEmailVerified: true,
        kycStatus: "APPROVED", blacklisted: false, registrationDate: "2025-02-10",
        totalTransactions: 45, totalVolume: 342000
      },
      {
        id: 3, name: "Peter Omondi", phone: "+254799887766", email: "peter@fraud.com",
        role: "SELLER", status: "SUSPENDED", isPhoneVerified: true, isEmailVerified: false,
        kycStatus: "REJECTED", blacklisted: true, registrationDate: "2025-01-20",
        totalTransactions: 3, totalVolume: 15700
      },
      {
        id: 4, name: "Zahra Hassan", phone: "+254700112233", email: "zahra@buyer.com",
        role: "BUYER", status: "ACTIVE", isPhoneVerified: true, isEmailVerified: true,
        kycStatus: "PENDING", blacklisted: false, registrationDate: "2025-03-05",
        totalTransactions: 8, totalVolume: 45200
      },
      {
        id: 5, name: "Samuel Njoroge", phone: "+254744556677", email: "samuel@shop.com",
        role: "SELLER", status: "ACTIVE", isPhoneVerified: true, isEmailVerified: true,
        kycStatus: "APPROVED", blacklisted: false, registrationDate: "2025-02-20",
        totalTransactions: 28, totalVolume: 234000
      },
      {
        id: 6, name: "Grace Adhiambo", phone: "+254711223344", email: "grace@designs.com",
        role: "SELLER", status: "ACTIVE", isPhoneVerified: true, isEmailVerified: true,
        kycStatus: "SUBMITTED", blacklisted: false, registrationDate: "2025-03-15",
        totalTransactions: 6, totalVolume: 42300
      }
    ]);

    // Mock disputes
    this.disputesSignal.set([
      { 
        id: "DSP-101", txId: "ESC-TX1003", raisedBy: "Peter Omondi", raisedByRole: "BUYER",
        against: "Faith Wanjiku", reason: "Item never delivered after payment",
        description: "I paid KES 23,000 for a laptop on May 20th.",
        evidence: ["screenshot_wa.png", "payment_receipt.jpg"], status: "PENDING", amount: 23000, createdAt: "2025-05-26"
      },
      { 
        id: "DSP-102", txId: "ESC-TX1006", raisedBy: "Brian Odhiambo", raisedByRole: "BUYER",
        against: "Peter Omondi", reason: "Received counterfeit phone",
        description: "The phone I received is clearly counterfeit.",
        evidence: ["counterfeit_photo.jpg"], status: "PENDING", amount: 9200, createdAt: "2025-05-24"
      },
      { 
        id: "DSP-103", txId: "ESC-TX1001", raisedBy: "Mercy Achieng", raisedByRole: "SELLER",
        against: "John Kamau", reason: "Buyer confirmed then disputed",
        description: "The buyer confirmed receipt then opened a dispute.",
        evidence: ["delivery_proof.png"], status: "UNDER_REVIEW", amount: 12500, createdAt: "2025-05-28"
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

  suspendUser(userId: number): void {
    const users = this.usersSignal();
    const user = users.find(u => u.id === userId);
    if (user && user.status === 'ACTIVE') {
      user.status = 'SUSPENDED';
      this.usersSignal.set([...users]);
      this.addAuditLog('SUSPEND_USER', user.email, `Suspended user ${user.name}`);
    }
  }

  activateUser(userId: number): void {
    const users = this.usersSignal();
    const user = users.find(u => u.id === userId);
    if (user && user.status === 'SUSPENDED' && !user.blacklisted) {
      user.status = 'ACTIVE';
      this.usersSignal.set([...users]);
      this.addAuditLog('ACTIVATE_USER', user.email, `Activated user ${user.name}`);
    }
  }

  approveKYC(userId: number): void {
    const users = this.usersSignal();
    const user = users.find(u => u.id === userId);
    if (user && user.role === 'SELLER' && user.kycStatus !== 'APPROVED') {
      user.kycStatus = 'APPROVED';
      this.usersSignal.set([...users]);
      this.addAuditLog('APPROVE_KYC', user.email, `Approved KYC for seller ${user.name}`);
    }
  }

  rejectKYC(userId: number): void {
    const users = this.usersSignal();
    const user = users.find(u => u.id === userId);
    if (user && user.role === 'SELLER' && user.kycStatus !== 'REJECTED') {
      user.kycStatus = 'REJECTED';
      this.usersSignal.set([...users]);
      this.addAuditLog('REJECT_KYC', user.email, `Rejected KYC for seller ${user.name}`);
    }
  }

  blacklistUser(userId: number): void {
    const users = this.usersSignal();
    const user = users.find(u => u.id === userId);
    if (user && !user.blacklisted) {
      user.blacklisted = true;
      user.status = 'SUSPENDED';
      this.usersSignal.set([...users]);
      this.addAuditLog('BLACKLIST_USER', user.email, `Blacklisted user ${user.name}`);
    }
  }

  removeBlacklist(userId: number): void {
    const users = this.usersSignal();
    const user = users.find(u => u.id === userId);
    if (user && user.blacklisted) {
      user.blacklisted = false;
      user.status = 'ACTIVE';
      this.usersSignal.set([...users]);
      this.addAuditLog('REMOVE_BLACKLIST', user.email, `Removed blacklist for ${user.name}`);
    }
  }

  getFilteredUsers(searchTerm: string = ''): User[] {
    const users = this.usersSignal();
    if (!searchTerm) return users;
    const term = searchTerm.toLowerCase();
    return users.filter(user =>
      user.name.toLowerCase().includes(term) ||
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
      phoneVerified: users.filter(u => u.isPhoneVerified === true).length,
      kycApproved: users.filter(u => u.kycStatus === 'APPROVED').length,
      blacklisted: users.filter(u => u.blacklisted === true).length
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
    }
  }

  cancelTransaction(transactionId: string): void {
    const transactions = this.transactionsSignal();
    const transaction = transactions.find(t => t.id === transactionId);
    
    if (transaction && transaction.status === 'FUNDS_HELD') {
      transaction.status = 'CANCELLED';
      this.transactionsSignal.set([...transactions]);
      this.addAuditLog('CANCEL_TRANSACTION', transactionId, `Transaction cancelled by admin`);
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
    }
  }

  updateDisputeStatus(disputeId: string, status: 'PENDING' | 'UNDER_REVIEW' | 'ESCALATED'): void {
    const disputes = this.disputesSignal();
    const dispute = disputes.find(d => d.id === disputeId);
    
    if (dispute) {
      dispute.status = status;
      this.disputesSignal.set([...disputes]);
      this.addAuditLog('UPDATE_DISPUTE_STATUS', disputeId, `Status changed to ${status}`);
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
    this.notificationService.add(action.replace(/_/g, ' '), `${target}: ${details}`, this.getNotificationTone(action));
  }

  private getNotificationTone(action: string): 'info' | 'success' | 'warning' | 'danger' {
    if (action.includes('REJECT') || action.includes('BLACKLIST') || action.includes('REFUND') || action.includes('CLEAR')) return 'danger';
    if (action.includes('SUSPEND') || action.includes('ESCALATED')) return 'warning';
    if (action.includes('APPROVE') || action.includes('RELEASE') || action.includes('ACTIVATE') || action.includes('RESOLVE')) return 'success';
    return 'info';
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
  }
}

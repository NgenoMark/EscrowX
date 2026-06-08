import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Dispute } from '../models/dispute';
import { Transaction } from '../models/transaction';
import { User } from '../models/user';

// ========== RESPONSE INTERFACES ==========

/**
 * Standard API response wrapper.
 * Responsible for checking response.success before using data and extracting payload from response.data.
 */
export interface ApiResponse<T> {
  success: boolean;   // Indicates if operation succeeded
  message: string;    // User-friendly message (e.g., "User suspended successfully")
  data: T;            // Actual payload (user, transaction, etc.)
  timestamp: string;  // Server timestamp of response
}

/**
 * Paginated response from backend.
 * Used for endpoints that return lists with pagination metadata.
 */
export interface PageResponse<T> {
  content: T[];         // Array of items for current page
  totalElements: number;// Total count across all pages
  totalPages: number;   // Total number of pages
  size: number;         // Items per page
  number: number;       // Current page index (0‑based)
  first: boolean;       // True if this is the first page
  last: boolean;        // True if this is the last page
}

export interface ApiEscrowTransaction {
  id: string;
  reference: string;
  buyerId: string;
  sellerId: string;
  title: string;
  productDescription?: string | null;
  amount: number;
  initialDepositAmount?: number | null;
  currency: string;
  status: Transaction['status'];
  deliveryDueAt?: string | null;
  autoReleaseAt?: string | null;
  createdAt: string;
  updatedAt?: string | null;
}

export interface ApiUserDetails {
  id: string;
  phone: string;
  email: string;
  role: User['role'];
  status: User['status'];
  blacklistStatus?: User['blacklistStatus'];
  displayName?: string | null;
  businessName?: string | null;
  avatarUrl?: string | null;
  createdAt: string;
  updatedAt?: string | null;
}

// ========== API SERVICE ==========

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private http = inject(HttpClient);
  private apiUrl = environment.apiUrl;

  // ========== AUTHENTICATION ==========

  /**
   * Login with email and password.
   * @param credentials - Object containing email and password
   * @returns Observable emitting ApiResponse containing auth token and user data
   */
  login(credentials: { email: string; password: string }): Observable<ApiResponse<{ token: string; user: User }>> {
    return this.http.post<ApiResponse<{ token: string; user: User }>>(`${this.apiUrl}/auth/login`, credentials);
  }

  // ========== USER MANAGEMENT ==========

  /**
   * Get paginated list of users with optional search.
   * @param params - Optional pagination (page, size) and search term
   * @returns Observable emitting PageResponse containing array of Users and pagination metadata
   */
  getUsers(params?: { page?: number; size?: number; phone?: string; role?: string; status?: string }): Observable<PageResponse<ApiUserDetails>> {
    let httpParams = new HttpParams();
    if (params?.page !== undefined) httpParams = httpParams.set('page', params.page);
    if (params?.size) httpParams = httpParams.set('size', params.size);
    if (params?.phone) httpParams = httpParams.set('phone', params.phone);
    if (params?.role) httpParams = httpParams.set('role', params.role);
    if (params?.status) httpParams = httpParams.set('status', params.status);
    return this.http.get<PageResponse<ApiUserDetails>>(`${this.apiUrl}/users`, { params: httpParams });
  }

  /**
   * Get a single user by ID.
   * @param id - User ID
   * @returns Observable emitting ApiResponse containing the User
   */
  getUserById(id: string): Observable<ApiUserDetails> {
    return this.http.get<ApiUserDetails>(`${this.apiUrl}/users/${id}`);
  }

  /**
   * Suspend a user (admin action).
   * @param userId - ID of user to suspend
   * @returns Observable emitting ApiResponse with updated User
   */
  suspendUser(userId: string, actorUserId?: string): Observable<User> {
    return this.updateUserStatus(userId, 'SUSPENDED', actorUserId);
  }

  /**
   * Activate a suspended user.
   * @param userId - ID of user to activate
   * @returns Observable emitting ApiResponse with updated User
   */
  activateUser(userId: string, actorUserId?: string): Observable<User> {
    return this.updateUserStatus(userId, 'ACTIVE', actorUserId);
  }

  /**
   * Approve KYC verification for a user.
   * @param userId - ID of user
   * @returns Observable emitting ApiResponse with updated User
   */
  approveKYC(userId: number): Observable<ApiResponse<User>> {
    return this.http.put<ApiResponse<User>>(`${this.apiUrl}/users/${userId}/kyc/approve`, {});
  }

  /**
   * Reject KYC verification with optional reason.
   * @param userId - ID of user
   * @param reason - Optional rejection reason
   * @returns Observable emitting ApiResponse with updated User
   */
  rejectKYC(userId: number, reason?: string): Observable<ApiResponse<User>> {
    return this.http.put<ApiResponse<User>>(`${this.apiUrl}/users/${userId}/kyc/reject`, { reason });
  }

  /**
   * Blacklist a user (prevents platform access).
   * @param userId - ID of user to blacklist
   * @returns Observable emitting ApiResponse with updated User
   */
  blacklistUser(userId: string, actorUserId?: string): Observable<User> {
    return this.updateUserStatus(userId, 'BLACKLISTED', actorUserId);
  }

  /**
   * Remove user from blacklist.
   * @param userId - ID of user
   * @returns Observable emitting ApiResponse with updated User
   */
  removeBlacklist(userId: string, actorUserId?: string): Observable<User> {
    return this.updateUserStatus(userId, 'PENDING_VERIFICATION', actorUserId);
  }

  // ========== TRANSACTION MANAGEMENT ==========

  /**
   * Get list of transactions with optional filters and pagination.
   * @param params - Status filter, pagination (page, limit), search term
   * @returns Observable emitting ApiResponse containing array of Transactions
   */
  getTransactions(params?: { role?: string; status?: string; userId?: string; dateFrom?: string; dateTo?: string; page?: number; size?: number }): Observable<PageResponse<ApiEscrowTransaction>> {
    let httpParams = new HttpParams();
    if (params?.role) httpParams = httpParams.set('role', params.role);
    if (params?.status) httpParams = httpParams.set('status', params.status);
    if (params?.userId) httpParams = httpParams.set('userId', params.userId);
    if (params?.dateFrom) httpParams = httpParams.set('dateFrom', params.dateFrom);
    if (params?.dateTo) httpParams = httpParams.set('dateTo', params.dateTo);
    if (params?.page !== undefined) httpParams = httpParams.set('page', params.page);
    if (params?.size) httpParams = httpParams.set('size', params.size);
    return this.http.get<PageResponse<ApiEscrowTransaction>>(`${this.apiUrl}/transactions`, { params: httpParams });
  }

  /**
   * Get a single transaction by ID.
   * @param id - Transaction ID
   * @returns Observable emitting ApiResponse containing the Transaction
   */
  getTransactionById(id: string): Observable<ApiEscrowTransaction> {
    return this.http.get<ApiEscrowTransaction>(`${this.apiUrl}/transactions/${id}`);
  }

  /**
   * Admin: Force release funds held in escrow for a transaction.
   * @param transactionId - ID of transaction
   * @returns Observable emitting ApiResponse with updated Transaction
   */
  forceReleaseFunds(transactionId: string): Observable<ApiEscrowTransaction> {
    return this.http.post<ApiEscrowTransaction>(`${this.apiUrl}/transactions/${transactionId}/confirm-receipt`, {});
  }

  /**
   * Admin: Force refund to buyer.
   * @param transactionId - ID of transaction
   * @returns Observable emitting ApiResponse with updated Transaction
   */
  forceRefund(transactionId: string): Observable<ApiResponse<Transaction>> {
    return this.http.post<ApiResponse<Transaction>>(`${this.apiUrl}/admin/transactions/${transactionId}/force-refund`, {});
  }

  /**
   * Admin: Cancel a transaction.
   * @param transactionId - ID of transaction
   * @returns Observable emitting ApiResponse with updated Transaction
   */
  cancelTransaction(transactionId: string): Observable<ApiEscrowTransaction> {
    return this.http.post<ApiEscrowTransaction>(`${this.apiUrl}/transactions/${transactionId}/cancel`, {});
  }

  updateUserStatus(userId: string, status: User['status'], actorUserId?: string): Observable<User> {
    const headers = actorUserId ? new HttpHeaders({ 'X-Actor-User-Id': actorUserId }) : undefined;
    return this.http.patch<User>(`${this.apiUrl}/users/${userId}/status`, { status }, { headers });
  }

  // ========== DISPUTE MANAGEMENT ==========

  /**
   * Get list of disputes with optional status filter and pagination.
   * @param params - Status, page, limit
   * @returns Observable emitting ApiResponse containing array of Disputes
   */
  getDisputes(params?: { status?: string; page?: number; limit?: number }): Observable<ApiResponse<Dispute[]>> {
    let httpParams = new HttpParams();
    if (params?.status) httpParams = httpParams.set('status', params.status);
    if (params?.page) httpParams = httpParams.set('page', params.page);
    if (params?.limit) httpParams = httpParams.set('limit', params.limit);
    return this.http.get<ApiResponse<Dispute[]>>(`${this.apiUrl}/disputes`, { params: httpParams });
  }

  /**
   * Get a single dispute by ID.
   * @param id - Dispute ID
   * @returns Observable emitting ApiResponse containing the Dispute
   */
  getDisputeById(id: string): Observable<ApiResponse<Dispute>> {
    return this.http.get<ApiResponse<Dispute>>(`${this.apiUrl}/disputes/${id}`);
  }

  /**
   * Admin: Resolve dispute by refunding the buyer.
   * @param disputeId - ID of dispute
   * @returns Observable emitting ApiResponse with updated Dispute
   */
  resolveDisputeRefund(disputeId: string): Observable<ApiResponse<Dispute>> {
    return this.http.post<ApiResponse<Dispute>>(`${this.apiUrl}/admin/disputes/${disputeId}/resolve/refund`, {});
  }

  /**
   * Admin: Resolve dispute by releasing funds to seller.
   * @param disputeId - ID of dispute
   * @returns Observable emitting ApiResponse with updated Dispute
   */
  resolveDisputeRelease(disputeId: string): Observable<ApiResponse<Dispute>> {
    return this.http.post<ApiResponse<Dispute>>(`${this.apiUrl}/admin/disputes/${disputeId}/resolve/release`, {});
  }

  /**
   * Admin: Resolve dispute with partial refund (split amount).
   * @param disputeId - ID of dispute
   * @param partialAmount - Amount to refund to buyer (rest goes to seller)
   * @param notes - Admin notes explaining decision
   * @returns Observable emitting ApiResponse with updated Dispute
   */
  resolveDisputePartial(disputeId: string, partialAmount: number, notes: string): Observable<ApiResponse<Dispute>> {
    return this.http.post<ApiResponse<Dispute>>(`${this.apiUrl}/admin/disputes/${disputeId}/resolve/partial`, { partialAmount, notes });
  }

  /**
   * Admin: Manually update dispute status.
   * @param disputeId - ID of dispute
   * @param status - New status (e.g., 'RESOLVED', 'ESCALATED')
   * @returns Observable emitting ApiResponse with updated Dispute
   */
  updateDisputeStatus(disputeId: string, status: string): Observable<ApiResponse<Dispute>> {
    return this.http.put<ApiResponse<Dispute>>(`${this.apiUrl}/admin/disputes/${disputeId}/status`, { status });
  }

  /**
   * Admin: Add internal note to a dispute.
   * @param disputeId - ID of dispute
   * @param note - Note text
   * @returns Observable emitting ApiResponse with updated Dispute
   */
  addAdminNote(disputeId: string, note: string): Observable<ApiResponse<Dispute>> {
    return this.http.post<ApiResponse<Dispute>>(`${this.apiUrl}/admin/disputes/${disputeId}/notes`, { note });
  }

  // ========== ANALYTICS ==========

  /**
   * Get dashboard statistics (e.g., total users, transaction volume, dispute count).
   * @returns Observable emitting ApiResponse with analytics data (structure may vary)
   */
  getDashboardStats(): Observable<ApiResponse<any>> {
    return this.http.get<ApiResponse<any>>(`${this.apiUrl}/analytics/dashboard`);
  }

  /**
   * Get transaction volume over time (for charts).
   * @param days - Number of days to include (optional)
   * @returns Observable emitting ApiResponse with labels (dates) and data (volume amounts)
   */
  getTransactionVolume(days?: number): Observable<ApiResponse<{ labels: string[]; data: number[] }>> {
    const params = days ? new HttpParams().set('days', days) : undefined;
    return this.http.get<ApiResponse<{ labels: string[]; data: number[] }>>(`${this.apiUrl}/analytics/volume`, { params });
  }

  /**
   * Get dispute trend over time.
   * @param weeks - Number of weeks to include (optional)
   * @returns Observable emitting ApiResponse with labels (weeks) and data (dispute counts)
   */
  getDisputeTrend(weeks?: number): Observable<ApiResponse<{ labels: string[]; data: number[] }>> {
    const params = weeks ? new HttpParams().set('weeks', weeks) : undefined;
    return this.http.get<ApiResponse<{ labels: string[]; data: number[] }>>(`${this.apiUrl}/analytics/disputes`, { params });
  }

  /**
   * Get user registration growth over time.
   * @returns Observable emitting ApiResponse with labels (dates) and data (new user counts)
   */
  getUserGrowth(): Observable<ApiResponse<{ labels: string[]; data: number[] }>> {
    return this.http.get<ApiResponse<{ labels: string[]; data: number[] }>>(`${this.apiUrl}/analytics/users/growth`);
  }

  /**
   * Get top sellers by transaction volume.
   * @param limit - Number of top sellers to return (optional)
   * @returns Observable emitting ApiResponse with array of seller objects (name, volume, transactions)
   */
  getTopSellers(limit?: number): Observable<ApiResponse<{ name: string; volume: number; transactions: number }[]>> {
    const params = limit ? new HttpParams().set('limit', limit) : undefined;
    return this.http.get<ApiResponse<{ name: string; volume: number; transactions: number }[]>>(`${this.apiUrl}/analytics/top-sellers`, { params });
  }

  // ========== AUDIT LOGS ==========

  /**
   * Get audit logs with filters and pagination.
   * @param params - Action type, pagination (page, limit), date range (startDate, endDate)
   * @returns Observable emitting ApiResponse containing array of audit log entries
   */
  getAuditLogs(params?: { action?: string; page?: number; limit?: number; startDate?: string; endDate?: string }): Observable<ApiResponse<any[]>> {
    let httpParams = new HttpParams();
    if (params?.action) httpParams = httpParams.set('action', params.action);
    if (params?.page) httpParams = httpParams.set('page', params.page);
    if (params?.limit) httpParams = httpParams.set('limit', params.limit);
    if (params?.startDate) httpParams = httpParams.set('startDate', params.startDate);
    if (params?.endDate) httpParams = httpParams.set('endDate', params.endDate);
    return this.http.get<ApiResponse<any[]>>(`${this.apiUrl}/audit-logs`, { params: httpParams });
  }

  // ========== AUTH HEADER HELPER ==========

  /**
   * Helper method to manually create Authorization header.
   * (Typically an HTTP interceptor would handle this automatically.)
   * @returns HttpHeaders with Bearer token from localStorage
   */
  getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('access_token');
    return new HttpHeaders().set('Authorization', `Bearer ${token}`);
  }
}

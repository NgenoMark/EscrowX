import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AppEnvironmentService } from '../config/app-environment';

// ========== RESPONSE INTERFACES ==========

/**
 * Responsible for checking response.success before using data and extracting payload from response.data.
 */
export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

/**
 * Paginated response from backend.
 * Used for endpoints that return lists with pagination metadata.
 */
export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

// ========== API-SPECIFIC DOMAIN MODELS ==========

/**
 * Escrow Transaction as returned by the API.
 * This matches the backend's transaction response structure.
 */
export interface ApiEscrowTransaction {
  id: string;
  reference?: string;
  buyerId: string;
  sellerId: string;
  amount: number;
  initialDepositAmount?: number | null;
  currency?: string;
  status: string;
  title?: string;
  productDescription?: string | null;
  createdAt: string;
  updatedAt?: string | null;
  completedAt?: string | null;
  deliveryDueAt?: string | null;
  autoReleaseAt?: string | null;
  disputeId?: string | null;
}

/**
 * User details as returned by the API.
 * This matches the backend's user response structure.
 */
export interface ApiUserDetails {
  id: string;
  phone: string;
  email: string;
  role: string;
  status: string;
  blacklistStatus?: string | null;
  displayName?: string;
  businessName?: string | null;
  avatarUrl?: string | null;
  createdAt: string;
  updatedAt?: string | null;
}

// ========== LEGACY DOMAIN MODELS (for backward compatibility) ==========

export interface Transaction {
  id: string;
  buyerId: number;
  sellerId: number;
  amount: number;
  status: string;
  description: string;
  createdAt: string;
  autoReleaseDate: string;
}

export interface User {
  id: String;
  name: string;
  phone: string;
  email: string;
  role: string;
  status: string;
  blacklistStatus: boolean;
  businessName: string;
  registrationDate: string;
}

export interface Dispute {
  id: string;
  transactionId: string;
  raisedBy: string;
  raisedByRole: string;
  against: string;
  reason: string;
  description: string;
  evidence: string[];
  status: string;
  amount: number;
  createdAt: string;
}

// ========== API SERVICE ==========

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private http = inject(HttpClient);
  private appEnvironment = inject(AppEnvironmentService);
  private apiUrl = this.appEnvironment.apiUrl;

  // ========== AUTHENTICATION ==========

  /**
   * Login with email and password.
   * @param credentials - Object containing email and password
   * @returns Observable emitting ApiResponse containing auth token and user data
   */
  login(credentials: { email: string; password: string }): Observable<ApiResponse<{ token: string; user: ApiUserDetails }>> {
    return this.http.post<ApiResponse<{ token: string; user: ApiUserDetails }>>(`${this.apiUrl}/auth/login`, credentials);
  }

  // ========== USER MANAGEMENT ==========

  /**
   * Get paginated list of users with optional search.
   * @param params - Optional pagination (page, size) and search term
   * @returns Observable emitting PageResponse containing array of ApiUserDetails and pagination metadata
   */
  getUsers(params?: { page?: number; size?: number; search?: string }): Observable<PageResponse<ApiUserDetails>> {
    let httpParams = new HttpParams();
    if (params?.page !== undefined) httpParams = httpParams.set('page', params.page);
    if (params?.size) httpParams = httpParams.set('size', params.size);
    if (params?.search) httpParams = httpParams.set('search', params.search);
    return this.http.get<PageResponse<ApiUserDetails>>(`${this.apiUrl}/users`, { params: httpParams });
  }

  /**
   * Get a single user by ID.
   * @param id - User ID
   * @returns Observable emitting ApiResponse containing the ApiUserDetails
   */
  getUserById(id: string): Observable<ApiResponse<ApiUserDetails>> {
    return this.http.get<ApiResponse<ApiUserDetails>>(`${this.apiUrl}/users/${id}`);
  }

  /**
   * Suspend a user (admin action).
   * @param userId - ID of user to suspend
   * @returns Observable emitting ApiResponse with updated ApiUserDetails
   */
  suspendUser(userId: string): Observable<ApiResponse<ApiUserDetails>> {
    return this.http.patch<ApiResponse<ApiUserDetails>>(`${this.apiUrl}/users/${userId}/status`, { status: 'SUSPENDED' });
  }

  /**
   * Activate a suspended user.
   * @param userId - ID of user to activate
   * @returns Observable emitting ApiResponse with updated ApiUserDetails
   */
  activateUser(userId: string): Observable<ApiResponse<ApiUserDetails>> {
    return this.http.patch<ApiResponse<ApiUserDetails>>(`${this.apiUrl}/users/${userId}/status`, { status: 'ACTIVE' });
  }

  /**
   * Approve KYC verification for a user.
   * @param userId - ID of user
   * @returns Observable emitting ApiResponse with updated ApiUserDetails
   */
  approveKYC(userId: string): Observable<ApiResponse<ApiUserDetails>> {
    return this.http.put<ApiResponse<ApiUserDetails>>(`${this.apiUrl}/users/${userId}/kyc/approve`, {});
  }

  /**
   * Reject KYC verification with optional reason.
   * @param userId - ID of user
   * @param reason - Optional rejection reason
   * @returns Observable emitting ApiResponse with updated ApiUserDetails
   */
  rejectKYC(userId: string, reason?: string): Observable<ApiResponse<ApiUserDetails>> {
    return this.http.put<ApiResponse<ApiUserDetails>>(`${this.apiUrl}/users/${userId}/kyc/reject`, { reason });
  }

  /**
   * Blacklist a user (prevents platform access).
   * @param userId - ID of user to blacklist
   * @returns Observable emitting ApiResponse with updated ApiUserDetails
   */
  blacklistUser(userId: string): Observable<ApiResponse<ApiUserDetails>> {
    return this.http.patch<ApiResponse<ApiUserDetails>>(`${this.apiUrl}/users/${userId}/blacklist`, {});
  }

  /**
   * Remove user from blacklist.
   * @param userId - ID of user
   * @returns Observable emitting ApiResponse with updated ApiUserDetails
   */
  removeBlacklist(userId: string): Observable<ApiResponse<ApiUserDetails>> {
    return this.http.patch<ApiResponse<ApiUserDetails>>(`${this.apiUrl}/users/${userId}/blacklist`, { blacklistStatus: 'NOT_BLACKLISTED' });
  }

  // ========== TRANSACTION MANAGEMENT ==========

  /**
   * Get paginated list of transactions with optional filters.
   * @param params - Status filter, pagination (page, size), search term
   * @returns Observable emitting PageResponse containing array of ApiEscrowTransaction
   */
  getTransactions(params?: { status?: string; page?: number; size?: number; search?: string }): Observable<PageResponse<ApiEscrowTransaction>> {
    let httpParams = new HttpParams();
    if (params?.status) httpParams = httpParams.set('status', params.status);
    if (params?.page !== undefined) httpParams = httpParams.set('page', params.page);
    if (params?.size) httpParams = httpParams.set('size', params.size);
    if (params?.search) httpParams = httpParams.set('search', params.search);
    return this.http.get<PageResponse<ApiEscrowTransaction>>(`${this.apiUrl}/transactions/search`, { params: httpParams });
  }

  /**
   * Get a single transaction by ID.
   * @param id - Transaction ID
   * @returns Observable emitting ApiResponse containing the ApiEscrowTransaction
   */
  getTransactionById(id: string): Observable<ApiResponse<ApiEscrowTransaction>> {
    return this.http.get<ApiResponse<ApiEscrowTransaction>>(`${this.apiUrl}/transactions/${id}`);
  }

  /**
   * Admin: Force release funds held in escrow for a transaction.
   * @param transactionId - ID of transaction
   * @returns Observable emitting ApiResponse with updated ApiEscrowTransaction
   */
  forceReleaseFunds(transactionId: string): Observable<ApiResponse<ApiEscrowTransaction>> {
    return this.http.post<ApiResponse<ApiEscrowTransaction>>(`${this.apiUrl}/transactions/${transactionId}/confirm-receipt`, {});
  }

  /**
   * Admin: Force refund to buyer.
   * @param transactionId - ID of transaction
   * @returns Observable emitting ApiResponse with updated ApiEscrowTransaction
   */
  forceRefund(transactionId: string): Observable<ApiResponse<ApiEscrowTransaction>> {
    return this.http.post<ApiResponse<ApiEscrowTransaction>>(`${this.apiUrl}/transactions/${transactionId}/cancel`, {});
  }

  /**
   * Admin: Cancel a transaction.
   * @param transactionId - ID of transaction
   * @returns Observable emitting ApiResponse with updated ApiEscrowTransaction
   */
  cancelTransaction(transactionId: string): Observable<ApiResponse<ApiEscrowTransaction>> {
    return this.http.post<ApiResponse<ApiEscrowTransaction>>(`${this.apiUrl}/transactions/${transactionId}/cancel`, {});
  }

  // ========== DISPUTE MANAGEMENT ==========

  /**
   * Get list of disputes with optional status filter and pagination.
   * @param params - Status, page, limit
   * @returns Observable emitting ApiResponse containing array of Disputes
   */
  getDisputes(params?: { status?: string; page?: number; limit?: number }): Observable<ApiResponse<any[]>> {
    let httpParams = new HttpParams();
    if (params?.status) httpParams = httpParams.set('status', params.status);
    if (params?.page) httpParams = httpParams.set('page', params.page);
    if (params?.limit) httpParams = httpParams.set('limit', params.limit);
    return this.http.get<ApiResponse<any[]>>(`${this.apiUrl}/admin/disputes`, { params: httpParams });
  }

  /**
   * Get a single dispute by ID.
   * @param id - Dispute ID
   * @returns Observable emitting ApiResponse containing the Dispute
   */
  getDisputeById(id: string): Observable<ApiResponse<any>> {
    return this.http.get<ApiResponse<any>>(`${this.apiUrl}/disputes/${id}`);
  }

  /**
   * Admin: Resolve dispute by refunding the buyer.
   * @param disputeId - ID of dispute
   * @returns Observable emitting ApiResponse with updated Dispute
   */
  resolveDisputeRefund(disputeId: string): Observable<ApiResponse<any>> {
    return this.http.post<ApiResponse<any>>(`${this.apiUrl}/admin/disputes/${disputeId}/resolve`, { resolution: 'REFUND_BUYER' });
  }

  /**
   * Admin: Resolve dispute by releasing funds to seller.
   * @param disputeId - ID of dispute
   * @returns Observable emitting ApiResponse with updated Dispute
   */
  resolveDisputeRelease(disputeId: string): Observable<ApiResponse<any>> {
    return this.http.post<ApiResponse<any>>(`${this.apiUrl}/admin/disputes/${disputeId}/resolve`, { resolution: 'RELEASE_SELLER' });
  }

  /**
   * Admin: Resolve dispute with partial refund (split amount).
   * @param disputeId - ID of dispute
   * @param partialAmount - Amount to refund to buyer (rest goes to seller)
   * @param notes - Admin notes explaining decision
   * @returns Observable emitting ApiResponse with updated Dispute
   */
  resolveDisputePartial(disputeId: string, partialAmount: number, notes: string): Observable<ApiResponse<any>> {
    return this.http.post<ApiResponse<any>>(`${this.apiUrl}/admin/disputes/${disputeId}/resolve`, { resolution: 'PARTIAL_SETTLEMENT', partialAmount, notes });
  }

  /**
   * Admin: Manually update dispute status.
   * @param disputeId - ID of dispute
   * @param status - New status (e.g., 'RESOLVED', 'ESCALATED')
   * @returns Observable emitting ApiResponse with updated Dispute
   */
  updateDisputeStatus(disputeId: string, status: string): Observable<ApiResponse<any>> {
    return this.http.put<ApiResponse<any>>(`${this.apiUrl}/admin/disputes/${disputeId}/status`, { status });
  }

  /**
   * Admin: Add internal note to a dispute.
   * @param disputeId - ID of dispute
   * @param note - Note text
   * @returns Observable emitting ApiResponse with updated Dispute
   */
  addAdminNote(disputeId: string, note: string): Observable<ApiResponse<any>> {
    return this.http.post<ApiResponse<any>>(`${this.apiUrl}/admin/disputes/${disputeId}/notes`, { note });
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

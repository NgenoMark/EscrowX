// src/app/core/services/api.service.ts

import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AppEnvironmentService } from '../config/app-environment';
import { PaymentIntent } from '../models/payment-intent';
import { TransactionStatusHistory } from '../models/transaction-history';
import { EscrowLedgerEntry } from '../models/escrow-ledger-entry';

// ========== RESPONSE INTERFACES ==========

/**
 * Standard API response wrapper.
 */
export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

/**
 * Paginated response from backend.
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

// ========== FRONTEND DOMAIN MODELS (re-exported for convenience) ==========

export interface User {
  id: string;
  phone: string;
  email: string;
  role: 'BUYER' | 'SELLER' | 'ADMIN' | 'SUPER_ADMIN';
  status: 'PENDING_VERIFICATION' | 'ACTIVE' | 'SUSPENDED' | 'BLACKLISTED';
  blacklistStatus: 'NOT_BLACKLISTED' | 'TEMPORARILY_MUTED' | 'PERMANENTLY_BANNED' | 'UNDER_INVESTIGATION';
  displayName: string;
  businessName: string | null;
  avatarUrl?: string | null;
  createdAt: string;
  updatedAt?: string;
}

// ========== LEGACY DOMAIN MODELS (backward compatibility) ==========

export interface TransactionLegacy {
  id: string;
  buyerId: number;
  sellerId: number;
  amount: number;
  status: string;
  description: string;
  createdAt: string;
  autoReleaseDate: string;
}

export interface UserLegacy {
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

export interface DisputeLegacy {
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

  /**
   * Get the current user ID from localStorage
   * Compatible with AuthService which stores user as 'auth_user'
   */
  private getActorUserId(): string | null {
    // Try to get from localStorage by checking all possible keys
    const possibleKeys = ['actor_user_id', 'user_id', 'userId'];
    for (const key of possibleKeys) {
      const value = localStorage.getItem(key);
      if (value) return value;
    }
    
    // Try to get from the auth_user stored by AuthService
    try {
      const authUserStr = localStorage.getItem('auth_user');
      if (authUserStr) {
        const authUser = JSON.parse(authUserStr);
        if (authUser?.id) return authUser.id;
      }
    } catch (e) {
      console.warn('Could not parse auth_user from localStorage', e);
    }
    
    // Try to get from user stored by other services
    try {
      const userStr = localStorage.getItem('user');
      if (userStr) {
        const user = JSON.parse(userStr);
        if (user?.id) return user.id;
      }
    } catch (e) {
      console.warn('Could not parse user from localStorage', e);
    }
    
    return null;
  }

  /**
   * Get headers with X-Actor-User-Id
   */
  private getHeaders(): HttpHeaders {
    const actorUserId = this.getActorUserId();
    let headers = new HttpHeaders();
    if (actorUserId) {
      headers = headers.set('X-Actor-User-Id', actorUserId);
    }
    return headers;
  }

  /**
   * Get headers with X-Actor-User-Id and additional headers
   */
  private getHeadersWithContentType(): HttpHeaders {
    return this.getHeaders().set('Content-Type', 'application/json');
  }

  // ================================================================
  // AUTHENTICATION
  // ================================================================

  /**
   * Login with email and password.
   */
  login(credentials: { email: string; password: string }): Observable<ApiResponse<{ token: string; user: ApiUserDetails }>> {
    return this.http.post<ApiResponse<{ token: string; user: ApiUserDetails }>>(
      `${this.apiUrl}/auth/login`, 
      credentials,
      { headers: this.getHeadersWithContentType() }
    );
  }

  // ================================================================
  // USER MANAGEMENT
  // ================================================================

  /**
   * Get paginated list of users with optional search.
   */
  getUsers(params?: { page?: number; size?: number; search?: string }): Observable<PageResponse<ApiUserDetails>> {
    let httpParams = new HttpParams();
    if (params?.page !== undefined) httpParams = httpParams.set('page', params.page);
    if (params?.size) httpParams = httpParams.set('size', params.size);
    if (params?.search) httpParams = httpParams.set('search', params.search);
    return this.http.get<PageResponse<ApiUserDetails>>(
      `${this.apiUrl}/users`, 
      { params: httpParams, headers: this.getHeaders() }
    );
  }

  /**
   * Get a single user by ID.
   */
  getUserById(id: string): Observable<ApiResponse<ApiUserDetails>> {
    return this.http.get<ApiResponse<ApiUserDetails>>(
      `${this.apiUrl}/users/${id}`,
      { headers: this.getHeaders() }
    );
  }

  /**
   * Generic method to update user status with optional reason.
   */
  updateUserStatus(userId: string, status: string, reason?: string): Observable<ApiResponse<ApiUserDetails>> {
    const body: any = { status };
    if (reason) body.reason = reason;
    return this.http.patch<ApiResponse<ApiUserDetails>>(
      `${this.apiUrl}/users/${userId}/status`, 
      body,
      { headers: this.getHeadersWithContentType() }
    );
  }

  /**
   * Suspend a user.
   */
  suspendUser(userId: string): Observable<ApiResponse<ApiUserDetails>> {
    return this.updateUserStatus(userId, 'SUSPENDED');
  }

  /**
   * Activate a suspended user.
   */
  activateUser(userId: string): Observable<ApiResponse<ApiUserDetails>> {
    return this.updateUserStatus(userId, 'ACTIVE');
  }

  /**
   * Approve KYC verification.
   */
  approveKYC(userId: string): Observable<ApiResponse<ApiUserDetails>> {
    return this.http.put<ApiResponse<ApiUserDetails>>(
      `${this.apiUrl}/users/${userId}/kyc/approve`, 
      {},
      { headers: this.getHeadersWithContentType() }
    );
  }

  /**
   * Reject KYC verification with optional reason.
   */
  rejectKYC(userId: string, reason?: string): Observable<ApiResponse<ApiUserDetails>> {
    return this.http.put<ApiResponse<ApiUserDetails>>(
      `${this.apiUrl}/users/${userId}/kyc/reject`, 
      { reason },
      { headers: this.getHeadersWithContentType() }
    );
  }

  /**
   * Blacklist a user (permanent ban).
   */
  blacklistUser(userId: string): Observable<ApiResponse<ApiUserDetails>> {
    return this.updateUserStatus(userId, 'BLACKLISTED');
  }

  /**
   * Remove user from blacklist.
   */
  removeBlacklist(userId: string): Observable<ApiResponse<ApiUserDetails>> {
    return this.updateUserStatus(userId, 'PENDING_VERIFICATION', 'Removed from blacklist by admin');
  }

  /**
   * Update a user's role (admin action).
   * @param userId - ID of user
   * @param role - New role (BUYER, SELLER, ADMIN, SUPER_ADMIN)
   * @returns Observable emitting ApiResponse with updated ApiUserDetails
   */
  updateUserRole(userId: string, role: string): Observable<ApiResponse<ApiUserDetails>> {
    return this.http.patch<ApiResponse<ApiUserDetails>>(
      `${this.apiUrl}/users/${userId}/role`, 
      { role },
      { headers: this.getHeadersWithContentType() }
    );
  }

  /**
   * Get paginated list of buyers
   */
  getBuyers(params?: { page?: number; size?: number; search?: string; status?: string }): Observable<PageResponse<ApiUserDetails>> {
    let httpParams = new HttpParams();
    if (params?.page !== undefined) httpParams = httpParams.set('page', params.page);
    if (params?.size) httpParams = httpParams.set('size', params.size);
    if (params?.search) httpParams = httpParams.set('search', params.search);
    if (params?.status) httpParams = httpParams.set('status', params.status);
    return this.http.get<PageResponse<ApiUserDetails>>(
      `${this.apiUrl}/users/buyers`, 
      { params: httpParams, headers: this.getHeaders() }
    );
  }

  /**
   * Get paginated list of sellers
   */
  getSellers(params?: { page?: number; size?: number; search?: string; status?: string }): Observable<PageResponse<ApiUserDetails>> {
    let httpParams = new HttpParams();
    if (params?.page !== undefined) httpParams = httpParams.set('page', params.page);
    if (params?.size) httpParams = httpParams.set('size', params.size);
    if (params?.search) httpParams = httpParams.set('search', params.search);
    if (params?.status) httpParams = httpParams.set('status', params.status);
    return this.http.get<PageResponse<ApiUserDetails>>(
      `${this.apiUrl}/users/sellers`, 
      { params: httpParams, headers: this.getHeaders() }
    );
  }

  /**
   * Get paginated list of employees (admins)
   */
  getEmployees(params?: { page?: number; size?: number; search?: string; status?: string }): Observable<PageResponse<ApiUserDetails>> {
    let httpParams = new HttpParams();
    if (params?.page !== undefined) httpParams = httpParams.set('page', params.page);
    if (params?.size) httpParams = httpParams.set('size', params.size);
    if (params?.search) httpParams = httpParams.set('search', params.search);
    if (params?.status) httpParams = httpParams.set('status', params.status);
    return this.http.get<PageResponse<ApiUserDetails>>(
      `${this.apiUrl}/users/employees`, 
      { params: httpParams, headers: this.getHeaders() }
    );
  }

  /**
   * Approve a seller (KYC verification)
   */
  approveSeller(userId: string, reason?: string): Observable<ApiResponse<{ userId: string; oldValue: string; newValue: string; updatedBy: string; updatedAt: string }>> {
    const body: any = {};
    if (reason) body.reason = reason;
    return this.http.post<ApiResponse<{ userId: string; oldValue: string; newValue: string; updatedBy: string; updatedAt: string }>>(
      `${this.apiUrl}/users/${userId}/approve-seller`,
      body,
      { headers: this.getHeadersWithContentType() }
    );
  }

  /**
   * Create a new Super Admin employee
   */
  createSuperAdmin(employeeData: {
    email: string;
    phone: string;
    password: string;
    displayName: string;
    businessName?: string;
    address?: string;
    avatarUrl?: string;
  }): Observable<ApiResponse<ApiUserDetails>> {
    return this.http.post<ApiResponse<ApiUserDetails>>(
      `${this.apiUrl}/users/employees/super-admin`,
      employeeData,
      { headers: this.getHeadersWithContentType() }
    );
  }

  /**
   * Create a new Admin employee
   */
  createAdmin(employeeData: {
    email: string;
    phone: string;
    password: string;
    displayName: string;
    businessName?: string;
    address?: string;
    avatarUrl?: string;
  }): Observable<ApiResponse<ApiUserDetails>> {
    return this.http.post<ApiResponse<ApiUserDetails>>(
      `${this.apiUrl}/users/employees/admin`,
      employeeData,
      { headers: this.getHeadersWithContentType() }
    );
  }

  // ================================================================
  // TRANSACTION MANAGEMENT
  // ================================================================

  /**
   * Get paginated list of transactions with optional filters.
   */
  getTransactions(params?: { status?: string; page?: number; size?: number; search?: string }): Observable<PageResponse<ApiEscrowTransaction>> {
    let httpParams = new HttpParams();
    if (params?.status) httpParams = httpParams.set('status', params.status);
    if (params?.page !== undefined) httpParams = httpParams.set('page', params.page);
    if (params?.size) httpParams = httpParams.set('size', params.size);
    if (params?.search) httpParams = httpParams.set('search', params.search);
    return this.http.get<PageResponse<ApiEscrowTransaction>>(
      `${this.apiUrl}/transactions/search`, 
      { params: httpParams, headers: this.getHeaders() }
    );
  }

  /**
   * Get a single transaction by ID.
   */
  getTransactionById(id: string): Observable<ApiResponse<ApiEscrowTransaction>> {
    return this.http.get<ApiResponse<ApiEscrowTransaction>>(
      `${this.apiUrl}/transactions/${id}`,
      { headers: this.getHeaders() }
    );
  }

  /**
   * Get the status history for a transaction (using /status-history).
   */
  getTransactionStatusHistory(transactionId: string): Observable<ApiResponse<TransactionStatusHistory[]>> {
    return this.http.get<ApiResponse<TransactionStatusHistory[]>>(
      `${this.apiUrl}/transactions/${transactionId}/status-history`,
      { headers: this.getHeaders() }
    );
  }

  /**
   * Get the status history for a transaction (legacy /history).
   * @deprecated Use getTransactionStatusHistory instead.
   */
  getTransactionHistory(transactionId: string): Observable<ApiResponse<TransactionStatusHistory[]>> {
    return this.http.get<ApiResponse<TransactionStatusHistory[]>>(
      `${this.apiUrl}/transactions/${transactionId}/history`,
      { headers: this.getHeaders() }
    );
  }

  /**
   * Get ledger entries for a transaction.
   */
  getLedgerEntries(transactionId?: string): Observable<ApiResponse<any[]>> {
    let params = new HttpParams();
    if (transactionId) {
      params = params.set('transactionId', transactionId);
    }
    return this.http.get<ApiResponse<any[]>>(
      `${this.apiUrl}/admin/ledger-entries`, 
      { params, headers: this.getHeaders() }
    );
  }

  /**
   * Admin: Force release funds held in escrow.
   */
  forceReleaseFunds(transactionId: string): Observable<ApiResponse<ApiEscrowTransaction>> {
    return this.http.post<ApiResponse<ApiEscrowTransaction>>(
      `${this.apiUrl}/transactions/${transactionId}/confirm-receipt`, 
      {},
      { headers: this.getHeadersWithContentType() }
    );
  }

  /**
   * Admin: Force refund to buyer.
   */
  forceRefund(transactionId: string): Observable<ApiResponse<ApiEscrowTransaction>> {
    return this.http.post<ApiResponse<ApiEscrowTransaction>>(
      `${this.apiUrl}/transactions/${transactionId}/cancel`, 
      {},
      { headers: this.getHeadersWithContentType() }
    );
  }

  /**
   * Admin: Cancel a transaction.
   */
  cancelTransaction(transactionId: string): Observable<ApiResponse<ApiEscrowTransaction>> {
    return this.http.post<ApiResponse<ApiEscrowTransaction>>(
      `${this.apiUrl}/transactions/${transactionId}/cancel`, 
      {},
      { headers: this.getHeadersWithContentType() }
    );
  }

  // ================================================================
  // DISPUTE MANAGEMENT – UPDATED ENDPOINTS
  // ================================================================

  /**
   * Get paginated list of disputes (summary).
   * This matches GET /api/v1/disputes with pageable response.
   */
  getDisputes(params?: { status?: string; page?: number; size?: number }): Observable<PageResponse<any>> {
    let httpParams = new HttpParams();
    if (params?.status) httpParams = httpParams.set('status', params.status);
    if (params?.page !== undefined) httpParams = httpParams.set('page', params.page);
    if (params?.size) httpParams = httpParams.set('size', params.size);
    return this.http.get<PageResponse<any>>(
      `${this.apiUrl}/admin/disputes`, 
      { params: httpParams, headers: this.getHeaders() }
    );
  }

  /**
   * Get full dispute details by ID.
   * This matches GET /api/v1/disputes/{id} and returns the full dispute object.
   */
  getDisputeById(id: string): Observable<any> {
    return this.http.get<any>(
      `${this.apiUrl}/disputes/${id}`,
      { headers: this.getHeaders() }
    );
  }

  // ----- Legacy / Admin endpoints (kept for backward compatibility) -----

  /**
   * @deprecated Use getDisputes() instead.
   */
  getAdminDisputes(params?: { status?: string; page?: number; limit?: number }): Observable<ApiResponse<any[]>> {
    let httpParams = new HttpParams();
    if (params?.status) httpParams = httpParams.set('status', params.status);
    if (params?.page !== undefined) httpParams = httpParams.set('page', params.page);
    if (params?.limit) httpParams = httpParams.set('limit', params.limit);
    return this.http.get<ApiResponse<any[]>>(
      `${this.apiUrl}/admin/disputes`, 
      { params: httpParams, headers: this.getHeaders() }
    );
  }

  /**
   * Resolve dispute by refunding the buyer.
   */
  resolveDisputeRefund(disputeId: string): Observable<ApiResponse<any>> {
    return this.http.post<ApiResponse<any>>(
      `${this.apiUrl}/admin/disputes/${disputeId}/resolve`, 
      { resolution: 'REFUND_BUYER' },
      { headers: this.getHeadersWithContentType() }
    );
  }

  /**
   * Resolve dispute by releasing funds to seller.
   */
  resolveDisputeRelease(disputeId: string): Observable<ApiResponse<any>> {
    return this.http.post<ApiResponse<any>>(
      `${this.apiUrl}/admin/disputes/${disputeId}/resolve`, 
      { resolution: 'RELEASE_SELLER' },
      { headers: this.getHeadersWithContentType() }
    );
  }

  /**
   * Resolve dispute with partial settlement.
   */
  resolveDisputePartial(disputeId: string, partialAmount: number, notes: string): Observable<ApiResponse<any>> {
    return this.http.post<ApiResponse<any>>(
      `${this.apiUrl}/admin/disputes/${disputeId}/resolve`, 
      { resolution: 'PARTIAL_SETTLEMENT', partialAmount, notes },
      { headers: this.getHeadersWithContentType() }
    );
  }

  /**
   * Manually update dispute status.
   */
  updateDisputeStatus(disputeId: string, status: string): Observable<ApiResponse<any>> {
    return this.http.put<ApiResponse<any>>(
      `${this.apiUrl}/admin/disputes/${disputeId}/status`, 
      { status },
      { headers: this.getHeadersWithContentType() }
    );
  }

  /**
   * Add internal note to a dispute.
   */
  addAdminNote(disputeId: string, note: string): Observable<ApiResponse<any>> {
    return this.http.post<ApiResponse<any>>(
      `${this.apiUrl}/admin/disputes/${disputeId}/notes`, 
      { note },
      { headers: this.getHeadersWithContentType() }
    );
  }

  // ================================================================
  // PAYMENT & PAYOUT MANAGEMENT
  // ================================================================

  /**
   * Get payment intents with optional filters.
   */
  getPaymentIntents(params?: { status?: string; page?: number; limit?: number }): Observable<PaymentIntent[]> {
    let httpParams = new HttpParams();
    if (params?.status) httpParams = httpParams.set('status', params.status);
    if (params?.page !== undefined) httpParams = httpParams.set('page', params.page);
    if (params?.limit) httpParams = httpParams.set('limit', params.limit);
    return this.http.get<PaymentIntent[]>(
      `${this.apiUrl}/admin/payment-intents`, 
      { params: httpParams, headers: this.getHeaders() }
    );
  }

  /**
   * Get payouts with optional filters.
   */
  getPayouts(params?: { status?: string; page?: number; limit?: number }): Observable<ApiResponse<any[]>> {
    let httpParams = new HttpParams();
    if (params?.status) httpParams = httpParams.set('status', params.status);
    if (params?.page !== undefined) httpParams = httpParams.set('page', params.page);
    if (params?.limit) httpParams = httpParams.set('limit', params.limit);
    return this.http.get<ApiResponse<any[]>>(
      `${this.apiUrl}/admin/payouts`, 
      { params: httpParams, headers: this.getHeaders() }
    );
  }

  // ================================================================
  // ANALYTICS
  // ================================================================

  /**
   * Get dashboard statistics.
   */
  getDashboardStats(): Observable<ApiResponse<any>> {
    return this.http.get<ApiResponse<any>>(
      `${this.apiUrl}/analytics/dashboard`,
      { headers: this.getHeaders() }
    );
  }

  /**
   * Get transaction volume over time.
   */
  getTransactionVolume(days?: number): Observable<ApiResponse<{ labels: string[]; data: number[] }>> {
    const params = days ? new HttpParams().set('days', days) : undefined;
    return this.http.get<ApiResponse<{ labels: string[]; data: number[] }>>(
      `${this.apiUrl}/analytics/volume`, 
      { params, headers: this.getHeaders() }
    );
  }

  /**
   * Get dispute trend over time.
   */
  getDisputeTrend(weeks?: number): Observable<ApiResponse<{ labels: string[]; data: number[] }>> {
    const params = weeks ? new HttpParams().set('weeks', weeks) : undefined;
    return this.http.get<ApiResponse<{ labels: string[]; data: number[] }>>(
      `${this.apiUrl}/analytics/disputes`, 
      { params, headers: this.getHeaders() }
    );
  }

  /**
   * Get user registration growth over time.
   */
  getUserGrowth(): Observable<ApiResponse<{ labels: string[]; data: number[] }>> {
    return this.http.get<ApiResponse<{ labels: string[]; data: number[] }>>(
      `${this.apiUrl}/analytics/users/growth`,
      { headers: this.getHeaders() }
    );
  }

  /**
   * Get top sellers by transaction volume.
   */
  getTopSellers(limit?: number): Observable<ApiResponse<{ name: string; volume: number; transactions: number }[]>> {
    const params = limit ? new HttpParams().set('limit', limit) : undefined;
    return this.http.get<ApiResponse<{ name: string; volume: number; transactions: number }[]>>(
      `${this.apiUrl}/analytics/top-sellers`, 
      { params, headers: this.getHeaders() }
    );
  }

  // ================================================================
  // AUDIT LOGS
  // ================================================================

  /**
   * Get audit logs with filters and pagination.
   */
  getAuditLogs(params?: { action?: string; page?: number; limit?: number; startDate?: string; endDate?: string }): Observable<ApiResponse<any[]>> {
    let httpParams = new HttpParams();
    if (params?.action) httpParams = httpParams.set('action', params.action);
    if (params?.page !== undefined) httpParams = httpParams.set('page', params.page);
    if (params?.limit) httpParams = httpParams.set('limit', params.limit);
    if (params?.startDate) httpParams = httpParams.set('startDate', params.startDate);
    if (params?.endDate) httpParams = httpParams.set('endDate', params.endDate);
    return this.http.get<ApiResponse<any[]>>(
      `${this.apiUrl}/audit-logs/all-logs`, 
      { params: httpParams, headers: this.getHeaders() }
    );
  }

  // ================================================================
  // AUTH HEADER HELPER
  // ================================================================

  /**
   * Helper method to manually create Authorization header.
   */
  getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('access_token');
    let headers = new HttpHeaders().set('Authorization', `Bearer ${token}`);
    const actorUserId = this.getActorUserId();
    if (actorUserId) {
      headers = headers.set('X-Actor-User-Id', actorUserId);
    }
    return headers;
  }
}
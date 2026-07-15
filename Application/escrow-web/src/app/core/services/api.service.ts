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

// ========== REQUEST/RESPONSE TYPES FOR ADDED ENDPOINTS ==========

export interface CreateEscrowTransactionRequest {
  buyerId: string;
  sellerId: string;
  amount: number;
  description: string;
  deliveryType: 'PHYSICAL' | 'DIGITAL' | 'SERVICE';
  deliveryAddress?: string;
  deliveryTimeline?: string;
}

export interface AssignRiderRequest {
  riderId: string;
}

export interface InitiateStkPushRequest {
  phoneNumber: string;
  amount: number;
}

export interface InitiateStkPushResponse {
  checkoutRequestId: string;
  merchantRequestId: string;
  responseCode: string;
  responseDescription: string;
  paymentId: string;
}

export interface ReleasePayoutResponse {
  paymentId: string;
  escrowId: string;
  amount: number;
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  releaseDate: string;
  transactionReference: string;
}

export interface PaymentResponse {
  id: string;
  escrowId: string;
  amount: number;
  paymentMethod: string;
  status: string;
  transactionReference: string;
  createdAt: string;
  updatedAt: string;
  metadata: any;
}

export interface PayoutFinanceResponse {
  id: string;
  userId: string;
  amount: number;
  status: string;
  paymentMethod: string;
  phoneNumber: string;
  transactionReference: string;
  createdAt: string;
  processedAt: string;
}

export interface PaymentIntentFinanceResponse {
  id: string;
  userId: string;
  amount: number;
  paymentType: string;
  status: string;
  reference: string;
  createdAt: string;
}

export interface UploadImageResponse {
  imageUrl: string;
  publicId: string;
  message: string;
}

export interface RegisterDeviceTokenRequest {
  deviceToken: string;
  platform: 'ANDROID' | 'IOS';
  userId?: string;
}

export interface RegisterDeviceTokenResponse {
  id: string;
  message: string;
}

export interface DisputeCreateRequest {
  transactionId: string;
  raisedBy: 'BUYER' | 'SELLER';
  reason: string;
  description: string;
}

export interface DisputeCreateResponse {
  id: string;
  transactionId: string;
  reference: string;
  status: string;
  raisedBy: string;
  reason: string;
  description: string;
  createdAt: string;
}

export interface DisputeEvidenceUpdateRequest {
  evidenceUrls: string[];
  description?: string;
}

export interface DisputeDetailsResponse {
  id: string;
  transactionId: string;
  reference: string;
  status: string;
  raisedBy: string;
  reason: string;
  description: string;
  evidenceUrls: string[];
  adminId: string | null;
  adminComments: string | null;
  resolution: string | null;
  createdAt: string;
  updatedAt: string;
  resolvedAt: string | null;
}

export interface DisputeCloseRequest {
  reason: string;
}

export interface DisputeResolveRequest {
  resolution: 'REFUND_BUYER' | 'PAY_SELLER' | 'CANCEL_TRANSACTION' | 'PARTIAL_SETTLEMENT';
  partialAmount?: number;
  adminComments?: string;
}

export interface DisputeActionRequiredRequest {
  actionRequired: string;
  adminComments: string;
}

export interface DisputeAssignAdminRequest {
  adminId: string;
}

export interface RiderProfileResponse {
  id: string;
  userId: string;
  fullName: string;
  phoneNumber: string;
  email: string;
  vehicleType: string;
  vehiclePlate: string;
  rating: number;
  totalDeliveries: number;
  isActive: boolean;
  profileImage: string;
  createdAt: string;
}

export interface CreateEmployeeRequest {
  email: string;
  phone: string;
  password: string;
  fullName: string;
  vehicleType?: string;
  vehiclePlate?: string;
}

export interface BlacklistUpdateRequest {
  blacklisted: boolean;
  reason?: string;
}

export interface BlacklistUpdateResponse {
  id: string;
  blacklisted: boolean;
  reason: string;
  updatedAt: string;
}

export interface PayoutReconciliationResultDto {
  processed: number;
  failed: number;
  details: string[];
}

export interface PasswordResetRequestDto {
  email: string;
}

export interface PasswordResetRequestResponse {
  message: string;
  email: string;
}

export interface PasswordResetConfirmRequest {
  token: string;
  newPassword: string;
}

export interface PasswordResetConfirmResponse {
  message: string;
}

export interface InAppNotificationResponse {
  id: string;
  userId: string;
  title: string;
  message: string;
  type: string;
  isRead: boolean;
  isArchived: boolean;
  referenceId: string;
  createdAt: string;
  readAt: string;
}

export interface NotificationStatusUpdateResponse {
  id: string;
  isRead: boolean;
  readAt: string;
  message: string;
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
   * Get headers with X-Actor-User-Id and Content-Type
   */
  private getHeadersWithContentType(): HttpHeaders {
    return this.getHeaders().set('Content-Type', 'application/json');
  }

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

  /**
   * Request password reset.
   */
  requestPasswordReset(email: string): Observable<ApiResponse<PasswordResetRequestResponse>> {
    return this.http.post<ApiResponse<PasswordResetRequestResponse>>(
      `${this.apiUrl}/auth/password-reset/request`,
      { email },
      { headers: this.getHeadersWithContentType() }
    );
  }

  /**
   * Confirm password reset with token and new passsword.
   */
  confirmPasswordReset(email: string, otp: string, newPassword: string): Observable<ApiResponse<{ email: string; passwordUpdated: boolean }>> {
    return this.http.post<ApiResponse<{ email: string; passwordUpdated: boolean }>>(
      `${this.apiUrl}/auth/password-reset/confirm`,
      { email, otp, newPassword },
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
   * Get user by phone.
   */
  getUserByPhone(phone: string): Observable<ApiResponse<ApiUserDetails>> {
    return this.http.get<ApiResponse<ApiUserDetails>>(
      `${this.apiUrl}/users/by-phone/${phone}`,
      { headers: this.getHeaders() }
    );
  }

  /**
   * Get user by email.
   */
  getUserByEmail(email: string): Observable<ApiResponse<ApiUserDetails>> {
    return this.http.get<ApiResponse<ApiUserDetails>>(
      `${this.apiUrl}/users/by-email/${email}`,
      { headers: this.getHeaders() }
    );
  }

  /**
   * Update user profile.
   */
  updateUserProfile(userId: string, data: any): Observable<ApiResponse<any>> {
    return this.http.patch<ApiResponse<any>>(
      `${this.apiUrl}/users/${userId}/update_profile`,
      data,
      { headers: this.getHeadersWithContentType() }
    );
  }

  sendVerificationOtp(userId: string): Observable<ApiResponse<{ message: string }>> {
    return this.http.post<ApiResponse<{ message: string; otpPreview?: string }>>(
      `${this.apiUrl}/admin/users/${userId}/send-verification-otp`, {},
      { headers: this.getHeadersWithContentType() }
    );
  }

  verifyUserOtp(userId: string, otp: string): Observable<ApiResponse<{ message: string; status: string }>> {
    return this.http.post<ApiResponse<{ message: string; status: string }>>(
      `${this.apiUrl}/admin/users/${userId}/verify-otp`,
      { otp },
      { headers: this.getHeadersWithContentType() }
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
   * Blacklist a user (permanent ban) with reason.
   */
  blacklistUserWithReason(userId: string, reason: string): Observable<ApiResponse<BlacklistUpdateResponse>> {
    return this.http.patch<ApiResponse<BlacklistUpdateResponse>>(
      `${this.apiUrl}/users/admin/${userId}/blacklist`,
      { blacklisted: true, reason },
      { headers: this.getHeadersWithContentType() }
    );
  }

  /**
   * Remove user from blacklist.
   */
  unblacklistUser(userId: string): Observable<ApiResponse<BlacklistUpdateResponse>> {
    return this.http.patch<ApiResponse<BlacklistUpdateResponse>>(
      `${this.apiUrl}/users/admin/${userId}/blacklist`,
      { blacklisted: false },
      { headers: this.getHeadersWithContentType() }
    );
  }

  /**
   * Update a user's role (admin action).
   */
  updateUserRole(userId: string, role: string): Observable<ApiResponse<ApiUserDetails>> {
    return this.http.patch<ApiResponse<ApiUserDetails>>(
      `${this.apiUrl}/users/${userId}/role`,
      { role },
      { headers: this.getHeadersWithContentType() }
    );
  }

  /**
   * Get paginated list of buyers.
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
   * Get paginated list of sellers.
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
   * Get paginated list of employees (admins).
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
   * Get paginated list of riders.
   */
  getRiders(params?: { page?: number; size?: number; search?: string }): Observable<PageResponse<ApiUserDetails>> {
    let httpParams = new HttpParams();
    if (params?.page !== undefined) httpParams = httpParams.set('page', params.page);
    if (params?.size) httpParams = httpParams.set('size', params.size);
    if (params?.search) httpParams = httpParams.set('search', params.search);

    // --- Force the header ---
    const actorId = this.getActorUserId();
    let headers = new HttpHeaders()
      .set('X-Actor-User-Id', actorId || '')
      .set('Authorization', `Bearer ${localStorage.getItem('access_token')}`)
      .set('ngrok-skip-browser-warning', 'true');

    console.log('🔑 Forced X-Actor-User-Id:', actorId);

    return this.http.get<PageResponse<ApiUserDetails>>(
      `${this.apiUrl}/users/riders`,
      { params: httpParams, headers }
    );
  }

  /**
   * Get rider profile.
   */
  getRiderProfile(userId: string): Observable<ApiResponse<RiderProfileResponse>> {
    return this.http.get<ApiResponse<RiderProfileResponse>>(
      `${this.apiUrl}/users/${userId}/rider-profile`,
      { headers: this.getHeaders() }
    );
  }

  /**
   * Approve a seller (KYC verification).
   */
  approveSeller(userId: string, reason?: string): Observable<ApiResponse<{ userId: string; oldValue: string; newValue: string; updatedBy: string; updatedAt: string }>> {
    const body: any = {};
    if (reason) body.reason = reason;
    return this.http.post<ApiResponse<{ userId: string; oldValue: string; newValue: string; updatedBy: string; updatedAt: string }>>(
      `${this.apiUrl}/admin/users/${userId}/approve-seller`,
      body,
      { headers: this.getHeadersWithContentType() }
    );
  }

  /**
   * Create a new Super Admin employee.
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
      `${this.apiUrl}/admin/users/employees/super-admin`,
      employeeData,
      { headers: this.getHeadersWithContentType() }
    );
  }

  /**
   * Create a new Admin employee.
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
      `${this.apiUrl}/admin/users/employees/admin`,
      employeeData,
      { headers: this.getHeadersWithContentType() }
    );
  }

  /**
   * Create a new Rider employee.
   */
  createRider(employeeData: {
    email: string;
    phone: string;
    password: string;
    fullName: string;
    vehicleType: string;
    vehiclePlate: string;
  }): Observable<ApiResponse<ApiUserDetails>> {
    return this.http.post<ApiResponse<ApiUserDetails>>(
      `${this.apiUrl}/admin/users/employees/rider`,
      employeeData,
      { headers: this.getHeadersWithContentType() }
    );
  }

  // ================================================================
  // TRANSACTION MANAGEMENT
  // ================================================================

  /**
   * Create a new escrow transaction.
   */
  createTransaction(request: CreateEscrowTransactionRequest): Observable<ApiResponse<ApiEscrowTransaction>> {
    return this.http.post<ApiResponse<ApiEscrowTransaction>>(
      `${this.apiUrl}/transactions/create`,
      request,
      { headers: this.getHeadersWithContentType() }
    );
  }

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
   * Get transactions for a buyer.
   */
  getTransactionsByBuyer(buyerId: string): Observable<PageResponse<ApiEscrowTransaction>> {
    return this.http.get<PageResponse<ApiEscrowTransaction>>(
      `${this.apiUrl}/transactions/buyer/${buyerId}`,
      { headers: this.getHeaders() }
    );
  }

  /**
   * Get transactions for a seller.
   */
  getTransactionsBySeller(sellerId: string): Observable<PageResponse<ApiEscrowTransaction>> {
    return this.http.get<PageResponse<ApiEscrowTransaction>>(
      `${this.apiUrl}/transactions/seller/${sellerId}`,
      { headers: this.getHeaders() }
    );
  }

  /**
   * Get transactions for a rider.
   */
  getTransactionsByRider(riderId: string): Observable<PageResponse<ApiEscrowTransaction>> {
    return this.http.get<PageResponse<ApiEscrowTransaction>>(
      `${this.apiUrl}/transactions/rider/${riderId}`,
      { headers: this.getHeaders() }
    );
  }

  /**
   * Seller accepts transaction.
   */
  acceptTransaction(transactionId: string): Observable<ApiResponse<ApiEscrowTransaction>> {
    return this.http.post<ApiResponse<ApiEscrowTransaction>>(
      `${this.apiUrl}/transactions/${transactionId}/accept-transaction`,
      {},
      { headers: this.getHeadersWithContentType() }
    );
  }

  /**
   * Admin approves transaction.
   */
  approveTransaction(transactionId: string): Observable<ApiResponse<ApiEscrowTransaction>> {
    return this.http.post<ApiResponse<ApiEscrowTransaction>>(
      `${this.apiUrl}/transactions/${transactionId}/approve-transaction`,
      {},
      { headers: this.getHeadersWithContentType() }
    );
  }

  /**
   * Decline transaction.
   */
  declineTransaction(transactionId: string): Observable<ApiResponse<ApiEscrowTransaction>> {
    return this.http.post<ApiResponse<ApiEscrowTransaction>>(
      `${this.apiUrl}/transactions/${transactionId}/decline-transaction`,
      {},
      { headers: this.getHeadersWithContentType() }
    );
  }

  /**
   * Assign rider to transaction.
   */
  assignRider(transactionId: string, riderId: string): Observable<ApiResponse<ApiEscrowTransaction>> {
    return this.http.post<ApiResponse<ApiEscrowTransaction>>(
      `${this.apiUrl}/transactions/${transactionId}/assign-rider`,
      { riderId },
      { headers: this.getHeadersWithContentType() }
    );
  }

  /**
   * Rider accepts delivery.
   */
  riderAcceptDelivery(transactionId: string): Observable<ApiResponse<ApiEscrowTransaction>> {
    return this.http.post<ApiResponse<ApiEscrowTransaction>>(
      `${this.apiUrl}/transactions/${transactionId}/rider-accept-delivery`,
      {},
      { headers: this.getHeadersWithContentType() }
    );
  }

  /**
   * Mark transaction as in delivery.
   */
  markInDelivery(transactionId: string): Observable<ApiResponse<ApiEscrowTransaction>> {
    return this.http.post<ApiResponse<ApiEscrowTransaction>>(
      `${this.apiUrl}/transactions/${transactionId}/mark-in-delivery`,
      {},
      { headers: this.getHeadersWithContentType() }
    );
  }

  /**
   * Buyer confirms delivery.
   */
  buyerConfirmDelivery(transactionId: string): Observable<ApiResponse<ApiEscrowTransaction>> {
    return this.http.post<ApiResponse<ApiEscrowTransaction>>(
      `${this.apiUrl}/transactions/${transactionId}/buyer-confirm-delivery`,
      {},
      { headers: this.getHeadersWithContentType() }
    );
  }

  /**
   * Seller confirms delivery.
   */
  sellerConfirmDelivery(transactionId: string): Observable<ApiResponse<ApiEscrowTransaction>> {
    return this.http.post<ApiResponse<ApiEscrowTransaction>>(
      `${this.apiUrl}/transactions/${transactionId}/seller-confirm-delivery`,
      {},
      { headers: this.getHeadersWithContentType() }
    );
  }

  /**
   * Activate a user (admin action).
   */
  forceActivateUser(userId: string): Observable<ApiResponse<ApiUserDetails>> {
    return this.updateUserStatus(userId, 'ACTIVE', 'Force activated by admin');
  }

  /**
   * Admin: Force release funds held in escrow.
   * Uses confirm-receipt endpoint.
   */
  forceReleaseFunds(transactionId: string): Observable<ApiResponse<ApiEscrowTransaction>> {
    return this.http.post<ApiResponse<ApiEscrowTransaction>>(
      `${this.apiUrl}/transactions/${transactionId}/confirm-receipt`,
      {},
      { headers: this.getHeadersWithContentType() }
    );
  }

  /**
   * Admin: Force refund to buyer (cancel transaction).
   */
  cancelTransaction(transactionId: string): Observable<ApiResponse<ApiEscrowTransaction>> {
    return this.http.post<ApiResponse<ApiEscrowTransaction>>(
      `${this.apiUrl}/transactions/${transactionId}/cancel`,
      {},
      { headers: this.getHeadersWithContentType() }
    );
  }

  /**
   * Get the status history for a transaction.
   */
  getTransactionStatusHistory(transactionId: string): Observable<ApiResponse<TransactionStatusHistory[]>> {
    return this.http.get<ApiResponse<TransactionStatusHistory[]>>(
      `${this.apiUrl}/transactions/${transactionId}/status-history`,
      { headers: this.getHeaders() }
    );
  }

  /**
   * Get all status history with filters (admin).
   */
  getAllTransactionStatusHistory(params?: { transactionId?: string; status?: string; startDate?: string; endDate?: string; page?: number; size?: number }): Observable<PageResponse<TransactionStatusHistory>> {
    let httpParams = new HttpParams();
    if (params?.transactionId) httpParams = httpParams.set('transactionId', params.transactionId);
    if (params?.status) httpParams = httpParams.set('status', params.status);
    if (params?.startDate) httpParams = httpParams.set('startDate', params.startDate);
    if (params?.endDate) httpParams = httpParams.set('endDate', params.endDate);
    if (params?.page !== undefined) httpParams = httpParams.set('page', params.page);
    if (params?.size) httpParams = httpParams.set('size', params.size);
    return this.http.get<PageResponse<TransactionStatusHistory>>(
      `${this.apiUrl}/transactions/status-history`,
      { params: httpParams, headers: this.getHeaders() }
    );
  }

  /**
   * Get ledger entries for a transaction (admin).
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
   * Get a specific ledger entry by ID (admin).
   */
  getLedgerEntryById(id: string): Observable<ApiResponse<any>> {
    return this.http.get<ApiResponse<any>>(
      `${this.apiUrl}/admin/ledger-entries/${id}`,
      { headers: this.getHeaders() }
    );
  }

  // ================================================================
  // DISPUTE MANAGEMENT
  // ================================================================

  /**
   * Create a new dispute.
   */
  createDispute(request: DisputeCreateRequest): Observable<ApiResponse<DisputeCreateResponse>> {
    return this.http.post<ApiResponse<DisputeCreateResponse>>(
      `${this.apiUrl}/disputes`,
      request,
      { headers: this.getHeadersWithContentType() }
    );
  }

  /**
   * Add evidence to a dispute.
   */
  addDisputeEvidence(disputeId: string, evidenceUrls: string[], description?: string): Observable<ApiResponse<DisputeDetailsResponse>> {
    const body: DisputeEvidenceUpdateRequest = { evidenceUrls, description };
    return this.http.post<ApiResponse<DisputeDetailsResponse>>(
      `${this.apiUrl}/disputes/${disputeId}/evidence`,
      body,
      { headers: this.getHeadersWithContentType() }
    );
  }

  /**
   * Remove evidence from a dispute.
   */
  removeDisputeEvidence(disputeId: string, evidenceUrls: string[]): Observable<ApiResponse<DisputeDetailsResponse>> {
    return this.http.post<ApiResponse<DisputeDetailsResponse>>(
      `${this.apiUrl}/disputes/${disputeId}/evidence/remove`,
      { evidenceUrls },
      { headers: this.getHeadersWithContentType() }
    );
  }

  /**
   * Close a dispute.
   */
  closeDispute(disputeId: string, reason: string): Observable<ApiResponse<DisputeDetailsResponse>> {
    return this.http.post<ApiResponse<DisputeDetailsResponse>>(
      `${this.apiUrl}/disputes/${disputeId}/close`,
      { reason },
      { headers: this.getHeadersWithContentType() }
    );
  }

  /**
   * Get full dispute details by ID.
   */
  getDisputeById(id: string): Observable<ApiResponse<DisputeDetailsResponse>> {
    return this.http.get<ApiResponse<DisputeDetailsResponse>>(
      `${this.apiUrl}/disputes/${id}`,
      { headers: this.getHeaders() }
    );
  }

  /**
   * Get disputes by transaction ID.
   */
  getDisputesByTransaction(transactionId: string): Observable<ApiResponse<DisputeDetailsResponse[]>> {
    return this.http.get<ApiResponse<DisputeDetailsResponse[]>>(
      `${this.apiUrl}/disputes/transaction/${transactionId}`,
      { headers: this.getHeaders() }
    );
  }

  /**
   * Get paginated list of disputes (admin summary).
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
   * Resolve dispute with specific resolution.
   */
  resolveDispute(disputeId: string, resolution: DisputeResolveRequest): Observable<ApiResponse<DisputeDetailsResponse>> {
    return this.http.post<ApiResponse<DisputeDetailsResponse>>(
      `${this.apiUrl}/admin/disputes/${disputeId}/resolve`,
      resolution,
      { headers: this.getHeadersWithContentType() }
    );
  }

  /**
   * Convenience: Refund buyer.
   */
  resolveDisputeRefund(disputeId: string): Observable<ApiResponse<DisputeDetailsResponse>> {
    return this.resolveDispute(disputeId, { resolution: 'REFUND_BUYER' });
  }

  /**
   * Convenience: Release to seller.
   */
  resolveDisputeRelease(disputeId: string): Observable<ApiResponse<DisputeDetailsResponse>> {
    return this.resolveDispute(disputeId, { resolution: 'PAY_SELLER' });
  }

  /**
   * Convenience: Partial settlement.
   */
  resolveDisputePartial(disputeId: string, partialAmount: number, adminComments?: string): Observable<ApiResponse<DisputeDetailsResponse>> {
    return this.resolveDispute(disputeId, { resolution: 'PARTIAL_SETTLEMENT', partialAmount, adminComments });
  }

  /**
   * Mark dispute as action required (admin).
   */
  markDisputeActionRequired(disputeId: string, actionRequired: string, adminComments: string): Observable<ApiResponse<DisputeDetailsResponse>> {
    return this.http.post<ApiResponse<DisputeDetailsResponse>>(
      `${this.apiUrl}/admin/disputes/${disputeId}/action-required`,
      { actionRequired, adminComments },
      { headers: this.getHeadersWithContentType() }
    );
  }

  /**
   * Assign dispute to an admin.
   */
  assignDisputeToAdmin(disputeId: string, adminId: string): Observable<ApiResponse<DisputeDetailsResponse>> {
    return this.http.patch<ApiResponse<DisputeDetailsResponse>>(
      `${this.apiUrl}/admin/disputes/${disputeId}/assign`,
      { adminId },
      { headers: this.getHeadersWithContentType() }
    );
  }

  /**
   * Reassign dispute to another admin.
   */
  reassignDispute(disputeId: string, adminId: string): Observable<ApiResponse<DisputeDetailsResponse>> {
    return this.http.patch<ApiResponse<DisputeDetailsResponse>>(
      `${this.apiUrl}/admin/disputes/${disputeId}/reassign`,
      { adminId },
      { headers: this.getHeadersWithContentType() }
    );
  }

  // ================================================================
  // PAYMENT & PAYOUT MANAGEMENT
  // ================================================================

  /**
   * Initiate STK Push for payment.
   */
  initiateStkPush(escrowId: string, phoneNumber: string, amount: number): Observable<ApiResponse<InitiateStkPushResponse>> {
    return this.http.post<ApiResponse<InitiateStkPushResponse>>(
      `${this.apiUrl}/payments/escrows/${escrowId}/stk-push`,
      { phoneNumber, amount },
      { headers: this.getHeadersWithContentType() }
    );
  }

  /**
   * Release payment from escrow.
   */
  releasePayment(escrowId: string): Observable<ApiResponse<ReleasePayoutResponse>> {
    return this.http.post<ApiResponse<ReleasePayoutResponse>>(
      `${this.apiUrl}/payments/escrows/${escrowId}/release`,
      {},
      { headers: this.getHeadersWithContentType() }
    );
  }

  /**
   * Get payment by ID.
   */
  getPayment(paymentId: string): Observable<ApiResponse<PaymentResponse>> {
    return this.http.get<ApiResponse<PaymentResponse>>(
      `${this.apiUrl}/payments/${paymentId}`,
      { headers: this.getHeaders() }
    );
  }

  /**
   * Get my payouts (for the authenticated user).
   */
  getMyPayouts(): Observable<ApiResponse<PayoutFinanceResponse[]>> {
    return this.http.get<ApiResponse<PayoutFinanceResponse[]>>(
      `${this.apiUrl}/payments/payouts/me`,
      { headers: this.getHeaders() }
    );
  }

  /**
   * Get my payment intents (for the authenticated user).
   */
  getMyPaymentIntents(): Observable<ApiResponse<PaymentIntentFinanceResponse[]>> {
    return this.http.get<ApiResponse<PaymentIntentFinanceResponse[]>>(
      `${this.apiUrl}/payments/intents/me`,
      { headers: this.getHeaders() }
    );
  }

  /**
   * Get payment intents (admin).
   */
  getPaymentIntents(params?: { status?: string; page?: number; limit?: number }): Observable<ApiResponse<any[]>> {
    let httpParams = new HttpParams();
    if (params?.status) httpParams = httpParams.set('status', params.status);
    if (params?.page !== undefined) httpParams = httpParams.set('page', params.page);
    if (params?.limit) httpParams = httpParams.set('limit', params.limit);
    return this.http.get<ApiResponse<any[]>>(
      `${this.apiUrl}/admin/payment-intents`,
      { params: httpParams, headers: this.getHeaders() }
    );
  }

  /**
   * Get a single payment intent by ID (admin).
   */
  getPaymentIntentById(id: string): Observable<ApiResponse<any>> {
    return this.http.get<ApiResponse<any>>(
      `${this.apiUrl}/admin/payment-intents/${id}`,
      { headers: this.getHeaders() }
    );
  }

  /**
   * Get payouts (admin).
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

  /**
   * Get a single payout by ID (admin).
   */
  getPayoutById(id: string): Observable<ApiResponse<any>> {
    return this.http.get<ApiResponse<any>>(
      `${this.apiUrl}/admin/payouts/${id}`,
      { headers: this.getHeaders() }
    );
  }

  /**
   * Reconcile stuck processing payouts (admin).
   */
  reconcileStuckPayouts(): Observable<ApiResponse<PayoutReconciliationResultDto>> {
    return this.http.post<ApiResponse<PayoutReconciliationResultDto>>(
      `${this.apiUrl}/admin/payouts/reconcile-stuck-processing`,
      {},
      { headers: this.getHeadersWithContentType() }
    );
  }

  // ================================================================
  // IMAGE UPLOAD
  // ================================================================

  /**
   * Upload user profile image.
   */
  uploadUserProfile(file: File): Observable<ApiResponse<UploadImageResponse>> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<ApiResponse<UploadImageResponse>>(
      `${this.apiUrl}/uploads/users/profile`,
      formData,
      { headers: this.getHeaders() } // Don't set Content-Type, browser sets multipart
    );
  }

  /**
   * Upload dispute evidence image.
   */
  uploadDisputeEvidence(file: File): Observable<ApiResponse<UploadImageResponse>> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<ApiResponse<UploadImageResponse>>(
      `${this.apiUrl}/uploads/disputes`,
      formData,
      { headers: this.getHeaders() }
    );
  }

  // ================================================================
  // NOTIFICATIONS
  // ================================================================

  /**
   * Get user notifications (paginated).
   */
  getNotifications(params?: { page?: number; size?: number; read?: boolean }): Observable<PageResponse<InAppNotificationResponse>> {
    let httpParams = new HttpParams();
    if (params?.page !== undefined) httpParams = httpParams.set('page', params.page);
    if (params?.size) httpParams = httpParams.set('size', params.size);
    if (params?.read !== undefined) httpParams = httpParams.set('read', params.read);
    return this.http.get<PageResponse<InAppNotificationResponse>>(
      `${this.apiUrl}/notifications`,
      { params: httpParams, headers: this.getHeaders() }
    );
  }

  /**
   * Get unread notification count.
   */
  getUnreadNotificationCount(): Observable<ApiResponse<{ count: number }>> {
    return this.http.get<ApiResponse<{ count: number }>>(
      `${this.apiUrl}/notifications/unread-count`,
      { headers: this.getHeaders() }
    );
  }

  /**
   * Get notification counts by type.
   */
  getNotificationCounts(): Observable<ApiResponse<{ total: number; unread: number; archived: number; byType: any }>> {
    return this.http.get<ApiResponse<any>>(
      `${this.apiUrl}/notifications/counts`,
      { headers: this.getHeaders() }
    );
  }

  /**
   * Mark a notification as read.
   */
  markNotificationRead(notificationId: string): Observable<ApiResponse<NotificationStatusUpdateResponse>> {
    return this.http.patch<ApiResponse<NotificationStatusUpdateResponse>>(
      `${this.apiUrl}/notifications/${notificationId}/read`,
      {},
      { headers: this.getHeadersWithContentType() }
    );
  }

  /**
   * Archive a notification.
   */
  archiveNotification(notificationId: string): Observable<ApiResponse<NotificationStatusUpdateResponse>> {
    return this.http.patch<ApiResponse<NotificationStatusUpdateResponse>>(
      `${this.apiUrl}/notifications/${notificationId}/archive`,
      {},
      { headers: this.getHeadersWithContentType() }
    );
  }

  /**
   * Register a device token for push notifications.
   */
  registerDeviceToken(request: RegisterDeviceTokenRequest): Observable<ApiResponse<RegisterDeviceTokenResponse>> {
    return this.http.post<ApiResponse<RegisterDeviceTokenResponse>>(
      `${this.apiUrl}/notifications/devices/register`,
      request,
      { headers: this.getHeadersWithContentType() }
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
      `${this.apiUrl}/admin/audit-logs/all-logs`,
      { params: httpParams, headers: this.getHeaders() }
    );
  }

  /**
   * Get a single audit log by ID.
   */
  getAuditLogById(id: string): Observable<ApiResponse<any>> {
    return this.http.get<ApiResponse<any>>(
      `${this.apiUrl}/admin/audit-logs/log/${id}`,
      { headers: this.getHeaders() }
    );
  }
}
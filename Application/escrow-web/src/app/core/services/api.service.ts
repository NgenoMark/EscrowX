import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

// Define interfaces for API responses
export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

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
  id: number;
  name: string;
  phone: string;
  email: string;
  role: string;
  status: string;
  isPhoneVerified: boolean;
  isEmailVerified: boolean;
  kycStatus: string;
  blacklisted: boolean;
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

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private http = inject(HttpClient);
  private apiUrl = environment.apiUrl;

  // ========== AUTHENTICATION ==========
  
  login(credentials: { email: string; password: string }): Observable<ApiResponse<{ token: string; user: User }>> {
    return this.http.post<ApiResponse<{ token: string; user: User }>>(`${this.apiUrl}/auth/login`, credentials);
  }

  // ========== USER MANAGEMENT ==========
  
  getUsers(params?: { page?: number; limit?: number; search?: string }): Observable<ApiResponse<User[]>> {
    let httpParams = new HttpParams();
    if (params?.page) httpParams = httpParams.set('page', params.page);
    if (params?.limit) httpParams = httpParams.set('limit', params.limit);
    if (params?.search) httpParams = httpParams.set('search', params.search);
    
    return this.http.get<ApiResponse<User[]>>(`${this.apiUrl}/users`, { params: httpParams });
  }

  getUserById(id: number): Observable<ApiResponse<User>> {
    return this.http.get<ApiResponse<User>>(`${this.apiUrl}/users/${id}`);
  }

  suspendUser(userId: number): Observable<ApiResponse<User>> {
    return this.http.put<ApiResponse<User>>(`${this.apiUrl}/users/${userId}/suspend`, {});
  }

  activateUser(userId: number): Observable<ApiResponse<User>> {
    return this.http.put<ApiResponse<User>>(`${this.apiUrl}/users/${userId}/activate`, {});
  }

  approveKYC(userId: number): Observable<ApiResponse<User>> {
    return this.http.put<ApiResponse<User>>(`${this.apiUrl}/users/${userId}/kyc/approve`, {});
  }

  rejectKYC(userId: number, reason?: string): Observable<ApiResponse<User>> {
    return this.http.put<ApiResponse<User>>(`${this.apiUrl}/users/${userId}/kyc/reject`, { reason });
  }

  blacklistUser(userId: number): Observable<ApiResponse<User>> {
    return this.http.put<ApiResponse<User>>(`${this.apiUrl}/users/${userId}/blacklist`, {});
  }

  removeBlacklist(userId: number): Observable<ApiResponse<User>> {
    return this.http.put<ApiResponse<User>>(`${this.apiUrl}/users/${userId}/blacklist/remove`, {});
  }

  // ========== TRANSACTION MANAGEMENT ==========
  
  getTransactions(params?: { status?: string; page?: number; limit?: number; search?: string }): Observable<ApiResponse<Transaction[]>> {
    let httpParams = new HttpParams();
    if (params?.status) httpParams = httpParams.set('status', params.status);
    if (params?.page) httpParams = httpParams.set('page', params.page);
    if (params?.limit) httpParams = httpParams.set('limit', params.limit);
    if (params?.search) httpParams = httpParams.set('search', params.search);
    
    return this.http.get<ApiResponse<Transaction[]>>(`${this.apiUrl}/transactions`, { params: httpParams });
  }

  getTransactionById(id: string): Observable<ApiResponse<Transaction>> {
    return this.http.get<ApiResponse<Transaction>>(`${this.apiUrl}/transactions/${id}`);
  }

  forceReleaseFunds(transactionId: string): Observable<ApiResponse<Transaction>> {
    return this.http.post<ApiResponse<Transaction>>(`${this.apiUrl}/admin/transactions/${transactionId}/force-release`, {});
  }

  forceRefund(transactionId: string): Observable<ApiResponse<Transaction>> {
    return this.http.post<ApiResponse<Transaction>>(`${this.apiUrl}/admin/transactions/${transactionId}/force-refund`, {});
  }

  cancelTransaction(transactionId: string): Observable<ApiResponse<Transaction>> {
    return this.http.post<ApiResponse<Transaction>>(`${this.apiUrl}/admin/transactions/${transactionId}/cancel`, {});
  }

  // ========== DISPUTE MANAGEMENT ==========
  
  getDisputes(params?: { status?: string; page?: number; limit?: number }): Observable<ApiResponse<Dispute[]>> {
    let httpParams = new HttpParams();
    if (params?.status) httpParams = httpParams.set('status', params.status);
    if (params?.page) httpParams = httpParams.set('page', params.page);
    if (params?.limit) httpParams = httpParams.set('limit', params.limit);
    
    return this.http.get<ApiResponse<Dispute[]>>(`${this.apiUrl}/disputes`, { params: httpParams });
  }

  getDisputeById(id: string): Observable<ApiResponse<Dispute>> {
    return this.http.get<ApiResponse<Dispute>>(`${this.apiUrl}/disputes/${id}`);
  }

  resolveDisputeRefund(disputeId: string): Observable<ApiResponse<Dispute>> {
    return this.http.post<ApiResponse<Dispute>>(`${this.apiUrl}/admin/disputes/${disputeId}/resolve/refund`, {});
  }

  resolveDisputeRelease(disputeId: string): Observable<ApiResponse<Dispute>> {
    return this.http.post<ApiResponse<Dispute>>(`${this.apiUrl}/admin/disputes/${disputeId}/resolve/release`, {});
  }

  resolveDisputePartial(disputeId: string, partialAmount: number, notes: string): Observable<ApiResponse<Dispute>> {
    return this.http.post<ApiResponse<Dispute>>(`${this.apiUrl}/admin/disputes/${disputeId}/resolve/partial`, { partialAmount, notes });
  }

  updateDisputeStatus(disputeId: string, status: string): Observable<ApiResponse<Dispute>> {
    return this.http.put<ApiResponse<Dispute>>(`${this.apiUrl}/admin/disputes/${disputeId}/status`, { status });
  }

  addAdminNote(disputeId: string, note: string): Observable<ApiResponse<Dispute>> {
    return this.http.post<ApiResponse<Dispute>>(`${this.apiUrl}/admin/disputes/${disputeId}/notes`, { note });
  }

  // ========== ANALYTICS ==========
  
  getDashboardStats(): Observable<ApiResponse<any>> {
    return this.http.get<ApiResponse<any>>(`${this.apiUrl}/analytics/dashboard`);
  }

  getTransactionVolume(days?: number): Observable<ApiResponse<{ labels: string[]; data: number[] }>> {
    const params = days ? new HttpParams().set('days', days) : undefined;
    return this.http.get<ApiResponse<{ labels: string[]; data: number[] }>>(`${this.apiUrl}/analytics/volume`, { params });
  }

  getDisputeTrend(weeks?: number): Observable<ApiResponse<{ labels: string[]; data: number[] }>> {
    const params = weeks ? new HttpParams().set('weeks', weeks) : undefined;
    return this.http.get<ApiResponse<{ labels: string[]; data: number[] }>>(`${this.apiUrl}/analytics/disputes`, { params });
  }

  getUserGrowth(): Observable<ApiResponse<{ labels: string[]; data: number[] }>> {
    return this.http.get<ApiResponse<{ labels: string[]; data: number[] }>>(`${this.apiUrl}/analytics/users/growth`);
  }

  getTopSellers(limit?: number): Observable<ApiResponse<{ name: string; volume: number; transactions: number }[]>> {
    const params = limit ? new HttpParams().set('limit', limit) : undefined;
    return this.http.get<ApiResponse<{ name: string; volume: number; transactions: number }[]>>(`${this.apiUrl}/analytics/top-sellers`, { params });
  }

  // ========== AUDIT LOGS ==========
  
  getAuditLogs(params?: { action?: string; page?: number; limit?: number; startDate?: string; endDate?: string }): Observable<ApiResponse<any[]>> {
    let httpParams = new HttpParams();
    if (params?.action) httpParams = httpParams.set('action', params.action);
    if (params?.page) httpParams = httpParams.set('page', params.page);
    if (params?.limit) httpParams = httpParams.set('limit', params.limit);
    if (params?.startDate) httpParams = httpParams.set('startDate', params.startDate);
    if (params?.endDate) httpParams = httpParams.set('endDate', params.endDate);
    
    return this.http.get<ApiResponse<any[]>>(`${this.apiUrl}/audit-logs`, { params: httpParams });
  }

  // ========== AUTH TOKEN INTERCEPTOR ==========
  
  getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('access_token');
    return new HttpHeaders().set('Authorization', `Bearer ${token}`);
  }
}

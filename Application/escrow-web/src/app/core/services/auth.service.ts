// src/app/core/services/auth.service.ts
import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, of, tap, throwError } from 'rxjs';
import { environment } from '../../../environments/environment.global';

export interface AuthUser {
  id: string;
  email: string;
  phone: string;
  role: 'BUYER' | 'SELLER' | 'ADMIN' | 'SUPER_ADMIN' | 'RIDER' | string;
  status: string;
  blacklistStatus?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: AuthUser;
}

export interface RegisterResponse {
  userId: string;
  phone: string;
  status: string;
  role: string;
  otpPreview?: string | null;
}

export interface ConfirmResponse {
  email: string;
  status: string;
  confirmed: boolean;
}

// Mock users for testing
const MOCK_USERS: AuthUser[] = [
  {
    id: 'admin-001',
    email: 'admin@escrowx.com',
    phone: '+254712345678',
    role: 'SUPER_ADMIN',
    status: 'ACTIVE',
    blacklistStatus: 'NOT_BLACKLISTED',
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  },
  {
    id: 'admin-002',
    email: 'admin@example.com',
    phone: '+254723456789',
    role: 'ADMIN',
    status: 'ACTIVE',
    blacklistStatus: 'NOT_BLACKLISTED',
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  },
  {
    id: 'admin-003',
    email: 'super@admin.com',
    phone: '+254734567890',
    role: 'SUPER_ADMIN',
    status: 'ACTIVE',
    blacklistStatus: 'NOT_BLACKLISTED',
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  }
];

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);
  private authUrl = `${environment.apiUrl}/auth`;

  private userSignal = signal<AuthUser | null>(this.readStoredUser());

  readonly user = this.userSignal.asReadonly();
  readonly isAuthenticated = computed(() => Boolean(this.accessToken && this.userSignal()));
  readonly isAdmin = computed(() => {
    const role = this.userSignal()?.role;
    return role === 'ADMIN' || role === 'SUPER_ADMIN';
  });

  get accessToken(): string | null {
    return localStorage.getItem('access_token');
  }

  get refreshToken(): string | null {
    return localStorage.getItem('refresh_token');
  }

  /**
   * Check if we're in mock mode
   */
  private get isMockMode(): boolean {
    return environment.useMockData === true;
  }

  /**
   * Mock login - validates credentials against mock users
   */
  private mockLogin(credentials: { email: string; password: string }): Observable<LoginResponse> {
    console.log('🔐 Mock login attempt:', credentials.email);

    // Mock passwords (in real app, these would be hashed)
    const mockPassword = 'Admin@123';
    
    // Find user by email
    const user = MOCK_USERS.find(u => u.email.toLowerCase() === credentials.email.toLowerCase());
    
    if (!user) {
      return throwError(() => new Error('Invalid email or password.'));
    }

    // Check password (mock validation)
    if (credentials.password !== mockPassword) {
      return throwError(() => new Error('Invalid email or password.'));
    }

    // Generate mock token
    const token = btoa(`${user.id}:${Date.now()}`); // Simple mock token
    const refreshToken = btoa(`${user.id}:refresh:${Date.now()}`);

    const response: LoginResponse = {
      accessToken: token,
      refreshToken: refreshToken,
      tokenType: 'Bearer',
      expiresIn: 3600, // 1 hour
      user: user
    };

    console.log('✅ Mock login successful for:', user.email);
    return of(response);
  }

  login(credentials: { email: string; password: string }): Observable<LoginResponse> {
    // Use mock login if in mock mode
    if (this.isMockMode) {
      return this.mockLogin(credentials).pipe(
        tap(response => this.storeSession(response))
      );
    }

    // Real API call
    return this.http.post<LoginResponse>(`${this.authUrl}/login`, credentials).pipe(
      tap(response => this.storeSession(response))
    );
  }

  register(payload: {
    phone: string;
    email: string;
    password: string;
    displayName?: string;
    businessName?: string;
  }): Observable<RegisterResponse> {
    // Mock registration
    if (this.isMockMode) {
      const mockResponse: RegisterResponse = {
        userId: `user-${Date.now()}`,
        phone: payload.phone,
        status: 'PENDING_VERIFICATION',
        role: payload.businessName ? 'SELLER' : 'BUYER',
        otpPreview: '123456'
      };
      console.log('📝 Mock registration:', payload.email);
      return of(mockResponse);
    }

    return this.http.post<RegisterResponse>(`${this.authUrl}/register`, payload);
  }

  confirm(payload: { email: string; otp: string }): Observable<ConfirmResponse> {
    // Mock confirmation
    if (this.isMockMode) {
      if (payload.otp === '123456') {
        const mockResponse: ConfirmResponse = {
          email: payload.email,
          status: 'ACTIVE',
          confirmed: true
        };
        return of(mockResponse);
      } else {
        return throwError(() => new Error('Invalid OTP code.'));
      }
    }

    return this.http.post<ConfirmResponse>(`${this.authUrl}/confirm`, payload);
  }

  logout(): void {
    const refreshToken = this.refreshToken;
    localStorage.removeItem('access_token');
    localStorage.removeItem('refresh_token');
    localStorage.removeItem('auth_user');
    localStorage.removeItem('auth_expires_at');
    this.userSignal.set(null);

    if (refreshToken && !this.isMockMode) {
      this.http.post(`${this.authUrl}/logout`, { refreshToken }).subscribe({ error: () => undefined });
    }

    this.router.navigate(['/login']);
  }

  private storeSession(response: LoginResponse): void {
    const expiresAt = Date.now() + response.expiresIn * 1000;
    localStorage.setItem('access_token', response.accessToken);
    localStorage.setItem('refresh_token', response.refreshToken);
    localStorage.setItem('auth_expires_at', String(expiresAt));
    localStorage.setItem('auth_user', JSON.stringify(response.user));
    this.userSignal.set(response.user);
  }

  private readStoredUser(): AuthUser | null {
    const rawUser = localStorage.getItem('auth_user');
    const token = localStorage.getItem('access_token');
    const expiresAt = Number(localStorage.getItem('auth_expires_at') ?? '0');

    if (!rawUser || !token || !expiresAt || Date.now() >= expiresAt) {
      localStorage.removeItem('access_token');
      localStorage.removeItem('refresh_token');
      localStorage.removeItem('auth_user');
      localStorage.removeItem('auth_expires_at');
      return null;
    }

    try {
      return JSON.parse(rawUser) as AuthUser;
    } catch {
      localStorage.removeItem('auth_user');
      return null;
    }
  }
}
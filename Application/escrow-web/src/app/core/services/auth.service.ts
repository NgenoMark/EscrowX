import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment.prod';

export interface AuthUser {
  id: string;
  email: string;
  phone: string;
  role: 'BUYER' | 'SELLER' | 'ADMIN' | 'SUPER_ADMIN' | string;
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

  login(credentials: { email: string; password: string }): Observable<LoginResponse> {
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
    return this.http.post<RegisterResponse>(`${this.authUrl}/register`, payload);
  }

  confirm(payload: { email: string; otp: string }): Observable<ConfirmResponse> {
    return this.http.post<ConfirmResponse>(`${this.authUrl}/confirm`, payload);
  }

  logout(): void {
    const refreshToken = this.refreshToken;
    localStorage.removeItem('access_token');
    localStorage.removeItem('refresh_token');
    localStorage.removeItem('auth_user');
    localStorage.removeItem('auth_expires_at');
    this.userSignal.set(null);

    if (refreshToken) {
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

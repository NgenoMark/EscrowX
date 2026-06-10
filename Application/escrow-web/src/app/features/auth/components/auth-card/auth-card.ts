import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';

type AuthMode = 'login' | 'register' | 'confirm';

@Component({
  selector: 'app-auth-card',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './auth-card.html',
  styleUrls: ['./auth-card.css']
})
export class AuthCardComponent {
  private authService = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  mode = signal<AuthMode>('login');
  loading = signal(false);
  error = signal('');
  success = signal('');

  loginForm = {
    email: '',
    password: ''
  };

  registerForm = {
    displayName: '',
    businessName: '',
    phone: '',
    email: '',
    password: ''
  };

  confirmForm = {
    email: '',
    otp: ''
  };

  constructor() {
    if (this.route.snapshot.queryParamMap.get('reason') === 'admin-required') {
      this.error.set('This console is restricted to ADMIN and SUPER_ADMIN accounts.');
    }
  }

  setMode(mode: AuthMode): void {
    this.mode.set(mode);
    this.error.set('');
    this.success.set('');
  }

  login(): void {
    this.loading.set(true);
    this.error.set('');

    this.authService.login(this.loginForm).subscribe({
      next: () => {
        if (!this.authService.isAdmin()) {
          this.authService.logout();
          this.error.set('This console is restricted to ADMIN and SUPER_ADMIN accounts.');
          this.loading.set(false);
          return;
        }

        this.router.navigate(['/dashboard']);
      },
      error: error => {
        this.error.set(this.getErrorMessage(error, 'Login failed. Check your email and password.'));
        this.loading.set(false);
      }
    });
  }

  register(): void {
    this.loading.set(true);
    this.error.set('');
    this.success.set('');

    this.authService.register(this.registerForm).subscribe({
      next: () => {
        this.confirmForm.email = this.registerForm.email;
        this.success.set('Account created. Enter the OTP sent to your email to activate it.');
        this.mode.set('confirm');
        this.loading.set(false);
      },
      error: error => {
        this.error.set(this.getErrorMessage(error, 'Registration failed. Check the details and try again.'));
        this.loading.set(false);
      }
    });
  }

  confirm(): void {
    this.loading.set(true);
    this.error.set('');

    this.authService.confirm(this.confirmForm).subscribe({
      next: () => {
        this.success.set('Account confirmed. Sign in with your email and password.');
        this.loginForm.email = this.confirmForm.email;
        this.mode.set('login');
        this.loading.set(false);
      },
      error: error => {
        this.error.set(this.getErrorMessage(error, 'Confirmation failed. Check the OTP and try again.'));
        this.loading.set(false);
      }
    });
  }

  private getErrorMessage(error: unknown, fallback: string): string {
    const response = error as { error?: { message?: string; error?: string }; message?: string };
    return response.error?.message ?? response.error?.error ?? response.message ?? fallback;
  }
}

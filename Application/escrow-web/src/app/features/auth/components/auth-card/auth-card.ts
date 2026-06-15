// auth-card.ts
import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';

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

  loading = signal(false);
  error = signal('');

  loginForm = {
    email: '',
    password: ''
  };

  constructor() {
    if (this.route.snapshot.queryParamMap.get('reason') === 'admin-required') {
      this.error.set('This console is restricted to ADMIN and SUPER_ADMIN accounts.');
    }
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

  private getErrorMessage(error: unknown, fallback: string): string {
    const response = error as { error?: { message?: string; error?: string }; message?: string };
    return response.error?.message ?? response.error?.error ?? response.message ?? fallback;
  }
}
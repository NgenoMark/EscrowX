// src/app/features/auth/components/forgot-password/forgot-password.ts
import { Component, signal } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { ApiService } from '../../../../core/services/api.service';
import { NotificationService } from '../../../../core/services/notifications';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './forgot-password.html',
  styleUrls: ['./forgot-password.css']
})
export class ForgotPasswordComponent {
  forgotForm: FormGroup;
  loading = signal(false);
  error = signal('');
  success = signal(false);

  constructor(
    private fb: FormBuilder,
    private apiService: ApiService,
    private notificationService: NotificationService,
    private router: Router
  ) {
    this.forgotForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]]
    });
  }

  onSubmit(): void {
    if (this.forgotForm.invalid) return;
    this.loading.set(true);
    this.error.set('');
    const email = this.forgotForm.value.email;

    this.apiService.requestPasswordReset(email).subscribe({
      next: () => {
        this.loading.set(false);
        this.success.set(true);
        this.notificationService.add('Password Reset', 'A reset code has been sent to your email.', 'success');
      },
      error: (err) => {
        this.loading.set(false);
        const msg = err.error?.message || 'Failed to send reset code. Please try again.';
        this.error.set(msg);
        this.notificationService.add('Error', msg, 'danger');
      }
    });
  }

  goToReset(): void {
    const email = this.forgotForm.value.email;
    // 👇 Pass email as query param
    this.router.navigate(['/reset-password'], { queryParams: { email } });
  }
}
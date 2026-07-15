// src/app/features/auth/components/reset-password/reset-password.ts
import { Component, OnInit, signal } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { ApiService } from '../../../../core/services/api.service';
import { NotificationService } from '../../../../core/services/notifications';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './reset-password.html',
  styleUrls: ['./reset-password.css']
})
export class ResetPasswordComponent implements OnInit {
  resetForm: FormGroup;
  loading = signal(false);
  error = signal('');
  otpFromUrl: string | null = null;

  // Password visibility toggles
  showNewPassword = signal(false);
  showConfirmPassword = signal(false);

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private apiService: ApiService,
    private notificationService: NotificationService,
    private router: Router
  ) {
    this.resetForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],   // kept but hidden
      otp: ['', Validators.required],
      newPassword: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', Validators.required]
    }, { validators: this.passwordMatchValidator });
  }

  ngOnInit(): void {
    this.otpFromUrl = this.route.snapshot.queryParamMap.get('otp');
    const emailFromUrl = this.route.snapshot.queryParamMap.get('email');

    if (this.otpFromUrl) {
      this.resetForm.patchValue({ otp: this.otpFromUrl });
      this.resetForm.get('otp')?.disable();
    }

    if (emailFromUrl) {
      this.resetForm.patchValue({ email: emailFromUrl });
      this.resetForm.get('email')?.disable(); // disable but keep value
    }
  }

  passwordMatchValidator(group: FormGroup): { [key: string]: boolean } | null {
    const newPass = group.get('newPassword')?.value;
    const confirmPass = group.get('confirmPassword')?.value;
    return newPass === confirmPass ? null : { mismatch: true };
  }

  toggleNewPassword(): void {
    this.showNewPassword.update(v => !v);
  }

  toggleConfirmPassword(): void {
    this.showConfirmPassword.update(v => !v);
  }

  onSubmit(): void {
    if (this.resetForm.invalid) return;
    this.loading.set(true);
    this.error.set('');

    // getRawValue() includes disabled fields
    const { email, otp, newPassword } = this.resetForm.getRawValue();

    this.apiService.confirmPasswordReset(email, otp, newPassword).subscribe({
      next: () => {
        this.loading.set(false);
        this.notificationService.add('Password Reset', 'Your password has been reset. Please login.', 'success');
        this.router.navigate(['/login']);
      },
      error: (err) => {
        this.loading.set(false);
        const msg = err.error?.message || 'Failed to reset password. Check your email and reset code.';
        this.error.set(msg);
        this.notificationService.add('Error', msg, 'danger');
      }
    });
  }
}
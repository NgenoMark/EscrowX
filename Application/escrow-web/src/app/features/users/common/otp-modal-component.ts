// src/app/core/components/otp-modal/otp-modal.component.ts

import { Component, inject, signal, computed, input, output, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../../core/services/api.service';
import { NotificationService } from '../../../core/services/notifications';
import { User } from '../../../core/models/user';

export type OtpActionType = 'verify' | 'sendOtp' | 'forceActivate' | 'approve' | 'suspend' | 'blacklist' | 'activate' | 'restore';

export interface OtpModalConfig {
  title: string;
  message: string;
  actionType: OtpActionType;
  user: User | null;
  showOtpInput?: boolean;
  showReasonInput?: boolean;
  showApprovalNote?: boolean;
  showWarning?: boolean;
  warningText?: string;
  confirmButtonText?: string;
  confirmButtonClass?: string;
  reasonLabel?: string;
  reasonPlaceholder?: string;
}

@Component({
  selector: 'app-otp-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    @if (isOpen()) {
      <div class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4" (click)="close()">
        <div class="bg-white rounded-2xl shadow-xl max-w-md w-full" (click)="$event.stopPropagation()">
          <div class="p-6">
            <!-- Header -->
            <div class="flex items-center gap-3 mb-4">
              <div class="h-12 w-12 rounded-full" [class]="getIconBgClass()">
                <i class="fas" [class]="getIconClass()"></i>
              </div>
              <div>
                <h3 class="text-xl font-bold text-gray-800">{{ config().title }}</h3>
                <p class="text-sm text-gray-500" [innerHTML]="config().message"></p>
              </div>
            </div>

            <!-- OTP Input -->
            @if (config().showOtpInput) {
              <div class="mb-4">
                <label class="block text-sm font-medium text-gray-700 mb-1">OTP Code *</label>
                <input 
                  type="text" 
                  [(ngModel)]="otpCode" 
                  placeholder="e.g. 123456" 
                  maxlength="6"
                  class="w-full px-4 py-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 text-center text-2xl tracking-widest"
                  (keydown.enter)="confirm()"
                  autofocus
                >
                <p class="text-xs text-gray-400 mt-1">Enter the 6-digit code sent to the user's email.</p>
              </div>
            }

            <!-- Reason Input -->
            @if (config().showReasonInput) {
              <div class="mb-4">
                <label class="block text-sm font-medium text-gray-700 mb-1">
                  {{ config().reasonLabel || 'Reason' }} *
                </label>
                <textarea
                  [(ngModel)]="reason"
                  rows="3"
                  class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  [placeholder]="config().reasonPlaceholder || 'Provide a detailed reason for this action...'"
                ></textarea>
              </div>
            }

            <!-- Approval Note -->
            @if (config().showApprovalNote) {
              <div class="mb-4 p-3 bg-green-50 border border-green-200 rounded-lg">
                <p class="text-sm text-green-800">
                  <i class="fas fa-info-circle mr-2"></i>
                  Approving this seller will set their status to <strong>ACTIVE</strong> and allow them to list products.
                </p>
              </div>
              <div class="mb-4">
                <label class="block text-sm font-medium text-gray-700 mb-1">Approval Note (optional)</label>
                <textarea
                  [(ngModel)]="reason"
                  rows="3"
                  class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  placeholder="Add any notes about this approval..."
                ></textarea>
              </div>
            }

            <!-- Warning -->
            @if (config().showWarning) {
              <div class="mb-4 p-3 bg-amber-50 border border-amber-200 rounded-lg">
                <p class="text-sm text-amber-800">
                  <i class="fas fa-exclamation-triangle mr-2"></i>
                  {{ config().warningText || 'This action is irreversible.' }}
                </p>
              </div>
            }

            <!-- Actions -->
            <div class="flex justify-end gap-3 mt-6">
              <button 
                (click)="close()" 
                class="px-4 py-2 bg-gray-200 text-gray-800 rounded-lg hover:bg-gray-300 transition"
              >
                Cancel
              </button>
              <button 
                (click)="confirm()" 
                [disabled]="isDisabled()"
                [class]="config().confirmButtonClass || 'bg-indigo-600 hover:bg-indigo-700'"
                class="px-4 py-2 text-white rounded-lg transition disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
              >
                @if (isLoading()) {
                  <i class="fas fa-spinner fa-spin"></i>
                } @else {
                  <i class="fas" [class]="getConfirmIcon()"></i>
                }
                {{ config().confirmButtonText || getDefaultConfirmText() }}
              </button>
            </div>
          </div>
        </div>
      </div>
    }
  `,
  styles: [`
    input[type="text"]:focus {
      outline: none;
    }
    textarea:focus {
      outline: none;
    }
  `]
})
export class OtpModalComponent implements OnInit, OnDestroy {
  private apiService = inject(ApiService);
  private notificationService = inject(NotificationService);

  // Inputs
  isOpen = input<boolean>(false);
  config = input<OtpModalConfig>({
    title: 'Verify OTP',
    message: 'Enter the verification code',
    actionType: 'verify',
    user: null,
    showOtpInput: true,
    showReasonInput: false,
    showApprovalNote: false,
    showWarning: false
  });

  // Outputs
  onConfirm = output<{ action: OtpActionType; user: User; otp?: string; reason?: string }>();
  onClose = output<void>();

  // State
  otpCode = signal('');
  reason = signal('');
  isLoading = signal(false);

  // Timer state for OTP
  private timerInterval: any;

  ngOnInit() {
    // Reset state when modal opens
    if (this.isOpen()) {
      this.resetState();
    }
  }

  ngOnDestroy() {
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
    }
  }

  resetState(): void {
    this.otpCode.set('');
    this.reason.set('');
    this.isLoading.set(false);
  }

  // Computed: Check if confirm button should be disabled
  isDisabled = computed(() => {
    const config = this.config();
    if (this.isLoading()) return true;
    
    if (config.showOtpInput && (!this.otpCode() || this.otpCode().length !== 6)) {
      return true;
    }
    
    if (config.showReasonInput && !this.reason().trim()) {
      return true;
    }
    
    return false;
  });

  // Close modal
  close(): void {
    this.resetState();
    this.onClose.emit();
  }

  // Confirm action
  confirm(): void {
    const config = this.config();
    const user = config.user;
    
    if (!user) {
      this.notificationService.add('Error', 'No user selected.', 'danger');
      return;
    }

    if (config.showOtpInput && (!this.otpCode() || this.otpCode().length !== 6)) {
      this.notificationService.add('Validation Error', 'Please enter a valid 6-digit OTP.', 'warning');
      return;
    }

    if (config.showReasonInput && !this.reason().trim()) {
      this.notificationService.add('Validation Error', 'Please provide a reason.', 'warning');
      return;
    }

    this.isLoading.set(true);

    // Emit the confirmation
    this.onConfirm.emit({
      action: config.actionType,
      user: user,
      otp: this.otpCode(),
      reason: this.reason().trim()
    });

    // Note: The parent component should handle the API call and close the modal
    // We'll close after a delay to show loading state
    setTimeout(() => {
      this.isLoading.set(false);
    }, 1000);
  }

  // Helper methods for icons and styles
  getIconBgClass(): string {
    const action = this.config().actionType;
    switch(action) {
      case 'verify': return 'bg-blue-100';
      case 'sendOtp': return 'bg-blue-100';
      case 'forceActivate': return 'bg-amber-100';
      case 'approve': return 'bg-green-100';
      case 'suspend': return 'bg-yellow-100';
      case 'blacklist': return 'bg-red-100';
      case 'activate': return 'bg-green-100';
      case 'restore': return 'bg-gray-100';
      default: return 'bg-indigo-100';
    }
  }

  getIconClass(): string {
    const action = this.config().actionType;
    switch(action) {
      case 'verify': return 'fa-shield-alt text-blue-600';
      case 'sendOtp': return 'fa-envelope text-blue-600';
      case 'forceActivate': return 'fa-bolt text-amber-600';
      case 'approve': return 'fa-check-circle text-green-600';
      case 'suspend': return 'fa-pause-circle text-yellow-600';
      case 'blacklist': return 'fa-ban text-red-600';
      case 'activate': return 'fa-play-circle text-green-600';
      case 'restore': return 'fa-undo text-gray-600';
      default: return 'fa-info-circle text-indigo-600';
    }
  }

  getConfirmIcon(): string {
    const action = this.config().actionType;
    switch(action) {
      case 'verify': return 'fa-check';
      case 'sendOtp': return 'fa-paper-plane';
      case 'forceActivate': return 'fa-bolt';
      case 'approve': return 'fa-check-circle';
      case 'suspend': return 'fa-pause';
      case 'blacklist': return 'fa-ban';
      case 'activate': return 'fa-play';
      case 'restore': return 'fa-undo';
      default: return 'fa-check';
    }
  }

  getDefaultConfirmText(): string {
    const action = this.config().actionType;
    switch(action) {
      case 'verify': return 'Verify';
      case 'sendOtp': return 'Send OTP';
      case 'forceActivate': return 'Force Activate';
      case 'approve': return 'Approve';
      case 'suspend': return 'Suspend';
      case 'blacklist': return 'Blacklist';
      case 'activate': return 'Activate';
      case 'restore': return 'Restore';
      default: return 'Confirm';
    }
  }

  // Helper to close from parent
  closeModal(): void {
    this.close();
  }
}
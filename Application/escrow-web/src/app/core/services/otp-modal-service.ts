// src/app/core/services/otp-modal.service.ts

import { Injectable } from '@angular/core';
import { User } from '../models/user';
import { OtpModalConfig, OtpActionType } from '../../features/users/common/otp-modal-component';

@Injectable({
  providedIn: 'root'
})
export class OtpModalService {
  
  /**
   * Get config for OTP verification
   */
  getVerifyOtpConfig(user: User): OtpModalConfig {
    return {
      title: 'Verify User OTP',
      message: `Enter the 6‑digit verification code sent to <strong>${user.email}</strong>.`,
      actionType: 'verify',
      user: user,
      showOtpInput: true,
      showReasonInput: false,
      showApprovalNote: false,
      showWarning: false,
      confirmButtonText: 'Verify',
      confirmButtonClass: 'bg-blue-600 hover:bg-blue-700'
    };
  }

  /**
   * Get config for force activation
   */
  getForceActivateConfig(user: User): OtpModalConfig {
    return {
      title: 'Force Activate User',
      message: `You are about to <strong class="text-amber-600">force activate</strong> <strong>${user.displayName}</strong>. This bypasses OTP verification.`,
      actionType: 'forceActivate',
      user: user,
      showOtpInput: false,
      showReasonInput: false,
      showApprovalNote: false,
      showWarning: true,
      warningText: 'Only use if the user has confirmed their identity through other means.',
      confirmButtonText: 'Force Activate',
      confirmButtonClass: 'bg-amber-600 hover:bg-amber-700'
    };
  }

  /**
   * Get config for seller approval
   */
  getApproveSellerConfig(user: User): OtpModalConfig {
    return {
      title: 'Approve Seller',
      message: `You are about to <strong class="text-green-600">approve</strong> <strong>${user.displayName}</strong> as a verified seller.`,
      actionType: 'approve',
      user: user,
      showOtpInput: false,
      showReasonInput: false,
      showApprovalNote: true,
      showWarning: false,
      confirmButtonText: 'Approve Seller',
      confirmButtonClass: 'bg-green-600 hover:bg-green-700'
    };
  }

  /**
   * Get config for suspending a user
   */
  getSuspendConfig(user: User): OtpModalConfig {
    return {
      title: 'Suspend User',
      message: `You are about to suspend <strong>${user.displayName}</strong>.`,
      actionType: 'suspend',
      user: user,
      showOtpInput: false,
      showReasonInput: true,
      showApprovalNote: false,
      showWarning: false,
      reasonLabel: 'Suspension Reason',
      reasonPlaceholder: 'Provide a detailed reason for suspending this user...',
      confirmButtonText: 'Suspend',
      confirmButtonClass: 'bg-yellow-600 hover:bg-yellow-700'
    };
  }

  /**
   * Get config for blacklisting a user
   */
  getBlacklistConfig(user: User): OtpModalConfig {
    return {
      title: 'Blacklist User',
      message: `You are about to permanently blacklist <strong>${user.displayName}</strong>. This action is irreversible.`,
      actionType: 'blacklist',
      user: user,
      showOtpInput: false,
      showReasonInput: true,
      showApprovalNote: false,
      showWarning: true,
      warningText: 'This action is irreversible. The user will be permanently banned.',
      reasonLabel: 'Blacklist Reason',
      reasonPlaceholder: 'Provide a detailed reason for blacklisting this user...',
      confirmButtonText: 'Blacklist',
      confirmButtonClass: 'bg-red-600 hover:bg-red-700'
    };
  }

  /**
   * Get config for activating a user
   */
  getActivateConfig(user: User): OtpModalConfig {
    return {
      title: 'Activate User',
      message: `Activate <strong>${user.displayName}</strong>? They will regain full access.`,
      actionType: 'activate',
      user: user,
      showOtpInput: false,
      showReasonInput: false,
      showApprovalNote: false,
      showWarning: false,
      confirmButtonText: 'Activate',
      confirmButtonClass: 'bg-green-600 hover:bg-green-700'
    };
  }

  /**
   * Get config for restoring a user from blacklist
   */
  getRestoreConfig(user: User): OtpModalConfig {
    return {
      title: 'Restore User',
      message: `Remove <strong>${user.displayName}</strong> from blacklist. Their status will become "Pending Verification".`,
      actionType: 'restore',
      user: user,
      showOtpInput: false,
      showReasonInput: false,
      showApprovalNote: false,
      showWarning: false,
      confirmButtonText: 'Restore',
      confirmButtonClass: 'bg-gray-600 hover:bg-gray-700'
    };
  }

  /**
   * Get config for sending OTP
   */
  getSendOtpConfig(user: User): OtpModalConfig {
    return {
      title: 'Send OTP',
      message: `Send a verification code to <strong>${user.email}</strong>?`,
      actionType: 'sendOtp',
      user: user,
      showOtpInput: false,
      showReasonInput: false,
      showApprovalNote: false,
      showWarning: false,
      confirmButtonText: 'Send OTP',
      confirmButtonClass: 'bg-blue-600 hover:bg-blue-700'
    };
  }

  /**
   * Get config by action type
   */
  getConfigForAction(user: User, action: OtpActionType): OtpModalConfig {
    switch(action) {
      case 'verify': return this.getVerifyOtpConfig(user);
      case 'forceActivate': return this.getForceActivateConfig(user);
      case 'approve': return this.getApproveSellerConfig(user);
      case 'suspend': return this.getSuspendConfig(user);
      case 'blacklist': return this.getBlacklistConfig(user);
      case 'activate': return this.getActivateConfig(user);
      case 'restore': return this.getRestoreConfig(user);
      case 'sendOtp': return this.getSendOtpConfig(user);
      default: return this.getVerifyOtpConfig(user);
    }
  }
}
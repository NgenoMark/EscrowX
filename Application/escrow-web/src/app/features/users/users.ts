// users/users.ts - Updated with shared modal

import { Component, inject, signal, OnInit, computed, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { DataService } from '../../core/services/data';
import { SearchService } from '../../core/services/search';
import { ApiService, ApiUserDetails } from '../../core/services/api.service';
import { AuthService } from '../../core/services/auth.service';
import { NotificationService } from '../../core/services/notifications';
import { User } from '../../core/models/user';
import { CacheService } from '../../core/services/cache.service';
import { NgxSmkSkeletonDirective } from 'ngxsmk-skeleton-loader';
import { OtpModalComponent, OtpModalConfig, OtpActionType } from './common/otp-modal-component';
import { OtpModalService } from '../../core/services/otp-modal-service';

type ActionType = OtpActionType;

const CACHE_KEY_USERS = 'admin_users';

@Component({
  selector: 'app-users',
  standalone: true,
  imports: [CommonModule, FormsModule, NgxSmkSkeletonDirective, OtpModalComponent],
  templateUrl: './users.html',
  styleUrls: ['./users.css']
})
export class UsersComponent implements OnInit, OnDestroy {
  private dataService = inject(DataService);
  private searchService = inject(SearchService);
  private apiService = inject(ApiService);
  private authService = inject(AuthService);
  private notificationService = inject(NotificationService);
  private cacheService = inject(CacheService);
  private otpModalService = inject(OtpModalService);

  isLoading = signal<boolean>(true);

  // Filters
  filterStatus = signal<string>('ALL');
  filterRole = signal<string>('ALL');
  filterBlacklist = signal<string>('ALL');

  private directUsersSignal = signal<User[]>([]);
  directUsers = this.directUsersSignal.asReadonly();
  searchTerm = this.searchService.query;

  // Multi-select
  public selectedIds = signal<Set<string>>(new Set());

  // OTP State - Simplified
  otpSending = signal<Record<string, boolean>>({});
  otpSent = signal<Record<string, boolean>>({});
  otpTimer = signal<Record<string, number>>({});
  private timerInterval: any;

  // Shared Modal State
  showModal = signal(false);
  modalConfig = signal<OtpModalConfig | null>(null);
  pendingAction = signal<{ action: OtpActionType; user: User } | null>(null);

  // Profile modal
  showProfileModal = signal(false);
  selectedUserForProfile = signal<User | null>(null);

  // Bulk action modal
  showBulkModal = signal(false);
  bulkActionType = signal<ActionType | null>(null);
  bulkReason = signal('');

  // Add User modal
  showAddUserModal = signal(false);
  addUserData = signal<{
    email: string;
    phone: string;
    password: string;
    displayName: string;
    businessName: string;
    role: 'BUYER' | 'SELLER' | 'ADMIN' | 'SUPER_ADMIN';
  }>({
    email: '',
    phone: '',
    password: '',
    displayName: '',
    businessName: '',
    role: 'BUYER'
  });
  confirmPassword = signal('');
  addUserLoading = signal(false);
  fieldErrors = signal<{ [key: string]: string }>({});

  // Computed filtered users
  filteredUsers = computed<User[]>(() => {
    const users = this.directUsersSignal().length ? this.directUsersSignal() : this.dataService.users();
    const term = (this.searchTerm() || '').toLowerCase();
    const status = this.filterStatus();
    const role = this.filterRole();
    const blacklist = this.filterBlacklist();

    return users.filter(u => {
      if (term) {
        const matchesTerm = (u.displayName || '').toLowerCase().includes(term)
          || String(u.phone || '').toLowerCase().includes(term)
          || (u.email || '').toLowerCase().includes(term);
        if (!matchesTerm) return false;
      }
      if (status && status !== 'ALL' && u.status !== status) return false;
      if (role && role !== 'ALL' && u.role !== role) return false;
      if (blacklist && blacklist !== 'ALL' && u.blacklistStatus !== blacklist) return false;
      return true;
    });
  });

  // Stats
  totalUsers = computed(() => this.filteredUsers().length);
  activeUsers = computed(() => this.filteredUsers().filter(u => u.status === 'ACTIVE').length);
  suspendedUsers = computed(() => this.filteredUsers().filter(u => u.status === 'SUSPENDED').length);
  pendingVerificationUsers = computed(() => this.filteredUsers().filter(u => u.status === 'PENDING_VERIFICATION').length);
  pendingAdminApprovalUsers = computed(() => this.filteredUsers().filter(u => u.status === 'PENDING_ADMIN_APPROVAL').length);
  blacklistedCount = computed(() => this.filteredUsers().filter(u => u.status === 'BLACKLISTED').length);
  underInvestigationCount = computed(() => this.filteredUsers().filter(u => u.blacklistStatus === 'UNDER_INVESTIGATION').length);

  // Selection helpers
  isAllSelected = computed(() => {
    const filtered = this.filteredUsers();
    const selected = this.selectedIds();
    return filtered.length > 0 && filtered.every(u => selected.has(u.id));
  });

  isSomeSelected = computed(() => {
    const filtered = this.filteredUsers();
    const selected = this.selectedIds();
    return !this.isAllSelected() && selected.size > 0 && filtered.some(u => selected.has(u.id));
  });

  selectedCount = computed(() => this.selectedIds().size);

  ngOnInit() {
    this.loadUsers();
  }

  ngOnDestroy() {
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
    }
  }

  // ------------------- Data Loading -------------------
  loadUsers(forceRefresh = false): void {
    if (forceRefresh) {
      this.cacheService.clear(CACHE_KEY_USERS);
    }
    const cached = this.cacheService.get<User[]>(CACHE_KEY_USERS);
    if (cached) {
      this.directUsersSignal.set(cached);
      this.isLoading.set(false);
      return;
    }
    this.isLoading.set(true);
    this.apiService.getUsers({ size: 500 }).subscribe({
      next: (response) => {
        if (!response || !response.content) {
          this.notificationService.add('Data Error', 'Received invalid user data from server.', 'danger');
          this.isLoading.set(false);
          return;
        }
        const users = response.content.map(apiUser => this.mapApiUser(apiUser));
        this.directUsersSignal.set(users);
        this.cacheService.set(CACHE_KEY_USERS, users);
        this.isLoading.set(false);
      },
      error: (err: any) => {
        console.error('Failed to fetch users:', err);
        const errorMsg = err.status === 401 ? 'Session expired. Please log in again.' :
                         err.status === 403 ? 'You do not have permission to view users.' :
                         'Could not load users. Please try again later.';
        this.notificationService.add('Error', errorMsg, 'danger');
        this.isLoading.set(false);
      }
    });
  }

  refreshUsers(): void {
    this.loadUsers(true);
  }

  private mapApiUser(user: ApiUserDetails): User {
    return {
      id: user.id,
      phone: user.phone,
      email: user.email,
      role: user.role as any,
      status: user.status as any,
      blacklistStatus: (user.blacklistStatus as any) ?? 'NOT_BLACKLISTED',
      displayName: user.displayName || user.email || user.phone,
      businessName: user.businessName ?? null,
      avatarUrl: user.avatarUrl ?? null,
      createdAt: user.createdAt,
      updatedAt: user.updatedAt ?? undefined
    };
  }

  // ------------------- OTP Management -------------------
  
  sendOtpToUser(user: User): void {
    if (this.otpSending()[user.id]) return;

    this.otpSending.update(map => ({ ...map, [user.id]: true }));
    this.otpSent.update(map => ({ ...map, [user.id]: false }));

    this.apiService.sendVerificationOtp(user.id).subscribe({
      next: (response) => {
        this.otpSending.update(map => ({ ...map, [user.id]: false }));
        this.otpSent.update(map => ({ ...map, [user.id]: true }));
        
        this.otpTimer.update(map => ({ ...map, [user.id]: 60 }));
        this.startTimer(user.id);
        
        const data = response.data as any;
        const preview = data?.otpPreview ? ` (Preview: ${data.otpPreview})` : '';
        this.notificationService.add(
          'OTP Sent',
          `Verification code sent to ${user.displayName}${preview}`,
          'success'
        );
      },
      error: (err) => {
        this.otpSending.update(map => ({ ...map, [user.id]: false }));
        const msg = err.error?.message || 'Failed to send OTP. Please try again.';
        this.notificationService.add('Error', msg, 'danger');
      }
    });
  }

  private startTimer(userId: string): void {
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
    }

    this.timerInterval = setInterval(() => {
      const current = this.otpTimer()[userId];
      if (current > 0) {
        this.otpTimer.update(map => ({ ...map, [userId]: current - 1 }));
      } else {
        clearInterval(this.timerInterval);
        this.otpSent.update(map => ({ ...map, [userId]: false }));
        this.otpTimer.update(map => {
          const newMap = { ...map };
          delete newMap[userId];
          return newMap;
        });
      }
    }, 1000);
  }

  getOtpButtonText(userId: string): string {
    if (this.otpSending()[userId]) return 'Sending...';
    if (this.otpTimer()[userId]) return `Resend (${this.otpTimer()[userId]}s)`;
    if (this.otpSent()[userId]) return 'Resend OTP';
    return 'Send OTP';
  }

  canSendOtp(userId: string): boolean {
    return !this.otpSending()[userId] && !this.otpTimer()[userId];
  }

  // ------------------- Search & Filters -------------------
  onSearch(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.searchService.setQuery(input.value);
  }

  clearSearch(): void {
    this.searchService.clear();
  }

  clearFilters(): void {
    this.filterStatus.set('ALL');
    this.filterRole.set('ALL');
    this.filterBlacklist.set('ALL');
    this.searchService.clear();
  }

  // ------------------- Selection -------------------
  toggleSelection(id: string): void {
    this.selectedIds.update(set => {
      const newSet = new Set(set);
      if (newSet.has(id)) {
        newSet.delete(id);
      } else {
        newSet.add(id);
      }
      return newSet;
    });
  }

  toggleAll(checked: boolean): void {
    if (checked) {
      const ids = this.filteredUsers().map(u => u.id);
      this.selectedIds.set(new Set(ids));
    } else {
      this.selectedIds.set(new Set());
    }
  }

  clearSelection(): void {
    this.selectedIds.set(new Set());
  }

  // ------------------- Shared Modal Actions -------------------
  
  /**
   * Open the shared OTP modal with the appropriate config
   */
  openActionModal(user: User, action: ActionType): void {
    const config = this.otpModalService.getConfigForAction(user, action);
    this.modalConfig.set(config);
    this.pendingAction.set({ action, user });
    this.showModal.set(true);
  }

  /**
   * Handle modal confirmation
   */
  handleModalConfirm(event: { action: OtpActionType; user: User; otp?: string; reason?: string }): void {
    const { action, user, otp, reason } = event;
    
    switch (action) {
      case 'verify':
        if (!otp) {
          this.notificationService.add('Validation Error', 'OTP is required.', 'warning');
          return;
        }
        this.apiService.verifyUserOtp(user.id, otp).subscribe({
          next: () => {
            this.loadUsers(true);
            this.closeModal();
            this.notificationService.add('Success', `User ${user.displayName} verified and activated.`, 'success');
          },
          error: (err) => {
            const msg = err.error?.message || 'Invalid OTP. Please try again.';
            this.notificationService.add('Error', msg, 'danger');
          }
        });
        break;

      case 'approve':
        this.apiService.approveSeller(user.id, reason || 'Approved by admin').subscribe({
          next: () => {
            this.loadUsers(true);
            this.closeModal();
            this.notificationService.add('Success', `Seller ${user.displayName} approved.`, 'success');
          },
          error: (err) => this.handleError(err, 'approve')
        });
        break;

      case 'forceActivate':
        this.apiService.updateUserStatus(user.id, 'ACTIVE', 'Force activated by admin').subscribe({
          next: () => {
            this.loadUsers(true);
            this.closeModal();
            this.notificationService.add('Success', `User ${user.displayName} force activated.`, 'success');
          },
          error: (err) => this.handleError(err, 'activate')
        });
        break;

      case 'suspend':
        if (!reason) {
          this.notificationService.add('Validation Error', 'Reason is required for suspension.', 'warning');
          return;
        }
        this.apiService.updateUserStatus(user.id, 'SUSPENDED', reason).subscribe({
          next: () => {
            this.loadUsers(true);
            this.closeModal();
            this.notificationService.add('Success', `User ${user.displayName} suspended.`, 'success');
          },
          error: (err) => this.handleError(err, 'suspend')
        });
        break;

      case 'blacklist':
        if (!reason) {
          this.notificationService.add('Validation Error', 'Reason is required for blacklisting.', 'warning');
          return;
        }
        this.apiService.updateUserStatus(user.id, 'BLACKLISTED', reason).subscribe({
          next: () => {
            this.loadUsers(true);
            this.closeModal();
            this.notificationService.add('Success', `User ${user.displayName} blacklisted.`, 'success');
          },
          error: (err) => this.handleError(err, 'blacklist')
        });
        break;

      case 'activate':
        this.apiService.updateUserStatus(user.id, 'ACTIVE').subscribe({
          next: () => {
            this.loadUsers(true);
            this.closeModal();
            this.notificationService.add('Success', `User ${user.displayName} activated.`, 'success');
          },
          error: (err) => this.handleError(err, 'activate')
        });
        break;

      case 'restore':
        this.apiService.updateUserStatus(user.id, 'PENDING_VERIFICATION', 'Removed from blacklist by admin').subscribe({
          next: () => {
            this.loadUsers(true);
            this.closeModal();
            this.notificationService.add('Success', `User ${user.displayName} restored.`, 'success');
          },
          error: (err) => this.handleError(err, 'restore')
        });
        break;

      case 'sendOtp':
        this.sendOtpToUser(user);
        this.closeModal();
        break;
    }
  }

  /**
   * Close the shared modal
   */
  closeModal(): void {
    this.showModal.set(false);
    this.modalConfig.set(null);
    this.pendingAction.set(null);
  }

  private handleError(err: any, action: string): void {
    console.error(err);
    const msg = err.status === 401 ? 'Session expired. Please log in again.' :
                err.status === 403 ? `You do not have permission to ${action} users.` :
                `Failed to ${action} user. Please try again.`;
    this.notificationService.add('Error', msg, 'danger');
  }

  // ------------------- Bulk Actions -------------------
  bulkAction(action: ActionType): void {
    const selectedUsers = this.filteredUsers().filter(u => this.selectedIds().has(u.id));
    if (selectedUsers.length === 0) return;

    if (action === 'suspend' || action === 'blacklist') {
      this.openBulkActionModal(action);
    } else {
      this.executeBulkAction(action, '');
    }
  }

  openBulkActionModal(action: ActionType): void {
    this.bulkActionType.set(action);
    this.bulkReason.set('');
    this.showBulkModal.set(true);
  }

  closeBulkModal(): void {
    this.showBulkModal.set(false);
    this.bulkActionType.set(null);
    this.bulkReason.set('');
  }

  confirmBulkAction(): void {
    const action = this.bulkActionType();
    const reason = this.bulkReason().trim();
    if (!action) return;

    if ((action === 'suspend' || action === 'blacklist') && !reason) {
      this.notificationService.add('Validation Error', 'Please provide a reason for the bulk action.', 'warning');
      return;
    }
    this.executeBulkAction(action, reason);
    this.closeBulkModal();
  }

  private executeBulkAction(action: ActionType, reason: string): void {
    const selectedUsers = this.filteredUsers().filter(u => this.selectedIds().has(u.id));
    if (selectedUsers.length === 0) return;

    let status: string;
    let apiReason = reason || 'Bulk action by admin';

    switch (action) {
      case 'suspend': status = 'SUSPENDED'; break;
      case 'blacklist': status = 'BLACKLISTED'; break;
      case 'activate': status = 'ACTIVE'; break;
      case 'restore': status = 'PENDING_VERIFICATION'; apiReason = 'Restored from blacklist by admin'; break;
      default: return;
    }

    this.isLoading.set(true);

    const requests = selectedUsers.map(user =>
      this.apiService.updateUserStatus(user.id, status, apiReason)
    );

    forkJoin(requests).subscribe({
      next: () => {
        this.isLoading.set(false);
        this.notificationService.add(
          'Success',
          `Bulk action '${action}' applied to ${selectedUsers.length} user(s).`,
          'success'
        );
        this.loadUsers(true);
        this.clearSelection();
      },
      error: (err) => {
        this.isLoading.set(false);
        console.error('Bulk action error:', err);
        this.notificationService.add('Error', 'Some actions failed. Check logs.', 'danger');
        this.loadUsers(true);
        this.clearSelection();
      }
    });
  }

  // ------------------- Add User -------------------
  openAddUserModal(): void {
    this.addUserData.set({
      email: '',
      phone: '',
      password: '',
      displayName: '',
      businessName: '',
      role: 'BUYER'
    });
    this.confirmPassword.set('');
    this.fieldErrors.set({});
    this.showAddUserModal.set(true);
  }

  closeAddUserModal(): void {
    this.showAddUserModal.set(false);
    this.addUserLoading.set(false);
    this.confirmPassword.set('');
    this.fieldErrors.set({});
  }

  createUser(): void {
    const data = this.addUserData();
    const errors: { [key: string]: string } = {};

    this.fieldErrors.set({});

    if (!data.displayName) errors['displayName'] = 'Full name is required';
    if (!data.email) errors['email'] = 'Email is required';
    if (!data.phone) errors['phone'] = 'Phone number is required';
    if (!data.password) errors['password'] = 'Password is required';

    if (data.password) {
      const passwordError = this.validatePassword(data.password);
      if (passwordError) errors['password'] = passwordError;
    }

    if (data.password && this.confirmPassword() && data.password !== this.confirmPassword()) {
      errors['confirmPassword'] = 'Passwords do not match';
    }

    if (Object.keys(errors).length > 0) {
      this.fieldErrors.set(errors);
      this.notificationService.add('Validation Error', 'Please fix the errors in the form.', 'warning');
      return;
    }

    this.addUserLoading.set(true);

    if (data.role === 'ADMIN' || data.role === 'SUPER_ADMIN') {
      const request = data.role === 'SUPER_ADMIN'
        ? this.apiService.createSuperAdmin({
            email: data.email,
            phone: data.phone,
            password: data.password,
            displayName: data.displayName,
            businessName: data.businessName || undefined,
            address: '',
            avatarUrl: ''
          })
        : this.apiService.createAdmin({
            email: data.email,
            phone: data.phone,
            password: data.password,
            displayName: data.displayName,
            businessName: data.businessName || undefined,
            address: '',
            avatarUrl: ''
          });

      request.subscribe({
        next: () => {
          this.addUserLoading.set(false);
          this.notificationService.add('Success', `${data.role} ${data.displayName} created.`, 'success');
          this.closeAddUserModal();
          this.loadUsers(true);
        },
        error: (err) => this.handleAddUserError(err)
      });
    } else {
      this.authService.register({
        phone: data.phone,
        email: data.email,
        password: data.password,
        displayName: data.displayName,
        businessName: data.businessName || undefined
      }).subscribe({
        next: (response) => {
          const userId = response.userId;
          
          if (data.role === 'SELLER') {
            this.apiService.updateUserRole(userId, 'SELLER').subscribe({
              next: () => {
                this.apiService.approveSeller(userId, 'Created by admin').subscribe({
                  next: () => {
                    this.addUserLoading.set(false);
                    this.notificationService.add('Success', `Seller ${data.displayName} created and approved.`, 'success');
                    this.closeAddUserModal();
                    this.loadUsers(true);
                  },
                  error: (err) => this.handleAddUserError(err)
                });
              },
              error: (err) => this.handleAddUserError(err)
            });
          } else {
            this.addUserLoading.set(false);
            this.notificationService.add(
              'Success', 
              `Buyer ${data.displayName} created. Send OTP to verify.`,
              'success'
            );
            this.closeAddUserModal();
            this.loadUsers(true);
          }
        },
        error: (err) => this.handleAddUserError(err)
      });
    }
  }

  private validatePassword(password: string): string | null {
    if (password.length < 8 || password.length > 64) {
      return 'Password must be 8–64 characters long.';
    }
    const regex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,64}$/;
    if (!regex.test(password)) {
      return 'Must include uppercase, lowercase, digit, and special character.';
    }
    return null;
  }

  private handleAddUserError(err: any): void {
    this.addUserLoading.set(false);
    console.error('Add user error:', err);
    
    if (err.status === 400 || err.error?.error === 'VALIDATION_ERROR') {
      const message = err.error?.message || '';
      if (message.toLowerCase().includes('password')) {
        this.fieldErrors.set({ password: message });
      } else if (message.toLowerCase().includes('email')) {
        this.fieldErrors.set({ email: message });
      } else if (message.toLowerCase().includes('phone')) {
        this.fieldErrors.set({ phone: message });
      } else {
        this.notificationService.add('Validation Error', message, 'warning');
      }
    } else if (err.status === 409) {
      this.notificationService.add('Error', 'Email or phone already registered.', 'danger');
    } else {
      this.notificationService.add('Error', 'Failed to create user. Try again.', 'danger');
    }
  }

  // ------------------- Profile Modal -------------------
  viewProfile(user: User): void {
    this.selectedUserForProfile.set(user);
    this.showProfileModal.set(true);
  }

  closeProfileModal(): void {
    this.showProfileModal.set(false);
    this.selectedUserForProfile.set(null);
  }

  onRowClick(user: User, event: MouseEvent): void {
    const target = event.target as HTMLElement;
    if (target.closest('.actions-column') || target.closest('input[type="checkbox"]')) {
      return;
    }
    this.viewProfile(user);
  }

  // ------------------- Helpers -------------------
  isBlacklisted(user: User): boolean {
    return user.status === 'BLACKLISTED';
  }

  canActivate(user: User): boolean {
    return user.status === 'SUSPENDED' || user.status === 'PENDING_VERIFICATION';
  }

  canSuspend(user: User): boolean {
    return user.status === 'ACTIVE';
  }

  canVerify(user: User): boolean {
    return user.status === 'PENDING_VERIFICATION';
  }

  canForceActivate(user: User): boolean {
    const currentUser = this.authService.user();
    return currentUser?.role === 'SUPER_ADMIN' && user.status === 'PENDING_VERIFICATION';
  }

  getStatusBadgeClass(status: string): string {
    switch(status) {
      case 'ACTIVE': return 'bg-green-100 text-green-800';
      case 'SUSPENDED': return 'bg-red-100 text-red-800';
      case 'PENDING_VERIFICATION': return 'bg-yellow-100 text-yellow-800';
      case 'PENDING_ADMIN_APPROVAL': return 'bg-blue-100 text-blue-800';
      case 'BLACKLISTED': return 'bg-gray-100 text-gray-800';
      default: return 'bg-gray-100 text-gray-600';
    }
  }

  getBlacklistStatusText(blacklistStatus: string): string {
    switch(blacklistStatus) {
      case 'PERMANENTLY_BANNED': return 'Blacklisted';
      case 'UNDER_INVESTIGATION': return 'Under Investigation';
      case 'TEMPORARILY_MUTED': return 'Temporarily Muted';
      default: return 'Clean';
    }
  }

  getRoleBadgeClass(role: string): string {
    switch(role) {
      case 'BUYER': return 'bg-blue-100 text-blue-800';
      case 'SELLER': return 'bg-purple-100 text-purple-800';
      case 'ADMIN': return 'bg-red-100 text-red-800';
      case 'SUPER_ADMIN': return 'bg-amber-100 text-amber-800';
      default: return 'bg-gray-100 text-gray-600';
    }
  }
}
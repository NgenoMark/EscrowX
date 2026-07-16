// src/app/features/users/riders/riders.ts

import { Component, inject, signal, OnInit, computed, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { DataService } from '../../../core/services/data';
import { ApiService, ApiUserDetails } from '../../../core/services/api.service';
import { NotificationService } from '../../../core/services/notifications';
import { User } from '../../../core/models/user';
import { Rider } from '../../../core/models/rider';
import { CacheService } from '../../../core/services/cache.service';
import { NgxSmkSkeletonDirective } from 'ngxsmk-skeleton-loader';

type ActionType = 'suspend' | 'blacklist' | 'activate' | 'restore' | 'verify' | 'sendOtp' | 'forceActivate';

const CACHE_KEY_RIDERS = 'admin_riders';

@Component({
  selector: 'app-riders',
  standalone: true,
  imports: [CommonModule, FormsModule, NgxSmkSkeletonDirective],
  templateUrl: './riders.html',
  styleUrls: ['./riders.css']
})
export class RidersComponent implements OnInit, OnDestroy {
  private dataService = inject(DataService);
  private apiService = inject(ApiService);
  private notificationService = inject(NotificationService);
  private cacheService = inject(CacheService);

  isLoading = signal<boolean>(true);
  searchTerm = signal<string>('');

  // Filters
  filterStatus = signal<string>('ALL');
  filterBlacklist = signal<string>('ALL');

  private directRidersSignal = signal<Rider[]>([]);
  riders = this.directRidersSignal.asReadonly();

  // Pagination
  currentPage = signal<number>(0);
  pageSize = signal<number>(20);
  totalElements = signal<number>(0);
  totalPages = signal<number>(0);

  // Multi-select
  selectedIds = signal<Set<string>>(new Set());

  // OTP Verification State
  otpSending = signal<Record<string, boolean>>({});
  otpSent = signal<Record<string, boolean>>({});
  otpTimer = signal<Record<string, number>>({});
  private timerInterval: any;

  // Action modal (single user)
  showModal = signal(false);
  modalAction = signal<ActionType | null>(null);
  modalUser = signal<User | null>(null);
  modalReason = signal('');
  otpCode = signal('');

  // Profile modal
  showProfileModal = signal(false);
  selectedUserForProfile = signal<User | null>(null);

  // Bulk action modal
  showBulkModal = signal(false);
  bulkActionType = signal<ActionType | null>(null);
  bulkReason = signal('');

  // Computed filtered riders - uses DataService as fallback
  filteredRiders = computed<Rider[]>(() => {
    // Use direct riders if available, otherwise fallback to DataService
    const riders = this.directRidersSignal().length 
      ? this.directRidersSignal() 
      : this.dataService.users().filter(u => u.role === 'RIDER') as Rider[];
    const term = this.searchTerm().toLowerCase();
    const status = this.filterStatus();
    const blacklist = this.filterBlacklist();

    return riders.filter(u => {
      if (term) {
        const matchesTerm = (u.displayName || '').toLowerCase().includes(term)
          || String(u.phone || '').toLowerCase().includes(term)
          || (u.email || '').toLowerCase().includes(term);
        if (!matchesTerm) return false;
      }
      if (status && status !== 'ALL' && u.status !== status) return false;
      if (blacklist && blacklist !== 'ALL' && u.blacklistStatus !== blacklist) return false;
      return true;
    });
  });

  // Stats
  totalRiders = computed(() => this.filteredRiders().length);
  activeRiders = computed(() => this.filteredRiders().filter(u => u.status === 'ACTIVE').length);
  pendingRiders = computed(() => this.filteredRiders().filter(u => u.status === 'PENDING_VERIFICATION').length);
  suspendedRiders = computed(() => this.filteredRiders().filter(u => u.status === 'SUSPENDED').length);
  blacklistedRiders = computed(() => this.filteredRiders().filter(u => u.status === 'BLACKLISTED').length);

  // Selection helpers
  isAllSelected = computed(() => {
    const filtered = this.filteredRiders();
    const selected = this.selectedIds();
    return filtered.length > 0 && filtered.every(u => selected.has(u.id));
  });

  isSomeSelected = computed(() => {
    const filtered = this.filteredRiders();
    const selected = this.selectedIds();
    return !this.isAllSelected() && selected.size > 0 && filtered.some(u => selected.has(u.id));
  });

  selectedCount = computed(() => this.selectedIds().size);

  ngOnInit() {
    this.loadRiders();
  }

  ngOnDestroy() {
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
    }
  }

  // Helper for displaying pagination info
  getStartIndex(): number {
    return this.currentPage() * this.pageSize() + 1;
  }

  getEndIndex(): number {
    return Math.min((this.currentPage() + 1) * this.pageSize(), this.totalElements());
  }

  // ------------------- Data Loading -------------------
  loadRiders(forceRefresh = false): void {
    if (forceRefresh) {
      this.cacheService.clear(CACHE_KEY_RIDERS);
    }

    // Try cache first
    const cached = this.cacheService.get<Rider[]>(CACHE_KEY_RIDERS);
    if (cached && !forceRefresh) {
      this.directRidersSignal.set(cached);
      this.isLoading.set(false);
      return;
    }

    // If DataService already has riders loaded, use them
    const dataServiceRiders = this.dataService.users().filter(u => u.role === 'RIDER') as Rider[];
    if (dataServiceRiders.length > 0 && !forceRefresh) {
      this.directRidersSignal.set(dataServiceRiders);
      this.totalElements.set(dataServiceRiders.length);
      this.totalPages.set(1);
      this.cacheService.set(CACHE_KEY_RIDERS, dataServiceRiders);
      this.isLoading.set(false);
      return;
    }

    // Fetch from API
    this.isLoading.set(true);
    this.apiService.getRiders({
      page: this.currentPage(),
      size: this.pageSize()
    }).subscribe({
      next: (response) => {
        if (!response || !response.content) {
          // If API fails, try DataService again (maybe it loaded in the meantime)
          const fallbackRiders = this.dataService.users().filter(u => u.role === 'RIDER') as Rider[];
          if (fallbackRiders.length > 0) {
            this.directRidersSignal.set(fallbackRiders);
            this.totalElements.set(fallbackRiders.length);
            this.totalPages.set(1);
            this.cacheService.set(CACHE_KEY_RIDERS, fallbackRiders);
            this.notificationService.add('Info', 'Using mock data for riders.', 'info');
          } else {
            this.notificationService.add('Data Error', 'Received invalid rider data from server.', 'danger');
          }
          this.isLoading.set(false);
          return;
        }
        const riders = response.content.map(apiUser => this.mapApiUser(apiUser)) as Rider[];
        this.directRidersSignal.set(riders);
        this.totalElements.set(response.totalElements);
        this.totalPages.set(response.totalPages);
        this.cacheService.set(CACHE_KEY_RIDERS, riders);
        this.isLoading.set(false);
      },
      error: (err: any) => {
        console.error('Failed to fetch riders:', err);
        
        // Fallback to DataService mock data
        const fallbackRiders = this.dataService.users().filter(u => u.role === 'RIDER') as Rider[];
        if (fallbackRiders.length > 0) {
          this.directRidersSignal.set(fallbackRiders);
          this.totalElements.set(fallbackRiders.length);
          this.totalPages.set(1);
          this.cacheService.set(CACHE_KEY_RIDERS, fallbackRiders);
          this.notificationService.add('Info', 'Using mock data for riders.', 'info');
        } else {
          const errorMsg = err.status === 401 ? 'Session expired. Please log in again.' :
                           err.status === 403 ? 'You do not have permission to view riders.' :
                           'Could not load riders. Using mock data.';
          this.notificationService.add('Error', errorMsg, 'danger');
        }
        this.isLoading.set(false);
      }
    });
  }

  refreshRiders(): void {
    this.cacheService.clear(CACHE_KEY_RIDERS);
    this.loadRiders(true);
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
    this.searchTerm.set(input.value);
  }

  clearSearch(): void {
    this.searchTerm.set('');
  }

  clearFilters(): void {
    this.filterStatus.set('ALL');
    this.filterBlacklist.set('ALL');
    this.searchTerm.set('');
  }

  // ------------------- Pagination -------------------
  goToPage(page: number): void {
    if (page >= 0 && page < this.totalPages()) {
      this.currentPage.set(page);
      this.loadRiders(true);
    }
  }

  nextPage(): void {
    if (this.currentPage() < this.totalPages() - 1) {
      this.currentPage.set(this.currentPage() + 1);
      this.loadRiders(true);
    }
  }

  prevPage(): void {
    if (this.currentPage() > 0) {
      this.currentPage.set(this.currentPage() - 1);
      this.loadRiders(true);
    }
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
      const ids = this.filteredRiders().map(u => u.id);
      this.selectedIds.set(new Set(ids));
    } else {
      this.selectedIds.set(new Set());
    }
  }

  clearSelection(): void {
    this.selectedIds.set(new Set());
  }

  // ------------------- Bulk Actions -------------------
  bulkAction(action: ActionType): void {
    const selectedRiders = this.filteredRiders().filter(u => this.selectedIds().has(u.id));
    if (selectedRiders.length === 0) return;

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
    const selectedRiders = this.filteredRiders().filter(u => this.selectedIds().has(u.id));
    if (selectedRiders.length === 0) return;

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

    const requests = selectedRiders.map(user =>
      this.apiService.updateUserStatus(user.id, status, apiReason)
    );

    forkJoin(requests).subscribe({
      next: () => {
        this.isLoading.set(false);
        this.notificationService.add(
          'Success',
          `Bulk action '${action}' applied to ${selectedRiders.length} rider(s).`,
          'success'
        );
        this.loadRiders(true);
        this.clearSelection();
      },
      error: (err) => {
        this.isLoading.set(false);
        console.error('Bulk action error:', err);
        this.notificationService.add('Error', 'Some actions failed. Check logs.', 'danger');
        this.loadRiders(true);
        this.clearSelection();
      }
    });
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

  // ------------------- Single Action Modal -------------------
  openActionModal(user: User, action: ActionType): void {
    this.modalUser.set(user);
    this.modalAction.set(action);
    this.modalReason.set('');
    this.otpCode.set('');
    this.showModal.set(true);
  }

  closeModal(): void {
    this.showModal.set(false);
    this.modalUser.set(null);
    this.modalAction.set(null);
    this.modalReason.set('');
    this.otpCode.set('');
  }

  confirmAction(): void {
    const user = this.modalUser();
    const action = this.modalAction();
    const reason = this.modalReason().trim();

    if (!user || !action) return;

    switch (action) {
      case 'verify':
        if (!this.otpCode()) {
          this.notificationService.add('Validation Error', 'Please enter the OTP sent to the user.', 'warning');
          return;
        }
        this.apiService.verifyUserOtp(user.id, this.otpCode()).subscribe({
          next: () => {
            this.loadRiders(true);
            this.closeModal();
            this.notificationService.add('Success', `Rider ${user.displayName} verified and activated.`, 'success');
          },
          error: (err) => {
            const msg = err.error?.message || 'Invalid OTP. Please try again.';
            this.notificationService.add('Error', msg, 'danger');
          }
        });
        break;

      case 'sendOtp':
        this.sendOtpToUser(user);
        this.closeModal();
        break;

      case 'forceActivate':
        this.apiService.updateUserStatus(user.id, 'ACTIVE', 'Force activated by admin').subscribe({
          next: () => {
            this.loadRiders(true);
            this.closeModal();
            this.notificationService.add('Success', `Rider ${user.displayName} force activated.`, 'success');
          },
          error: (err) => this.handleError(err, 'activate')
        });
        break;

      case 'suspend':
        if (!reason) {
          this.notificationService.add('Validation Error', 'Please provide a reason for suspension.', 'warning');
          return;
        }
        this.apiService.updateUserStatus(user.id, 'SUSPENDED', reason).subscribe({
          next: () => {
            this.loadRiders(true);
            this.closeModal();
            this.notificationService.add('Success', `Rider ${user.displayName} suspended.`, 'success');
          },
          error: (err) => this.handleError(err, 'suspend')
        });
        break;

      case 'blacklist':
        if (!reason) {
          this.notificationService.add('Validation Error', 'Please provide a reason for blacklisting.', 'warning');
          return;
        }
        this.apiService.updateUserStatus(user.id, 'BLACKLISTED', reason).subscribe({
          next: () => {
            this.loadRiders(true);
            this.closeModal();
            this.notificationService.add('Success', `Rider ${user.displayName} blacklisted.`, 'success');
          },
          error: (err) => this.handleError(err, 'blacklist')
        });
        break;

      case 'activate':
        this.apiService.updateUserStatus(user.id, 'ACTIVE').subscribe({
          next: () => {
            this.loadRiders(true);
            this.closeModal();
            this.notificationService.add('Success', `Rider ${user.displayName} activated.`, 'success');
          },
          error: (err) => this.handleError(err, 'activate')
        });
        break;

      case 'restore':
        this.apiService.updateUserStatus(user.id, 'PENDING_VERIFICATION', 'Removed from blacklist by admin').subscribe({
          next: () => {
            this.loadRiders(true);
            this.closeModal();
            this.notificationService.add('Success', `Rider ${user.displayName} restored.`, 'success');
          },
          error: (err) => this.handleError(err, 'restore')
        });
        break;
    }
  }

  private handleError(err: any, action: string): void {
    console.error(err);
    const msg = err.status === 401 ? 'Session expired. Please log in again.' :
                err.status === 403 ? `You do not have permission to ${action} riders.` :
                `Failed to ${action} rider. Please try again.`;
    this.notificationService.add('Error', msg, 'danger');
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
    return user.status === 'PENDING_VERIFICATION';
  }

  getRoleBadgeClass(role: string): string {
    switch(role) {
      case 'RIDER': return 'bg-emerald-100 text-emerald-800';
      case 'BUYER': return 'bg-blue-100 text-blue-800';
      case 'SELLER': return 'bg-purple-100 text-purple-800';
      case 'ADMIN': return 'bg-red-100 text-red-800';
      case 'SUPER_ADMIN': return 'bg-amber-100 text-amber-800';
      default: return 'bg-gray-100 text-gray-600';
    }
  }

  getStatusBadgeClass(status: string): string {
    switch(status) {
      case 'ACTIVE': return 'bg-green-100 text-green-800';
      case 'SUSPENDED': return 'bg-red-100 text-red-800';
      case 'PENDING_VERIFICATION': return 'bg-yellow-100 text-yellow-800';
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
}
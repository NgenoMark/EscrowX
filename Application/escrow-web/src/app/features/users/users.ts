import { Component, inject, signal, OnInit, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { DataService } from '../../core/services/data';
import { SearchService } from '../../core/services/search';
import { ApiService, ApiUserDetails } from '../../core/services/api.service';
import { NotificationService } from '../../core/services/notifications';
import { User } from '../../core/models/user';
import { CacheService } from '../../core/services/cache.service';
import { NgxSmkSkeletonDirective } from 'ngxsmk-skeleton-loader';

type ActionType = 'suspend' | 'blacklist' | 'activate' | 'restore';

const CACHE_KEY_USERS = 'admin_users';

@Component({
  selector: 'app-users',
  standalone: true,
  imports: [CommonModule, FormsModule, NgxSmkSkeletonDirective],
  templateUrl: './users.html',
  styleUrls: ['./users.css']
})
export class UsersComponent implements OnInit {
  private dataService = inject(DataService);
  private searchService = inject(SearchService);
  private apiService = inject(ApiService);
  private notificationService = inject(NotificationService);
  private cacheService = inject(CacheService);

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

  // Action modal (single user)
  showModal = signal(false);
  modalAction = signal<ActionType | null>(null);
  modalUser = signal<User | null>(null);
  modalReason = signal('');

  // Profile modal
  showProfileModal = signal(false);
  selectedUserForProfile = signal<User | null>(null);

  // Bulk action modal
  showBulkModal = signal(false);
  bulkActionType = signal<ActionType | null>(null);
  bulkReason = signal('');

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

  // Stats (based on filtered)
  totalUsers = computed(() => this.filteredUsers().length);
  activeUsers = computed(() => this.filteredUsers().filter(u => u.status === 'ACTIVE').length);
  suspendedUsers = computed(() => this.filteredUsers().filter(u => u.status === 'SUSPENDED').length);
  phoneVerifiedUsers = computed(() => this.filteredUsers().filter(u => u.status === 'ACTIVE').length);
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

  // ------------------- Data Loading -------------------
  loadUsers(forceRefresh = false): void {
    if (forceRefresh) {
      console.log('🔄 Force refreshing users, clearing cache...');
      this.cacheService.clear(CACHE_KEY_USERS);
    }
    const cached = this.cacheService.get<User[]>(CACHE_KEY_USERS);
    if (cached) {
      console.log('✅ Users loaded from cache:', cached.length, 'users');
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
        console.log('✅ Users fetched from API:', users.length, 'users');
        this.directUsersSignal.set(users);
        this.cacheService.set(CACHE_KEY_USERS, users);
        this.isLoading.set(false);
      },
      error: (err: any) => {
        console.error('❌ Failed to fetch users:', err);
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

  // ------------------- Bulk Actions -------------------
  bulkAction(action: ActionType): void {
    const selectedUsers = this.filteredUsers().filter(u => this.selectedIds().has(u.id));
    if (selectedUsers.length === 0) return;

    if (action === 'suspend' || action === 'blacklist') {
      this.openBulkActionModal(action);
    } else {
      // activate / restore (no reason needed)
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
    this.showModal.set(true);
  }

  closeModal(): void {
    this.showModal.set(false);
    this.modalUser.set(null);
    this.modalAction.set(null);
    this.modalReason.set('');
  }

  confirmAction(): void {
    const user = this.modalUser();
    const action = this.modalAction();
    const reason = this.modalReason().trim();

    if (!user || !action) return;

    switch (action) {
      case 'suspend':
        if (!reason) {
          this.notificationService.add('Validation Error', 'Please provide a reason for suspension.', 'warning');
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
          this.notificationService.add('Validation Error', 'Please provide a reason for blacklisting.', 'warning');
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
    }
  }

  private handleError(err: any, action: string): void {
    console.error(err);
    const msg = err.status === 401 ? 'Session expired. Please log in again.' :
                err.status === 403 ? `You do not have permission to ${action} users.` :
                `Failed to ${action} user. Please try again.`;
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

  getRoleBadgeClass(role: string): string {
    switch(role) {
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
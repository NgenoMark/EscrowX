// src/app/features/users/admins/admins.ts

import { Component, inject, signal, OnInit, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { ApiService, ApiUserDetails } from '../../../core/services/api.service';
import { NotificationService } from '../../../core/services/notifications';
import { User } from '../../../core/models/user';
import { CacheService } from '../../../core/services/cache.service';
import { NgxSmkSkeletonDirective } from 'ngxsmk-skeleton-loader';

type ActionType = 'suspend' | 'blacklist' | 'activate' | 'restore';
type AdminType = 'ADMIN' | 'SUPER_ADMIN';

const CACHE_KEY_ADMINS = 'admin_employees';

@Component({
  selector: 'app-admins',
  standalone: true,
  imports: [CommonModule, FormsModule, NgxSmkSkeletonDirective],
  templateUrl: './admins.html',
  styleUrls: ['./admins.css']
})
export class AdminsComponent implements OnInit {
  private apiService = inject(ApiService);
  private notificationService = inject(NotificationService);
  private cacheService = inject(CacheService);

  isLoading = signal<boolean>(true);
  searchTerm = signal<string>('');

  // Filters
  filterRole = signal<string>('ALL');
  filterStatus = signal<string>('ALL');
  filterBlacklist = signal<string>('ALL');

  private adminsSignal = signal<User[]>([]);
  admins = this.adminsSignal.asReadonly();

  // Pagination
  currentPage = signal<number>(0);
  pageSize = signal<number>(20);
  totalElements = signal<number>(0);
  totalPages = signal<number>(0);

  // Multi-select
  selectedIds = signal<Set<string>>(new Set());

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

  // Create Admin Modal
  showCreateAdminModal = signal(false);
  newAdmin = signal<{
    email: string;
    phone: string;
    password: string;
    displayName: string;
    businessName: string;
    address: string;
    avatarUrl: string;
    adminType: AdminType;
  }>({
    email: '',
    phone: '',
    password: '',
    displayName: '',
    businessName: '',
    address: '',
    avatarUrl: '',
    adminType: 'ADMIN'
  });

  // Computed filtered admins
  filteredAdmins = computed<User[]>(() => {
    const admins = this.adminsSignal();
    const term = this.searchTerm().toLowerCase();
    const status = this.filterStatus();
    const role = this.filterRole();
    const blacklist = this.filterBlacklist();

    return admins.filter(u => {
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
  totalAdmins = computed(() => this.filteredAdmins().length);
  activeAdmins = computed(() => this.filteredAdmins().filter(u => u.status === 'ACTIVE').length);
  suspendedAdmins = computed(() => this.filteredAdmins().filter(u => u.status === 'SUSPENDED').length);
  superAdmins = computed(() => this.filteredAdmins().filter(u => u.role === 'SUPER_ADMIN').length);
  regularAdmins = computed(() => this.filteredAdmins().filter(u => u.role === 'ADMIN').length);

  // Selection helpers
  isAllSelected = computed(() => {
    const filtered = this.filteredAdmins();
    const selected = this.selectedIds();
    return filtered.length > 0 && filtered.every(u => selected.has(u.id));
  });

  isSomeSelected = computed(() => {
    const filtered = this.filteredAdmins();
    const selected = this.selectedIds();
    return !this.isAllSelected() && selected.size > 0 && filtered.some(u => selected.has(u.id));
  });

  selectedCount = computed(() => this.selectedIds().size);

  ngOnInit() {
    this.loadAdmins();
  }

  // Helper for Math.min in template
  getMinValue(a: number, b: number): number {
    return Math.min(a, b);
  }

  // Helper for displaying pagination info
  getStartIndex(): number {
    return this.currentPage() * this.pageSize() + 1;
  }

  getEndIndex(): number {
    return Math.min((this.currentPage() + 1) * this.pageSize(), this.totalElements());
  }

  // ------------------- Data Loading -------------------
  loadAdmins(forceRefresh = false): void {
    if (forceRefresh) {
      this.cacheService.clear(CACHE_KEY_ADMINS);
    }

    const cached = this.cacheService.get<User[]>(CACHE_KEY_ADMINS);
    if (cached && !forceRefresh) {
      this.adminsSignal.set(cached);
      this.isLoading.set(false);
      return;
    }

    this.isLoading.set(true);
    this.apiService.getEmployees({
      page: this.currentPage(),
      size: this.pageSize()
    }).subscribe({
      next: (response) => {
        if (!response || !response.content) {
          this.notificationService.add('Data Error', 'Received invalid admin data from server.', 'danger');
          this.isLoading.set(false);
          return;
        }
        const admins = response.content.map(apiUser => this.mapApiUser(apiUser));
        this.adminsSignal.set(admins);
        this.totalElements.set(response.totalElements);
        this.totalPages.set(response.totalPages);
        this.cacheService.set(CACHE_KEY_ADMINS, admins);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Failed to fetch admins:', err);
        const errorMsg = err.status === 401 ? 'Session expired. Please log in again.' :
                         err.status === 403 ? 'You do not have permission to view admins.' :
                         'Could not load admins. Please try again later.';
        this.notificationService.add('Error', errorMsg, 'danger');
        this.isLoading.set(false);
      }
    });
  }

  refreshAdmins(): void {
    this.loadAdmins(true);
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
    this.searchTerm.set(input.value);
  }

  clearSearch(): void {
    this.searchTerm.set('');
  }

  clearFilters(): void {
    this.filterStatus.set('ALL');
    this.filterRole.set('ALL');
    this.filterBlacklist.set('ALL');
    this.searchTerm.set('');
  }

  // ------------------- Pagination -------------------
  goToPage(page: number): void {
    if (page >= 0 && page < this.totalPages()) {
      this.currentPage.set(page);
      this.loadAdmins(true);
    }
  }

  nextPage(): void {
    if (this.currentPage() < this.totalPages() - 1) {
      this.currentPage.set(this.currentPage() + 1);
      this.loadAdmins(true);
    }
  }

  prevPage(): void {
    if (this.currentPage() > 0) {
      this.currentPage.set(this.currentPage() - 1);
      this.loadAdmins(true);
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
      const ids = this.filteredAdmins().map(u => u.id);
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
    const selectedAdmins = this.filteredAdmins().filter(u => this.selectedIds().has(u.id));
    if (selectedAdmins.length === 0) return;

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
    const selectedAdmins = this.filteredAdmins().filter(u => this.selectedIds().has(u.id));
    if (selectedAdmins.length === 0) return;

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

    const requests = selectedAdmins.map(user =>
      this.apiService.updateUserStatus(user.id, status, apiReason)
    );

    forkJoin(requests).subscribe({
      next: () => {
        this.isLoading.set(false);
        this.notificationService.add(
          'Success',
          `Bulk action '${action}' applied to ${selectedAdmins.length} admin(s).`,
          'success'
        );
        this.loadAdmins(true);
        this.clearSelection();
      },
      error: (err) => {
        this.isLoading.set(false);
        console.error('Bulk action error:', err);
        this.notificationService.add('Error', 'Some actions failed. Check logs.', 'danger');
        this.loadAdmins(true);
        this.clearSelection();
      }
    });
  }

  // ------------------- Create Admin -------------------
  openCreateAdminModal(): void {
    this.newAdmin.set({
      email: '',
      phone: '',
      password: '',
      displayName: '',
      businessName: '',
      address: '',
      avatarUrl: '',
      adminType: 'ADMIN'
    });
    this.showCreateAdminModal.set(true);
  }

  closeCreateAdminModal(): void {
    this.showCreateAdminModal.set(false);
  }

  createAdmin(): void {
    const data = this.newAdmin();
    
    // Validation
    if (!data.email || !data.phone || !data.password || !data.displayName) {
      this.notificationService.add('Validation Error', 'Please fill in all required fields.', 'warning');
      return;
    }

    this.isLoading.set(true);

    const request = data.adminType === 'SUPER_ADMIN'
      ? this.apiService.createSuperAdmin(data)
      : this.apiService.createAdmin(data);

    request.subscribe({
      next: () => {
        this.isLoading.set(false);
        this.notificationService.add(
          'Success',
          `${data.adminType} ${data.displayName} created successfully.`,
          'success'
        );
        this.closeCreateAdminModal();
        this.loadAdmins(true);
      },
      error: (err) => {
        this.isLoading.set(false);
        console.error('Create admin error:', err);
        const msg = err.status === 400 ? 'Invalid data provided. Please check the form.' :
                    err.status === 409 ? 'A user with this email or phone already exists.' :
                    'Failed to create admin. Please try again.';
        this.notificationService.add('Error', msg, 'danger');
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
            this.loadAdmins(true);
            this.closeModal();
            this.notificationService.add('Success', `Admin ${user.displayName} suspended.`, 'success');
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
            this.loadAdmins(true);
            this.closeModal();
            this.notificationService.add('Success', `Admin ${user.displayName} blacklisted.`, 'success');
          },
          error: (err) => this.handleError(err, 'blacklist')
        });
        break;

      case 'activate':
        this.apiService.updateUserStatus(user.id, 'ACTIVE').subscribe({
          next: () => {
            this.loadAdmins(true);
            this.closeModal();
            this.notificationService.add('Success', `Admin ${user.displayName} activated.`, 'success');
          },
          error: (err) => this.handleError(err, 'activate')
        });
        break;

      case 'restore':
        this.apiService.updateUserStatus(user.id, 'PENDING_VERIFICATION', 'Removed from blacklist by admin').subscribe({
          next: () => {
            this.loadAdmins(true);
            this.closeModal();
            this.notificationService.add('Success', `Admin ${user.displayName} restored.`, 'success');
          },
          error: (err) => this.handleError(err, 'restore')
        });
        break;
    }
  }

  private handleError(err: any, action: string): void {
    console.error(err);
    const msg = err.status === 401 ? 'Session expired. Please log in again.' :
                err.status === 403 ? `You do not have permission to ${action} admins.` :
                `Failed to ${action} admin. Please try again.`;
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
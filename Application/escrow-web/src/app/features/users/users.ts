import { Component, inject, signal, OnInit, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DataService } from '../../core/services/data';
import { SearchService } from '../../core/services/search';
import { ApiService, ApiUserDetails } from '../../core/services/api.service';
import { User } from '../../core/models/user';

@Component({
  selector: 'app-users',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './users.html',
  styleUrls: ['./users.css']
})
export class UsersComponent implements OnInit {
  private dataService = inject(DataService);
  private searchService = inject(SearchService);
  private apiService = inject(ApiService);

  // Local signal to hold users fetched directly from API
  private directUsersSignal = signal<User[]>([]);
  directUsers = this.directUsersSignal.asReadonly();

  searchTerm = this.searchService.query;

  // Method instead of getter – allows template to call filteredUsers()
  filteredUsers(): User[] {
    const users = this.directUsersSignal().length ? this.directUsersSignal() : this.dataService.users();
    const term = this.searchTerm().toLowerCase();
    if (!term) return users;
    return users.filter(user =>
      user.displayName.toLowerCase().includes(term) ||
      user.phone.includes(term) ||
      user.email.toLowerCase().includes(term)
    );
  }

  // Computed signals now call filteredUsers() as a method
  totalUsers = computed(() => this.filteredUsers().length);
  activeUsers = computed(() => this.filteredUsers().filter(u => u.status === 'ACTIVE').length);
  suspendedUsers = computed(() => this.filteredUsers().filter(u => u.status === 'SUSPENDED').length);
  phoneVerifiedUsers = computed(() => this.filteredUsers().filter(u => u.status === 'ACTIVE').length);
  blacklistedCount = computed(() => this.filteredUsers().filter(u => u.blacklistStatus === 'PERMANENTLY_BANNED').length);
  underInvestigationCount = computed(() => this.filteredUsers().filter(u => u.blacklistStatus === 'UNDER_INVESTIGATION').length);

  ngOnInit() {
    console.log('🟢 UsersComponent – fetching users directly from API');
    this.apiService.getUsers({ size: 500 }).subscribe({
      next: (response) => {
        console.log('✅ Raw API response for users:', response);
        const users = response.content.map(apiUser => this.mapApiUser(apiUser));
        this.directUsersSignal.set(users);
        console.log(`✅ Loaded ${users.length} users directly`);
      },
      error: (err) => {
        console.error('❌ Failed to fetch users directly:', err);
      }
    });
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

  onSearch(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.searchService.setQuery(input.value);
  }

  clearSearch(): void {
    this.searchService.clear();
  }

  suspendUser(user: User): void {
    if (user.blacklistStatus === 'PERMANENTLY_BANNED') {
      alert('Cannot suspend a permanently banned user.');
      return;
    }
    if (user.status === 'SUSPENDED') {
      alert('User is already suspended.');
      return;
    }
    if (confirm(`⚠️ Are you sure you want to SUSPEND ${user.displayName}?`)) {
      this.dataService.suspendUser(user.id);
      this.ngOnInit(); // refresh list
    }
  }

  activateUser(user: User): void {
    if (user.status === 'ACTIVE') {
      alert('User is already active.');
      return;
    }
    if (user.blacklistStatus === 'PERMANENTLY_BANNED') {
      alert('Cannot activate a permanently banned user.');
      return;
    }
    if (confirm(`✅ Activate ${user.displayName}?`)) {
      this.dataService.activateUser(user.id);
      this.ngOnInit();
    }
  }

  blacklistUser(user: User): void {
    if (user.blacklistStatus === 'PERMANENTLY_BANNED') {
      alert('User is already blacklisted.');
      return;
    }
    if (confirm(`🚫 PERMANENT ACTION: Blacklist ${user.displayName}?`)) {
      this.dataService.blacklistUser(user.id);
      this.ngOnInit();
    }
  }

  removeBlacklist(user: User): void {
    if (user.blacklistStatus !== 'PERMANENTLY_BANNED') {
      alert('User is not blacklisted.');
      return;
    }
    if (confirm(`🔄 Remove ${user.displayName} from blacklist?`)) {
      this.dataService.removeBlacklist(user.id);
      this.ngOnInit();
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

  getStatusBadgeClass(status: string): string {
    switch(status) {
      case 'ACTIVE': return 'bg-green-100 text-green-800';
      case 'SUSPENDED': return 'bg-red-100 text-red-800';
      case 'PENDING_VERIFICATION': return 'bg-yellow-100 text-yellow-800';
      case 'BLACKLISTED': return 'bg-gray-100 text-gray-800';
      default: return 'bg-gray-100 text-gray-600';
    }
  }

  getBlacklistStatusBadgeClass(blacklistStatus: string): string {
    switch(blacklistStatus) {
      case 'PERMANENTLY_BANNED': return 'bg-red-100 text-red-800';
      case 'UNDER_INVESTIGATION': return 'bg-orange-100 text-orange-800';
      case 'TEMPORARILY_MUTED': return 'bg-amber-100 text-amber-800';
      default: return 'bg-green-100 text-green-800';
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

  isBlacklisted(user: User): boolean {
    return user.blacklistStatus === 'PERMANENTLY_BANNED';
  }

  canActivate(user: User): boolean {
    return user.status === 'SUSPENDED' && user.blacklistStatus !== 'PERMANENTLY_BANNED';
  }

  canSuspend(user: User): boolean {
    return user.status === 'ACTIVE' && user.blacklistStatus !== 'PERMANENTLY_BANNED';
  }
}
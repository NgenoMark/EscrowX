import { Component, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DataService } from '../../core/services/data';
import { SearchService } from '../../core/services/search';
import { User } from '../../core/models/user';

@Component({
  selector: 'app-users',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './users.html',
  styleUrls: ['./users.css']
})
export class UsersComponent {
  // Make dataService public so it can be accessed in the template
  public dataService = inject(DataService);
  private searchService = inject(SearchService);
  
  searchTerm = this.searchService.query;
  
  // Computed filtered users – using displayName
  filteredUsers = computed(() => {
    const term = this.searchTerm().toLowerCase();
    const users = this.dataService.users();
    
    if (!term) return users;
    
    return users.filter(user => 
      user.displayName.toLowerCase().includes(term) ||
      user.phone.includes(term) ||
      user.email.toLowerCase().includes(term)
    );
  });

  // Statistics derived from actual User fields
  totalUsers = computed(() => this.dataService.users().length);
  activeUsers = computed(() => this.dataService.users().filter(u => u.status === 'ACTIVE').length);
  suspendedUsers = computed(() => this.dataService.users().filter(u => u.status === 'SUSPENDED').length);
  
  // Phone verification - derived from status
  phoneVerifiedUsers = computed(() => this.dataService.users().filter(u => u.status === 'ACTIVE').length);
  
  // KYC stats - not applicable in base User model, set to 0
  kycApproved = computed(() => 0);
  
  // Blacklisted count based on blacklistStatus
  blacklistedCount = computed(() => this.dataService.users().filter(u => u.blacklistStatus === 'PERMANENTLY_BANNED').length);
  
  // Under investigation count
  underInvestigationCount = computed(() => this.dataService.users().filter(u => u.blacklistStatus === 'UNDER_INVESTIGATION').length);

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
    if (confirm(`⚠️ Are you sure you want to SUSPEND ${user.displayName}?\n\nThey will not be able to create new transactions or access their account until activated.`)) {
      this.dataService.suspendUser(user.id);
    }
  }

  activateUser(user: User): void {
    if (user.status === 'ACTIVE') {
      alert('User is already active.');
      return;
    }
    if (user.blacklistStatus === 'PERMANENTLY_BANNED') {
      alert('Cannot activate a permanently banned user. Remove from blacklist first.');
      return;
    }
    if (confirm(`✅ Activate ${user.displayName}?\n\nThey will regain full access to the platform.`)) {
      this.dataService.activateUser(user.id);
    }
  }

  blacklistUser(user: User): void {
    if (user.blacklistStatus === 'PERMANENTLY_BANNED') {
      alert('User is already blacklisted.');
      return;
    }
    if (confirm(`🚫 PERMANENT ACTION: Blacklisting ${user.displayName}\n\nThis will:\n• Permanently block their phone number\n• Prevent future registrations with this email\n• Suspend their current account\n\nThis action is IRREVERSIBLE. Continue?`)) {
      this.dataService.blacklistUser(user.id);
    }
  }

  removeBlacklist(user: User): void {
    if (user.blacklistStatus !== 'PERMANENTLY_BANNED') {
      alert('User is not blacklisted.');
      return;
    }
    if (confirm(`🔄 Remove ${user.displayName} from blacklist?\n\nThey will be able to register again with the same phone/email. Their account status will be reset to pending verification.`)) {
      this.dataService.removeBlacklist(user.id);
    }
  }

  // Helper methods for badge styling
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

  // Helper to check if user is blacklisted
  isBlacklisted(user: User): boolean {
    return user.blacklistStatus === 'PERMANENTLY_BANNED';
  }

  // Helper to check if user can be activated
  canActivate(user: User): boolean {
    return user.status === 'SUSPENDED' && user.blacklistStatus !== 'PERMANENTLY_BANNED';
  }

  // Helper to check if user can be suspended
  canSuspend(user: User): boolean {
    return user.status === 'ACTIVE' && user.blacklistStatus !== 'PERMANENTLY_BANNED';
  }
}
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
  private dataService = inject(DataService);
  private searchService = inject(SearchService);
  
  searchTerm = this.searchService.query;
  
  // Computed filtered users
  filteredUsers = computed(() => {
    const term = this.searchTerm().toLowerCase();
    const users = this.dataService.users();
    
    if (!term) return users;
    
    return users.filter(user => 
      user.name.toLowerCase().includes(term) ||
      user.phone.includes(term) ||
      user.email.toLowerCase().includes(term)
    );
  });

  // Statistics (Updated for OTP & KYC)
  totalUsers = computed(() => this.dataService.users().length);
  activeUsers = computed(() => this.dataService.users().filter(u => u.status === 'ACTIVE').length);
  suspendedUsers = computed(() => this.dataService.users().filter(u => u.status === 'SUSPENDED').length);
  phoneVerifiedUsers = computed(() => this.dataService.users().filter(u => u.isPhoneVerified === true).length);
  kycApproved = computed(() => this.dataService.users().filter(u => u.kycStatus === 'APPROVED').length);
  blacklistedCount = computed(() => this.dataService.users().filter(u => u.blacklisted === true).length);

  onSearch(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.searchService.setQuery(input.value);
  }

  clearSearch(): void {
    this.searchService.clear();
  }

  suspendUser(user: User): void {
    if (confirm(`⚠️ Are you sure you want to SUSPEND ${user.name}?\n\nThey will not be able to create new transactions or access their account until activated.`)) {
      this.dataService.suspendUser(user.id);
    }
  }

  activateUser(user: User): void {
    if (confirm(`✅ Activate ${user.name}?\n\nThey will regain full access to the platform.`)) {
      this.dataService.activateUser(user.id);
    }
  }

  approveKYC(user: User): void {
    if (confirm(`📋 Approve KYC for ${user.name}?\n\nThis will allow them to have higher transaction limits.`)) {
      this.dataService.approveKYC(user.id);
    }
  }

  rejectKYC(user: User): void {
    if (confirm(`❌ Reject KYC for ${user.name}?\n\nThey will need to resubmit their documents.`)) {
      this.dataService.rejectKYC(user.id);
    }
  }

  blacklistUser(user: User): void {
    if (confirm(`🚫 PERMANENT ACTION: Blacklisting ${user.name}\n\nThis will:\n• Permanently block their phone number\n• Prevent future registrations with this email\n• Suspend their current account\n\nThis action is IRREVERSIBLE. Continue?`)) {
      this.dataService.blacklistUser(user.id);
    }
  }

  removeBlacklist(user: User): void {
    if (confirm(`🔄 Remove ${user.name} from blacklist?\n\nThey will be able to register again with the same phone/email.`)) {
      this.dataService.removeBlacklist(user.id);
    }
  }

  getRoleBadgeClass(role: string): string {
    return role === 'BUYER' 
      ? 'bg-blue-100 text-blue-800' 
      : 'bg-purple-100 text-purple-800';
  }

  getStatusBadgeClass(status: string): string {
    return status === 'ACTIVE'
      ? 'bg-green-100 text-green-800'
      : 'bg-red-100 text-red-800';
  }

  getKYCStatusClass(kycStatus: string): string {
    switch(kycStatus) {
      case 'APPROVED': return 'bg-green-100 text-green-800';
      case 'SUBMITTED': return 'bg-yellow-100 text-yellow-800';
      case 'REJECTED': return 'bg-red-100 text-red-800';
      default: return 'bg-gray-100 text-gray-600';
    }
  }

  getKYCStatusIcon(kycStatus: string): string {
    switch(kycStatus) {
      case 'APPROVED': return 'fa-check-circle';
      case 'SUBMITTED': return 'fa-clock';
      case 'REJECTED': return 'fa-times-circle';
      default: return 'fa-hourglass-half';
    }
  }
}

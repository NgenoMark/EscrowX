import { Component, EventEmitter, inject, Input, Output } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './sidebar.html',
  styleUrls: ['./sidebar.css']
})
export class SidebarComponent {
  @Input() collapsed = false;
  @Input() mobileOpen = false;
  @Output() closeMobile = new EventEmitter<void>();

  private authService = inject(AuthService);

  navItems = [
    { label: 'Dashboard', route: '/dashboard', icon: 'fa-tachometer-alt' },
    { label: 'Users', route: '/users', icon: 'fa-users' },
    { label: 'Transactions', route: '/transactions', icon: 'fa-exchange-alt' },
    { label: 'Disputes', route: '/disputes', icon: 'fa-gavel', count: 3 },
    { label: 'Analytics', route: '/analytics', icon: 'fa-chart-line' },
    { label: 'Audit Logs', route: '/audit', icon: 'fa-history' }
  ];

  // Get current user from AuthService
  get currentUser() {
    return this.authService.user();
  }

  get userDisplayName(): string {
    const user = this.currentUser;
    // Use email or phone as fallback
    return user?.email?.split('@')[0] || user?.phone || 'Admin';
  }

  get userEmail(): string {
    return this.currentUser?.email || '';
  }

  logout(): void {
    this.authService.logout();
    // Sidebar will close if mobile
    this.closeMobile.emit();
  }
}
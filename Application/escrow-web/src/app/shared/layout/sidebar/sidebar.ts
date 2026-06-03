import { Component, EventEmitter, Input, Output } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

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

  navItems = [
    { label: 'Dashboard', route: '/dashboard', icon: 'fa-tachometer-alt' },
    { label: 'Users', route: '/users', icon: 'fa-users' },
    { label: 'Transactions', route: '/transactions', icon: 'fa-exchange-alt' },
    { label: 'Disputes', route: '/disputes', icon: 'fa-gavel', count: 3 },
    { label: 'Analytics', route: '/analytics', icon: 'fa-chart-line' },
    { label: 'Audit Logs', route: '/audit', icon: 'fa-history' }
  ];
}

// src/app/shared/layout/sidebar/sidebar.component.ts
import {
  Component,
  EventEmitter,
  inject,
  Input,
  Output,
  ViewChild,
  ElementRef,
  AfterViewInit,
  OnDestroy,
  HostListener,
  NgZone,
  OnInit
} from '@angular/core';
import { RouterLink, RouterLinkActive, Router, NavigationEnd } from '@angular/router';
import { Subscription } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { DataService } from '../../../core/services/data';

// Define navigation item types
interface NavLinkItem {
  label: string;
  route: string;
  icon: string;
  type: 'link';
  count?: number;
}

interface NavDropdownItem {
  label: string;
  icon: string;
  type: 'dropdown';
  dropdownId: string;
  children: {
    label: string;
    route: string;
    icon: string;
  }[];
  count?: number;
}

type NavItem = NavLinkItem | NavDropdownItem;

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './sidebar.html',
  styleUrls: ['./sidebar.css']
})
export class SidebarComponent implements OnInit, AfterViewInit, OnDestroy {
  @Input() collapsed = false;
  @Input() mobileOpen = false;
  @Output() closeMobile = new EventEmitter<void>();

  @ViewChild('navList') navList!: ElementRef<HTMLElement>;
  @ViewChild('activeIndicator') activeIndicator!: ElementRef<HTMLElement>;

  private authService = inject(AuthService);
  private dataService = inject(DataService);
  private router = inject(Router);
  private ngZone = inject(NgZone);

  private routerSubscription!: Subscription;
  private disputesViewed = false;

  // Track which dropdowns are open
  openDropdowns = new Set<string>();

  // Track if dropdown was opened via click or hover
  private dropdownHoverTimeout: any = null;

  ngOnInit() {
    // Check if any child route is active and open the dropdown
    this.checkAndOpenActiveDropdown();
  }

  ngAfterViewInit() {
    // Initial update after the view is rendered
    setTimeout(() => this.updateIndicator(), 100);

    // Subscribe to router events to update on navigation
    this.routerSubscription = this.router.events.subscribe(event => {
      if (event instanceof NavigationEnd) {
        // Use requestAnimationFrame to let Angular apply the 'nav-active' class
        requestAnimationFrame(() => {
          this.updateIndicator();
          this.checkAndOpenActiveDropdown();
        });
      }
    });
  }

  ngOnDestroy() {
    this.routerSubscription?.unsubscribe();
    if (this.dropdownHoverTimeout) {
      clearTimeout(this.dropdownHoverTimeout);
    }
  }

  // React to input changes (collapsed, mobileOpen)
  ngOnChanges() {
    // Schedule update after the DOM settles
    setTimeout(() => this.updateIndicator());
  }

  // Also update on window resize (optional)
  @HostListener('window:resize')
  onResize() {
    this.updateIndicator();
  }

  /**
   * Check if any dropdown child is active and open it
   */
  private checkAndOpenActiveDropdown(): void {
    const currentUrl = this.router.url;
    const usersDropdown = this.navItems.find(item => item.type === 'dropdown');
    if (usersDropdown && usersDropdown.type === 'dropdown') {
      const isChildActive = usersDropdown.children.some(child => currentUrl.includes(child.route));
      if (isChildActive) {
        this.openDropdowns.add(usersDropdown.dropdownId);
      }
    }
  }

  /**
   * Computes the position of the active nav item and moves the indicator.
   */
  private updateIndicator(): void {
    // Run inside Angular's zone to avoid change detection issues, but we only touch DOM
    this.ngZone.runOutsideAngular(() => {
      const navListEl = this.navList?.nativeElement;
      const indicatorEl = this.activeIndicator?.nativeElement;
      if (!navListEl || !indicatorEl) return;

      // Find the currently active item (class added by routerLinkActive)
      // Check for both regular nav-active and nav-active-child
      let activeItem = navListEl.querySelector('.nav-item.nav-active') as HTMLElement;
      
      // If no nav-active, check for nav-active-child
      if (!activeItem) {
        activeItem = navListEl.querySelector('.nav-item.nav-active-child') as HTMLElement;
        // If we found a child, find its parent dropdown trigger
        if (activeItem) {
          const wrapper = activeItem.closest('.nav-dropdown-wrapper');
          if (wrapper) {
            const trigger = wrapper.querySelector('.nav-dropdown-trigger') as HTMLElement;
            if (trigger) {
              activeItem = trigger;
            }
          }
        }
      }

      if (activeItem) {
        const listRect = navListEl.getBoundingClientRect();
        const itemRect = activeItem.getBoundingClientRect();

        // Relative top and height
        const top = itemRect.top - listRect.top;
        const height = itemRect.height;

        // Apply transform and height with a smooth transition (CSS handles the animation)
        indicatorEl.style.transform = `translateY(${top}px)`;
        indicatorEl.style.height = `${height}px`;
        indicatorEl.style.display = 'block';
      } else {
        // No active route – hide the indicator
        indicatorEl.style.display = 'none';
      }
    });
  }

  // Toggle dropdown open/close
  toggleDropdown(dropdownId: string, event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    
    if (this.openDropdowns.has(dropdownId)) {
      this.openDropdowns.delete(dropdownId);
    } else {
      // Close other dropdowns
      this.openDropdowns.clear();
      this.openDropdowns.add(dropdownId);
    }
  }

  // Open dropdown on hover (for desktop)
  openDropdownOnHover(dropdownId: string): void {
    if (this.dropdownHoverTimeout) {
      clearTimeout(this.dropdownHoverTimeout);
      this.dropdownHoverTimeout = null;
    }
    this.openDropdowns.add(dropdownId);
  }

  // Close dropdown on mouse leave
  closeDropdownOnLeave(dropdownId: string): void {
    if (this.dropdownHoverTimeout) {
      clearTimeout(this.dropdownHoverTimeout);
    }
    this.dropdownHoverTimeout = setTimeout(() => {
      // Don't close if it was opened via click
      if (this.openDropdowns.has(dropdownId)) {
        // Check if mouse is still over the dropdown
        this.openDropdowns.delete(dropdownId);
      }
      this.dropdownHoverTimeout = null;
    }, 300);
  }

  // Check if dropdown is open
  isDropdownOpen(dropdownId: string): boolean {
    return this.openDropdowns.has(dropdownId);
  }

  // Close dropdown when navigating to a sub-item
  closeDropdownOnNavigate(): void {
    // Close all dropdowns after navigation
    this.openDropdowns.clear();
    // Close mobile sidebar if open
    if (this.mobileOpen) {
      this.closeMobile.emit();
    }
  }

  get navItems(): NavItem[] {
    const activeDisputes = this.dataService.disputes().filter(d =>
      d.status === 'PENDING' ||
      d.status === 'UNDER_REVIEW' ||
      d.status === 'OPEN' ||
      d.status === 'ESCALATED'
    ).length;

    // Show count only if disputes haven't been viewed yet
    const count = this.disputesViewed ? 0 : activeDisputes;

    return [
      { 
        label: 'Dashboard', 
        route: '/dashboard', 
        icon: 'fa-tachometer-alt',
        type: 'link'
      },
      { 
        label: 'Users', 
        icon: 'fa-users',
        type: 'dropdown',
        dropdownId: 'users-dropdown',
        children: [
          { label: 'All Users', route: '/users', icon: 'fa-users' },
          { label: 'Buyers', route: '/buyers', icon: 'fa-user' },
          { label: 'Sellers', route: '/sellers', icon: 'fa-store' },
          { label: 'Admins', route: '/admins', icon: 'fa-user-shield' }
        ]
      },
      { 
        label: 'Transactions', 
        route: '/transactions', 
        icon: 'fa-exchange-alt',
        type: 'link'
      },
      { 
        label: 'Disputes', 
        route: '/disputes', 
        icon: 'fa-gavel', 
        count: count,
        type: 'link'
      },
      { 
        label: 'Payments', 
        route: '/payments', 
        icon: 'fa-credit-card',
        type: 'link'
      },
      { 
        label: 'Payouts', 
        route: '/payouts', 
        icon: 'fa-money-bill-wave',
        type: 'link'
      },
      { 
        label: 'Analytics', 
        route: '/analytics', 
        icon: 'fa-chart-line',
        type: 'link'
      },
      { 
        label: 'Audit Logs', 
        route: '/audit', 
        icon: 'fa-history',
        type: 'link'
      }
    ];
  }

  // Check if any child route is active
  isChildActive(children: { route: string }[]): boolean {
    const currentUrl = this.router.url;
    return children.some(child => currentUrl.includes(child.route));
  }

  // Get the active child label for display in collapsed state
  getActiveChildLabel(): string {
    const currentUrl = this.router.url;
    const usersDropdown = this.navItems.find(item => item.type === 'dropdown');
    if (usersDropdown && usersDropdown.type === 'dropdown') {
      const activeChild = usersDropdown.children.find(child => currentUrl.includes(child.route));
      if (activeChild) {
        return activeChild.label;
      }
    }
    return '';
  }

  get currentUser() {
    return this.authService.user();
  }

  get userDisplayName(): string {
    const user = this.currentUser;
    return user?.email?.split('@')[0] || user?.phone || 'Admin';
  }

  get userEmail(): string {
    return this.currentUser?.email || '';
  }

  logout(): void {
    this.authService.logout();
    this.closeMobile.emit();
  }
}
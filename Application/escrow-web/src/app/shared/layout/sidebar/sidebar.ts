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
  OnInit,
  OnChanges,
  SimpleChanges
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive, Router, NavigationEnd } from '@angular/router';
import { Subscription } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { DataService } from '../../../core/services/data';

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
  children: { label: string; route: string; icon: string }[];
  count?: number;
}

type NavItem = NavLinkItem | NavDropdownItem;

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './sidebar.html',
  styleUrls: ['./sidebar.css']
})
export class SidebarComponent implements OnInit, OnChanges, AfterViewInit, OnDestroy {
  @Input() collapsed = false;
  @Input() mobileOpen = false;
  @Output() closeMobile = new EventEmitter<void>();

  @ViewChild('navList') navList!: ElementRef<HTMLElement>;
  @ViewChild('activeIndicator') activeIndicator!: ElementRef<HTMLElement>;

  private authService = inject(AuthService);
  private dataService = inject(DataService);
  public router = inject(Router);
  private ngZone = inject(NgZone);

  private routerSubscription!: Subscription;
  private disputesViewed = false;
  private dropdownHoverTimeout: any = null;
  private updateTimeout: any = null;
  private isHoveringSidebar = false;

  openDropdowns = new Set<string>();

  ngOnInit() {
    this.checkAndOpenActiveDropdown();
  }

  ngAfterViewInit() {
    setTimeout(() => this.updateIndicator(), 100);
    this.routerSubscription = this.router.events.subscribe(event => {
      if (event instanceof NavigationEnd) {
        requestAnimationFrame(() => {
          this.updateIndicator();
          this.handleNavigationDropdownState();
        });
      }
    });
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes['collapsed'] && changes['collapsed'].currentValue === true) {
      this.openDropdowns.clear();
    }

    if (this.updateTimeout) clearTimeout(this.updateTimeout);
    this.updateTimeout = setTimeout(() => this.updateIndicator(), 50);
  }

  ngOnDestroy() {
    this.routerSubscription?.unsubscribe();
    if (this.dropdownHoverTimeout) clearTimeout(this.dropdownHoverTimeout);
    if (this.updateTimeout) clearTimeout(this.updateTimeout);
  }

  @HostListener('mouseenter')
  onSidebarHover() {
    this.isHoveringSidebar = true;
  }

  @HostListener('mouseleave')
  onSidebarLeave() {
    this.isHoveringSidebar = false;
    if (this.dropdownHoverTimeout) {
      clearTimeout(this.dropdownHoverTimeout);
      this.dropdownHoverTimeout = null;
    }
  }

  @HostListener('window:resize')
  onResize() {
    this.updateIndicator();
  }

  private checkAndOpenActiveDropdown(): void {
    const currentUrl = this.router.url;
    const usersDropdown = this.navItems.find(item => item.type === 'dropdown' && item.dropdownId === 'users-dropdown');
    if (usersDropdown && usersDropdown.type === 'dropdown') {
      const isChildActive = usersDropdown.children.some(child => currentUrl.includes(child.route));
      if (isChildActive) this.openDropdowns.add(usersDropdown.dropdownId);
    }
  }

  private handleNavigationDropdownState(): void {
    const currentUrl = this.router.url;
    const usersDropdown = this.navItems.find(item => item.type === 'dropdown' && item.dropdownId === 'users-dropdown');
    if (usersDropdown && usersDropdown.type === 'dropdown') {
      const isChildActive = usersDropdown.children.some(child => currentUrl.includes(child.route));
      const isOpen = this.openDropdowns.has(usersDropdown.dropdownId);
      if (isOpen && !isChildActive) {
        this.openDropdowns.delete(usersDropdown.dropdownId);
      }
    }
  }

  private updateIndicator(): void {
    this.ngZone.runOutsideAngular(() => {
      const navListEl = this.navList?.nativeElement;
      const indicatorEl = this.activeIndicator?.nativeElement;
      if (!navListEl || !indicatorEl) return;

      let activeItem = navListEl.querySelector('.nav-item.nav-active') as HTMLElement;
      if (!activeItem) {
        activeItem = navListEl.querySelector('.nav-item.nav-active-child') as HTMLElement;
        if (activeItem) {
          const wrapper = activeItem.closest('.nav-dropdown-wrapper');
          if (wrapper) {
            const trigger = wrapper.querySelector('.nav-dropdown-trigger') as HTMLElement;
            if (trigger) activeItem = trigger;
          }
        }
      }

      if (activeItem) {
        const listRect = navListEl.getBoundingClientRect();
        const itemRect = activeItem.getBoundingClientRect();
        const top = itemRect.top - listRect.top;
        const height = itemRect.height;
        indicatorEl.style.transform = `translateY(${top}px)`;
        indicatorEl.style.height = `${height}px`;
        indicatorEl.style.display = 'block';
      } else {
        indicatorEl.style.display = 'none';
      }
    });
  }

  toggleDropdown(dropdownId: string, event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    if (this.dropdownHoverTimeout) {
      clearTimeout(this.dropdownHoverTimeout);
      this.dropdownHoverTimeout = null;
    }
    if (this.openDropdowns.has(dropdownId)) {
      this.openDropdowns.delete(dropdownId);
    } else {
      this.openDropdowns.clear();
      this.openDropdowns.add(dropdownId);
    }
  }

  openDropdownOnHover(dropdownId: string): void {
    if (!this.collapsed) {
      if (this.dropdownHoverTimeout) {
        clearTimeout(this.dropdownHoverTimeout);
        this.dropdownHoverTimeout = null;
      }
      this.openDropdowns.add(dropdownId);
    }
  }

  closeDropdownOnLeave(dropdownId: string): void {
    if (!this.collapsed) {
      if (this.dropdownHoverTimeout) clearTimeout(this.dropdownHoverTimeout);
      this.dropdownHoverTimeout = setTimeout(() => {
        this.openDropdowns.delete(dropdownId);
        this.dropdownHoverTimeout = null;
      }, 300);
    }
  }

  isDropdownOpen(dropdownId: string): boolean {
    return this.openDropdowns.has(dropdownId);
  }

  closeDropdownOnNavigate(): void {
    this.openDropdowns.clear();
    if (this.mobileOpen) this.closeMobile.emit();
  }

  get navItems(): NavItem[] {
    const activeDisputes = this.dataService.disputes().filter(d =>
      ['PENDING', 'UNDER_REVIEW', 'OPEN', 'ESCALATED'].includes(d.status)
    ).length;
    const count = this.disputesViewed ? 0 : activeDisputes;
    return [
      { label: 'Dashboard', route: '/dashboard', icon: 'fa-tachometer-alt', type: 'link' },
      {
        label: 'Users',
        icon: 'fa-users',
        type: 'dropdown',
        dropdownId: 'users-dropdown',
        children: [
          { label: 'All Users', route: '/users', icon: 'fa-users' },
          { label: 'Buyers', route: '/buyers', icon: 'fa-user' },
          { label: 'Sellers', route: '/sellers', icon: 'fa-store' },
          { label: 'Riders', route: '/riders', icon: 'fa-motorcycle' }, // ✅ Added Riders
          { label: 'Admins', route: '/admins', icon: 'fa-user-shield' }
        ]
      },
      { label: 'Escrows', route: '/transactions', icon: 'fa-exchange-alt', type: 'link' },
      { label: 'Disputes', route: '/disputes', icon: 'fa-gavel', count, type: 'link' },
      { label: 'Payments', route: '/payments', icon: 'fa-credit-card', type: 'link' },
      { label: 'Payouts', route: '/payouts', icon: 'fa-money-bill-wave', type: 'link' },
      { label: 'Analytics', route: '/analytics', icon: 'fa-chart-line', type: 'link' },
      { label: 'Audit Logs', route: '/audit', icon: 'fa-history', type: 'link' }
    ];
  }

  isChildActive(children: { route: string }[]): boolean {
    return children.some(child => this.router.url.includes(child.route));
  }

  getActiveChildLabel(): string {
    const currentUrl = this.router.url;
    const usersDropdown = this.navItems.find(item => item.type === 'dropdown' && item.dropdownId === 'users-dropdown');
    if (usersDropdown && usersDropdown.type === 'dropdown') {
      const activeChild = usersDropdown.children.find(child => currentUrl.includes(child.route));
      if (activeChild) return activeChild.label;
    }
    return '';
  }

  get currentUser() { return this.authService.user(); }
  get userDisplayName(): string {
    const user = this.currentUser;
    return user?.email?.split('@')[0] || user?.phone || 'Admin';
  }
  get userEmail(): string { return this.currentUser?.email || ''; }

  logout(): void {
    this.authService.logout();
    this.closeMobile.emit();
  }
}
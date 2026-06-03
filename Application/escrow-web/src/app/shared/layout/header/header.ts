import { Component, computed, EventEmitter, Input, Output, signal, HostListener, ElementRef, ViewChild, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { DataService } from '../../../core/services/data';
import { GlobalSearchFilter, SearchMode, SearchService } from '../../../core/services/search';
import { NotificationService } from '../../../core/services/notifications';

interface GlobalSearchResult {
  type: GlobalSearchFilter;
  title: string;
  detail: string;
  route: string;
  icon: string;
}

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './header.html',
  styleUrls: ['./header.css']
})
export class HeaderComponent implements AfterViewInit {
  @Input() pageTitle: string = 'Dashboard';
  @Output() toggleSidebar = new EventEmitter<void>();
  
  notificationsOpen = signal(false);
  globalSearchOpen = signal(false);
  searchQuery = signal('');
  selectedFilter = signal<GlobalSearchFilter>('all');
  isSearching = signal(false);
  
  @ViewChild('searchInput') searchInputRef!: ElementRef<HTMLInputElement>;

  private searchDebounceTimer: any = null;
  private searchResultsSignal = signal<GlobalSearchResult[]>([]);
  
  globalResults = computed(() => {
    return this.searchResultsSignal().slice(0, 10);
  });

  availableFilters: GlobalSearchFilter[] = ['all', 'users', 'transactions', 'disputes', 'audit'];

  constructor(
    public searchService: SearchService,
    public notificationService: NotificationService,
    private dataService: DataService,
    private router: Router
  ) {}

  ngAfterViewInit() {}

  openGlobalSearch(): void {
    this.globalSearchOpen.set(true);
    this.searchQuery.set('');
    this.searchResultsSignal.set([]);
    this.isSearching.set(false);
    setTimeout(() => {
      if (this.searchInputRef?.nativeElement) {
        this.searchInputRef.nativeElement.focus();
      }
    }, 100);
  }

  closeGlobalSearch(): void {
    this.globalSearchOpen.set(false);
    this.searchQuery.set('');
    this.searchResultsSignal.set([]);
    this.isSearching.set(false);
    this.searchService.setMode('current');
    if (this.searchDebounceTimer) {
      clearTimeout(this.searchDebounceTimer);
    }
  }

  @HostListener('document:keydown.escape', ['$event'])
  onEscape(event: Event): void {
    if (this.globalSearchOpen()) {
      this.closeGlobalSearch();
    }
    if (this.notificationsOpen()) {
      this.notificationsOpen.set(false);
    }
  }

  @HostListener('document:keydown.ctrl.k', ['$event'])
  @HostListener('document:keydown.meta.k', ['$event'])
  onCmdK(event: Event): void {
    event.preventDefault();
    this.openGlobalSearch();
  }

  onSearchInput(query: string): void {
    this.searchQuery.set(query);
    this.isSearching.set(true);
    
    if (this.searchDebounceTimer) {
      clearTimeout(this.searchDebounceTimer);
    }
    
    this.searchDebounceTimer = setTimeout(() => {
      this.performSearch();
    }, 300);
  }

  performSearch(): void {
    const query = this.searchQuery().toLowerCase().trim();
    const filter = this.selectedFilter();
    
    if (!query) {
      this.searchResultsSignal.set([]);
      this.isSearching.set(false);
      return;
    }

    const results: GlobalSearchResult[] = [];
    const include = (type: GlobalSearchFilter) => filter === 'all' || filter === type;

    setTimeout(() => {
      try {
        if (include('users')) {
          const users = this.dataService.users();
          for (const user of users) {
            const haystack = `${user.name} ${user.phone} ${user.email} ${user.role} ${user.status} ${user.kycStatus}`.toLowerCase();
            if (haystack.includes(query)) {
              results.push({
                type: 'users',
                title: user.name,
                detail: `${user.role} | ${user.status} | ${user.email}`,
                route: '/users',
                icon: 'fas fa-user-circle'
              });
            }
          }
        }

        if (include('transactions')) {
          const transactions = this.dataService.transactions();
          for (const tx of transactions) {
            const haystack = `${tx.id} ${tx.buyer} ${tx.seller} ${tx.status} ${tx.amount}`.toLowerCase();
            if (haystack.includes(query)) {
              results.push({
                type: 'transactions',
                title: tx.id,
                detail: `${tx.buyer} → ${tx.seller} | KES ${tx.amount.toLocaleString()} | ${tx.status}`,
                route: '/transactions',
                icon: 'fas fa-exchange-alt'
              });
            }
          }
        }

        if (include('disputes')) {
          const disputes = this.dataService.disputes();
          for (const dispute of disputes) {
            const haystack = `${dispute.id} ${dispute.txId} ${dispute.raisedBy} ${dispute.against} ${dispute.reason} ${dispute.status}`.toLowerCase();
            if (haystack.includes(query)) {
              results.push({
                type: 'disputes',
                title: dispute.id,
                detail: `${dispute.reason} | ${dispute.status}`,
                route: '/disputes',
                icon: 'fas fa-gavel'
              });
            }
          }
        }

        if (include('audit')) {
          const auditLogs = this.dataService.getAuditLogs();
          for (const log of auditLogs) {
            const haystack = `${log.timestamp} ${log.admin} ${log.action} ${log.target} ${log.details}`.toLowerCase();
            if (haystack.includes(query)) {
              results.push({
                type: 'audit',
                title: log.action,
                detail: `${log.target} | ${log.details}`,
                route: '/audit',
                icon: 'fas fa-history'
              });
            }
          }
        }

        this.searchResultsSignal.set(results);
      } catch (error) {
        console.error('Search error:', error);
        this.searchResultsSignal.set([]);
      } finally {
        this.isSearching.set(false);
      }
    }, 0);
  }

  openResult(result: GlobalSearchResult): void {
    this.router.navigateByUrl(result.route);
    this.closeGlobalSearch();
    this.searchService.setQuery('');
  }

  toggleNotifications(): void {
    this.notificationsOpen.set(!this.notificationsOpen());
    if (this.globalSearchOpen()) {
      this.closeGlobalSearch();
    }
  }

  formatNotificationTime(value: string): string {
    const minutes = Math.max(1, Math.floor((Date.now() - new Date(value).getTime()) / 60000));
    if (minutes < 60) return `${minutes}m ago`;
    const hours = Math.floor(minutes / 60);
    if (hours < 24) return `${hours}h ago`;
    return `${Math.floor(hours / 24)}d ago`;
  }

  getFilterChipClass(filter: GlobalSearchFilter): string {
    return this.selectedFilter() === filter ? 'filter-chip-active' : 'filter-chip';
  }

  clearSearch(): void {
    this.searchQuery.set('');
    this.searchResultsSignal.set([]);
    this.isSearching.set(false);
    if (this.searchDebounceTimer) {
      clearTimeout(this.searchDebounceTimer);
    }
  }

  setFilter(filter: string): void {
    this.selectedFilter.set(filter as GlobalSearchFilter);
    if (this.searchQuery()) {
      this.performSearch();
    }
  }
}
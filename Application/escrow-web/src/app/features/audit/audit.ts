import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DataService, AuditLog } from '../../core/services/data';
import { SearchService } from '../../core/services/search';

@Component({
  selector: 'app-audit',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './audit.html',
  styleUrls: ['./audit.css']
})
export class AuditComponent implements OnInit {
  // Expose Math for template use
  readonly Math = Math;
  
  private dataService = inject(DataService);
  private searchService = inject(SearchService);
  
  // Filters
  actionFilter = signal<string>('all');
  searchTerm = this.searchService.query;
  dateFilter = signal<string>('all');
  
  // Pagination
  currentPage = signal<number>(1);
  itemsPerPage = signal<number>(20);
  
  // Computed filtered logs
  filteredLogs = computed(() => {
    let logs: AuditLog[] = this.dataService.getFilteredAuditLogs(this.actionFilter(), this.searchTerm());
    
    // Apply date filter
    if (this.dateFilter() !== 'all') {
      const now = new Date();
      const filterDays = parseInt(this.dateFilter());
      const cutoffDate = new Date(now.setDate(now.getDate() - filterDays));
      logs = logs.filter((log: AuditLog) => new Date(log.timestamp) >= cutoffDate);
    }
    
    return logs;
  });
  
  // Paginated logs
  paginatedLogs = computed(() => {
    const start = (this.currentPage() - 1) * this.itemsPerPage();
    const end = start + this.itemsPerPage();
    return this.filteredLogs().slice(start, end);
  });
  
  // Total pages
  totalPages = computed(() => {
    return Math.ceil(this.filteredLogs().length / this.itemsPerPage());
  });
  
  // Statistics
  stats = computed(() => this.dataService.getAuditLogStats());
  
  // Available action types for filter
  actionTypes = computed(() => {
    const actions = new Set<string>();
    this.dataService.getAuditLogs().forEach((log: AuditLog) => actions.add(log.action));
    return Array.from(actions).sort();
  });
  
  ngOnInit() {
    // Initialize - logs are already in the service
  }
  
  onActionFilterChange(event: Event): void {
    const select = event.target as HTMLSelectElement;
    this.actionFilter.set(select.value);
    this.currentPage.set(1);
  }
  
  onDateFilterChange(event: Event): void {
    const select = event.target as HTMLSelectElement;
    this.dateFilter.set(select.value);
    this.currentPage.set(1);
  }
  
  onSearch(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.searchService.setQuery(input.value);
    this.currentPage.set(1);
  }
  
  clearSearch(): void {
    this.searchService.clear();
    this.currentPage.set(1);
  }
  
  clearAllFilters(): void {
    this.actionFilter.set('all');
    this.dateFilter.set('all');
    this.searchService.clear();
    this.currentPage.set(1);
  }
  
  previousPage(): void {
    if (this.currentPage() > 1) {
      this.currentPage.set(this.currentPage() - 1);
    }
  }
  
  nextPage(): void {
    if (this.currentPage() < this.totalPages()) {
      this.currentPage.set(this.currentPage() + 1);
    }
  }
  
  goToPage(page: number): void {
    if (page >= 1 && page <= this.totalPages()) {
      this.currentPage.set(page);
    }
  }
  
  exportToCSV(): void {
    const logs = this.filteredLogs();
    
    if (logs.length === 0) {
      alert('No logs to export');
      return;
    }
    
    const headers = ['Timestamp', 'Admin', 'Action', 'Target', 'Details'];
    const csvRows = [headers.join(',')];
    
    for (const log of logs) {
      const values = [
        `"${log.timestamp}"`,
        `"${log.admin}"`,
        `"${log.action}"`,
        `"${log.target}"`,
        `"${log.details.replace(/"/g, '""')}"`
      ];
      csvRows.push(values.join(','));
    }
    
    const blob = new Blob([csvRows.join('\n')], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `escrowx-audit-logs-${new Date().toISOString().split('T')[0]}.csv`;
    a.click();
    window.URL.revokeObjectURL(url);
  }
  
  clearAllLogs(): void {
    if (confirm('⚠️ PERMANENT ACTION: Clear all audit logs?\n\nThis action cannot be undone. All audit history will be permanently deleted.')) {
      this.dataService.clearAuditLogs();
      this.clearAllFilters();
    }
  }
  
  getActionBadgeClass(action: string): string {
    const actionMap: Record<string, string> = {
      'FORCE_RELEASE': 'bg-green-100 text-green-800',
      'FORCE_REFUND': 'bg-red-100 text-red-800',
      'CANCEL_TRANSACTION': 'bg-gray-100 text-gray-800',
      'RESOLVE_DISPUTE': 'bg-purple-100 text-purple-800',
      'SUSPEND_USER': 'bg-yellow-100 text-yellow-800',
      'ACTIVATE_USER': 'bg-emerald-100 text-emerald-800',
      'APPROVE_KYC': 'bg-blue-100 text-blue-800',
      'REJECT_KYC': 'bg-orange-100 text-orange-800',
      'BLACKLIST_USER': 'bg-red-100 text-red-800',
      'REMOVE_BLACKLIST': 'bg-gray-100 text-gray-800',
      'UPDATE_DISPUTE_STATUS': 'bg-indigo-100 text-indigo-800',
      'ADD_ADMIN_NOTE': 'bg-cyan-100 text-cyan-800',
      'CLEAR_AUDIT_LOGS': 'bg-red-100 text-red-800'
    };
    return actionMap[action] || 'bg-gray-100 text-gray-800';
  }
  
  getActionIcon(action: string): string {
    const iconMap: Record<string, string> = {
      'FORCE_RELEASE': 'fa-hand-holding-usd',
      'FORCE_REFUND': 'fa-undo-alt',
      'CANCEL_TRANSACTION': 'fa-times-circle',
      'RESOLVE_DISPUTE': 'fa-gavel',
      'SUSPEND_USER': 'fa-ban',
      'ACTIVATE_USER': 'fa-check-circle',
      'APPROVE_KYC': 'fa-id-card',
      'REJECT_KYC': 'fa-times-circle',
      'BLACKLIST_USER': 'fa-skull',
      'REMOVE_BLACKLIST': 'fa-undo',
      'UPDATE_DISPUTE_STATUS': 'fa-exchange-alt',
      'ADD_ADMIN_NOTE': 'fa-sticky-note',
      'CLEAR_AUDIT_LOGS': 'fa-trash-alt'
    };
    return iconMap[action] || 'fa-history';
  }
  
  formatDate(timestamp: string): string {
    const date = new Date(timestamp);
    return date.toLocaleString('en-KE', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    });
  }
  
  getPagesArray(): number[] {
    const total = this.totalPages();
    const current = this.currentPage();
    const pages: number[] = [];
    
    // Show max 5 pages at a time
    let start = Math.max(1, current - 2);
    let end = Math.min(total, start + 4);
    
    if (end - start < 4) {
      start = Math.max(1, end - 4);
    }
    
    for (let i = start; i <= end; i++) {
      pages.push(i);
    }
    
    return pages;
  }
}

// src/app/features/transactions/transactions.ts
import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DataService } from '../../core/services/data';
import { SearchService } from '../../core/services/search';
import { ApiService } from '../../core/services/api.service';
import { Transaction } from '../../core/models/transaction';
// 👇 Import the skeleton directive
import { NgxSmkSkeletonDirective } from 'ngxsmk-skeleton-loader';

@Component({
  selector: 'app-transactions',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    // 👇 Add the directive to imports
    NgxSmkSkeletonDirective
  ],
  templateUrl: './transactions.html',
  styleUrls: ['./transactions.css']
})
export class TransactionsComponent implements OnInit {
  private dataService = inject(DataService);
  private searchService = inject(SearchService);
  private apiService = inject(ApiService);

  // 👇 Inject global loading state
  isLoading = this.dataService.isLoading;

  // Filters
  statusFilter = signal<string>('all');
  searchTerm = this.searchService.query;

  // Detail modal
  selectedTransaction = signal<Transaction | null>(null);
  showDetailModal = signal<boolean>(false);

  // Info modal (History & Ledger)
  showInfoModal = signal<boolean>(false);
  selectedTransactionForInfo = signal<Transaction | null>(null);
  activeTab = signal<'history' | 'ledger'>('history');
  historyData = signal<any[]>([]);
  ledgerData = signal<any[]>([]);
  loadingHistory = signal<boolean>(false);
  loadingLedger = signal<boolean>(false);
  historyFetched = signal<boolean>(false);
  ledgerFetched = signal<boolean>(false);

  // Computed filtered transactions
  filteredTransactions = computed(() => {
    return this.dataService.getFilteredTransactions(
      this.statusFilter(),
      this.searchTerm()
    );
  });

  stats = computed(() => this.dataService.getTransactionStats());

  ngOnInit() { }

  // ------------------------------------------------------------------
  // Filter handlers
  // ------------------------------------------------------------------

  onStatusFilterChange(event: Event): void {
    const select = event.target as HTMLSelectElement;
    this.statusFilter.set(select.value);
  }

  onSearch(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.searchService.setQuery(input.value);
  }

  clearSearch(): void {
    this.searchService.clear();
  }

  // ------------------------------------------------------------------
  // Admin Actions
  // ------------------------------------------------------------------

  forceRelease(transaction: Transaction): void {
    if (confirm(`⚠️ FORCE RELEASE FUNDS\n\nTransaction: ${transaction.id}\nAmount: KES ${transaction.amount.toLocaleString()}\nSeller: ${transaction.seller}\n\nContinue?`)) {
      this.dataService.forceReleaseFunds(transaction.id);
    }
  }

  forceRefund(transaction: Transaction): void {
    if (confirm(`⚠️ FORCE REFUND\n\nTransaction: ${transaction.id}\nAmount: KES ${transaction.amount.toLocaleString()}\nBuyer: ${transaction.buyer}\n\nContinue?`)) {
      this.dataService.forceRefund(transaction.id);
    }
  }

  cancelTransaction(transaction: Transaction): void {
    if (confirm(`❌ CANCEL TRANSACTION\n\nTransaction: ${transaction.id}\nAmount: KES ${transaction.amount.toLocaleString()}\n\nContinue?`)) {
      this.dataService.cancelTransaction(transaction.id);
    }
  }

  clearAllFilters(): void {
    this.searchService.clear();   // clears search term
    this.statusFilter.set('all'); // resets status filter
  }
  // ------------------------------------------------------------------
  // Detail Modal
  // ------------------------------------------------------------------

  viewTransactionDetails(transaction: Transaction): void {
    this.selectedTransaction.set(transaction);
    this.showDetailModal.set(true);
  }

  closeDetailModal(): void {
    this.showDetailModal.set(false);
    this.selectedTransaction.set(null);
  }

  // ------------------------------------------------------------------
  // Info Modal (History & Ledger)
  // ------------------------------------------------------------------

  viewHistory(transaction: Transaction): void {
    this.selectedTransactionForInfo.set(transaction);
    this.activeTab.set('history');
    this.showInfoModal.set(true);
    this.historyFetched.set(false);
    this.ledgerFetched.set(false);
    this.fetchHistory(transaction.id);
  }

  viewLedger(transaction: Transaction): void {
    this.selectedTransactionForInfo.set(transaction);
    this.activeTab.set('ledger');
    this.showInfoModal.set(true);
    this.historyFetched.set(false);
    this.ledgerFetched.set(false);
    this.fetchLedger(transaction.id);
  }

  onTabChange(tab: 'history' | 'ledger'): void {
    const transaction = this.selectedTransactionForInfo();
    if (!transaction) return;

    this.activeTab.set(tab);

    if (tab === 'history' && !this.historyFetched() && !this.loadingHistory()) {
      this.fetchHistory(transaction.id);
    } else if (tab === 'ledger' && !this.ledgerFetched() && !this.loadingLedger()) {
      this.fetchLedger(transaction.id);
    }
  }

  private fetchHistory(transactionId: string): void {
    if (this.loadingHistory()) return;
    this.loadingHistory.set(true);
    this.apiService.getTransactionStatusHistory(transactionId).subscribe({
      next: (response: any) => {
        let history = [];
        if (Array.isArray(response)) {
          history = response;
        } else if (response && Array.isArray(response.data)) {
          history = response.data;
        } else if (response && Array.isArray(response.content)) {
          history = response.content;
        }
        this.historyData.set(history);
        this.historyFetched.set(true);
        this.loadingHistory.set(false);
      },
      error: (err: any) => {
        console.error('Failed to load history:', err);
        this.historyData.set([]);
        this.loadingHistory.set(false);
      }
    });
  }

  private fetchLedger(transactionId: string): void {
    if (this.loadingLedger()) return;
    this.loadingLedger.set(true);
    this.apiService.getLedgerEntries(transactionId).subscribe({
      next: (response: any) => {
        let raw = [];
        if (Array.isArray(response)) {
          raw = response;
        } else if (response && Array.isArray(response.data)) {
          raw = response.data;
        } else if (response && Array.isArray(response.content)) {
          raw = response.content;
        }

        const mapped = raw.map((entry: any) => ({
          ...entry,
          entryType: entry.entryType || entry.entry_type,
          referenceId: entry.referenceId || entry.reference_id,
          referenceType: entry.referenceType || entry.reference_type,
          createdAt: entry.createdAt || entry.created_at,
          updatedAt: entry.updatedAt || entry.updated_at
        }));

        this.ledgerData.set(mapped);
        this.ledgerFetched.set(true);
        this.loadingLedger.set(false);
      },
      error: (err: any) => {
        console.error('Failed to load ledger:', err);
        this.ledgerData.set([]);
        this.loadingLedger.set(false);
      }
    });
  }

  closeInfoModal(): void {
    this.showInfoModal.set(false);
    this.selectedTransactionForInfo.set(null);
    this.historyData.set([]);
    this.ledgerData.set([]);
    this.historyFetched.set(false);
    this.ledgerFetched.set(false);
    this.loadingHistory.set(false);
    this.loadingLedger.set(false);
  }

  // ------------------------------------------------------------------
  // Helpers
  // ------------------------------------------------------------------

  getStatusBadgeClass(status: string): string {
    switch (status) {
      case 'FUNDS_HELD': return 'bg-yellow-100 text-yellow-800';
      case 'COMPLETED': return 'bg-green-100 text-green-800';
      case 'DISPUTED': return 'bg-red-100 text-red-800';
      case 'REFUNDED': return 'bg-gray-100 text-gray-800';
      case 'CANCELLED': return 'bg-gray-100 text-gray-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  }

  getStatusIcon(status: string): string {
    switch (status) {
      case 'FUNDS_HELD': return 'fa-lock';
      case 'COMPLETED': return 'fa-check-circle';
      case 'DISPUTED': return 'fa-gavel';
      case 'REFUNDED': return 'fa-undo-alt';
      case 'CANCELLED': return 'fa-times-circle';
      default: return 'fa-question-circle';
    }
  }

  formatDate(dateString: string): string {
    return new Date(dateString).toLocaleDateString('en-KE');
  }

  formatDateTime(dateString: string): string {
    return new Date(dateString).toLocaleString('en-KE');
  }
}
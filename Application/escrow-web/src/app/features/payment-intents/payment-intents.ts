// src/app/features/payment-intents/payment-intents.ts

import { Component, inject, signal, OnInit, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/services/api.service';
import { SearchService } from '../../core/services/search';
import { CacheService } from '../../core/services/cache.service';
import { NgxSmkSkeletonDirective } from 'ngxsmk-skeleton-loader';

export interface PaymentIntent {
  id: string;
  transactionId: string;
  buyerId: string;
  sellerId: string;
  amount: number;
  status: 'INITIATED' | 'PENDING' | 'PAID' | 'FAILED' | 'CANCELLED';
  mpesaReceiptNumber: string;
  phoneNumber: string;
  checkoutRequestId: string;
  merchantRequestId: string;
  providerResponseCode: string;
  providerResponseDescription: string;
  createdAt: string;
  updatedAt: string;
  paidAt: string | null;
}

interface StatusHistoryEntry {
  id: string;
  from_status: string | null;
  to_status: string;
  changed_by: string | null;
  reason: string | null;
  created_at: string;
  createdAt?: string;
}

const CACHE_KEY_PAYMENT_INTENTS = 'admin_payment_intents';

@Component({
  selector: 'app-payment-intents',
  standalone: true,
  imports: [CommonModule, FormsModule, NgxSmkSkeletonDirective],
  templateUrl: './payment-intents.html',
  styleUrls: ['./payment-intents.css']
})
export class PaymentIntentsComponent implements OnInit {
  private apiService = inject(ApiService);
  public searchService = inject(SearchService);
  private cacheService = inject(CacheService);

  // Loading state
  isLoading = signal<boolean>(true);

  // Pagination
  currentPage = signal<number>(0);
  pageSize = signal<number>(20);
  totalElements = signal<number>(0);
  totalPages = signal<number>(0);

  // Status filter
  statusFilter = signal<string>('all');

  // Data
  private intentsSignal = signal<PaymentIntent[]>([]);

  // Computed filtered intents
  filteredIntents = computed(() => {
    const term = this.searchService.query().toLowerCase().trim();
    const status = this.statusFilter();
    let list = this.intentsSignal();

    // Apply status filter
    if (status !== 'all') {
      list = list.filter(item => item.status === status);
    }

    // Apply search
    if (term) {
      list = list.filter(item =>
        item.id.toLowerCase().includes(term) ||
        item.transactionId.toLowerCase().includes(term) ||
        item.buyerId.toLowerCase().includes(term) ||
        item.sellerId.toLowerCase().includes(term) ||
        item.status.toLowerCase().includes(term) ||
        (item.mpesaReceiptNumber && item.mpesaReceiptNumber.toLowerCase().includes(term)) ||
        (item.phoneNumber && item.phoneNumber.includes(term))
      );
    }

    return list;
  });

  searchTerm = this.searchService.query;

  // Stats
  stats = computed(() => {
    const list = this.intentsSignal();
    const total = list.length;
    const initiated = list.filter(i => i.status === 'INITIATED').length;
    const pending = list.filter(i => i.status === 'PENDING').length;
    const paid = list.filter(i => i.status === 'PAID').length;
    const failed = list.filter(i => i.status === 'FAILED').length;
    const cancelled = list.filter(i => i.status === 'CANCELLED').length;
    const totalAmount = list.reduce((sum, i) => sum + i.amount, 0);

    return { total, initiated, pending, paid, failed, cancelled, totalAmount };
  });

  // Modal state
  selectedIntent = signal<PaymentIntent | null>(null);
  showModal = signal(false);
  statusHistory = signal<StatusHistoryEntry[]>([]);
  isLoadingHistory = signal(false);

  ngOnInit() {
    this.loadPaymentIntents();
  }

  /**
   * Load payment intents – first check cache, then API if needed.
   */
  loadPaymentIntents(forceRefresh = false): void {
    if (forceRefresh) {
      console.log('🔄 Force refreshing payment intents, clearing cache...');
      this.cacheService.clear(CACHE_KEY_PAYMENT_INTENTS);
    }

    // 1️⃣ Try cache
    const cached = this.cacheService.get<PaymentIntent[]>(CACHE_KEY_PAYMENT_INTENTS);
    if (cached && !forceRefresh) {
      console.log('✅ Payment intents loaded from cache:', cached.length, 'intents');
      this.intentsSignal.set(cached);
      this.isLoading.set(false);
      return;
    }

    console.log('⏳ No cache found, fetching payment intents from API...');

    // 2️⃣ No cache – fetch from API
    this.isLoading.set(true);
    this.apiService.getPaymentIntents({
      page: this.currentPage(),
      limit: this.pageSize()
    }).subscribe({
      next: (response) => {
        // Handle both ApiResponse wrapper and direct array
        let data: any[] = [];

        if (response && Array.isArray(response)) {
          data = response;
        } else if (response && response.data && Array.isArray(response.data)) {
          data = response.data;
        } else if (response && (response as any).content && Array.isArray((response as any).content)) {
          // support responses that may return a `content` field (avoid TS error by using any)
          data = (response as any).content;
        }

        // Map to PaymentIntent format
        const mapped: PaymentIntent[] = data.map((item: any) => ({
          id: item.id || '',
          transactionId: item.transactionId || item.transaction_id || '',
          buyerId: item.buyerId || item.buyer_id || '',
          sellerId: item.sellerId || item.seller_id || '',
          amount: Number(item.amount) || 0,
          status: item.status || 'INITIATED',
          mpesaReceiptNumber: item.mpesaReceiptNumber || item.mpesa_receipt_number || '',
          phoneNumber: item.phoneNumber || item.phone_number || '',
          checkoutRequestId: item.checkoutRequestId || item.checkout_request_id || '',
          merchantRequestId: item.merchantRequestId || item.merchant_request_id || '',
          providerResponseCode: item.providerResponseCode || item.provider_response_code || '',
          providerResponseDescription: item.providerResponseDescription || item.provider_response_description || '',
          createdAt: item.createdAt || item.created_at || new Date().toISOString(),
          updatedAt: item.updatedAt || item.updated_at || new Date().toISOString(),
          paidAt: item.paidAt || item.paid_at || null
        }));

        console.log('✅ Payment intents fetched from API:', mapped.length, 'intents');
        this.intentsSignal.set(mapped);
        this.cacheService.set(CACHE_KEY_PAYMENT_INTENTS, mapped);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('❌ Failed to load payment intents:', err);
        this.isLoading.set(false);
        // Show error state
        this.intentsSignal.set([]);
      }
    });
  }

  /**
   * Refresh payment intents – clear cache and reload.
   */
  refreshIntents(): void {
    this.loadPaymentIntents(true);
  }

  /**
   * Open detail modal and load status history.
   */
  viewIntentDetails(intent: PaymentIntent): void {
    this.selectedIntent.set(intent);
    this.showModal.set(true);
    this.loadStatusHistory(intent.transactionId);
  }

  /**
   * Fetch transaction status history.
   */
  private loadStatusHistory(transactionId: string): void {
    if (!transactionId) {
      this.statusHistory.set([]);
      this.isLoadingHistory.set(false);
      return;
    }

    this.isLoadingHistory.set(true);
    this.apiService.getTransactionStatusHistory(transactionId).subscribe({
      next: (response) => {
        let historyResponse: any[] = [];

        if (response && Array.isArray(response)) {
          historyResponse = response;
        } else if (response && response.data && Array.isArray(response.data)) {
          historyResponse = response.data;
        } else if (response && (response as any).content && Array.isArray((response as any).content)) {
          historyResponse = (response as any).content;
        }

        const history: StatusHistoryEntry[] = historyResponse.map((entry: any) => ({
          id: entry.id || entry.historyId || '',
          from_status: entry.from_status ?? entry.fromStatus ?? null,
          to_status: entry.to_status || entry.toStatus || '',
          changed_by: entry.changed_by ?? entry.changedBy ?? null,
          reason: entry.reason ?? null,
          created_at: entry.created_at || entry.createdAt || new Date().toISOString(),
          createdAt: entry.createdAt || entry.created_at || new Date().toISOString()
        }));

        this.statusHistory.set(history);
        this.isLoadingHistory.set(false);
      },
      error: (err) => {
        console.error('Failed to load status history:', err);
        this.statusHistory.set([]);
        this.isLoadingHistory.set(false);
      }
    });
  }

  /**
   * Close the detail modal.
   */
  closeModal(): void {
    this.showModal.set(false);
    this.selectedIntent.set(null);
    this.statusHistory.set([]);
    this.isLoadingHistory.set(false);
  }

  /**
   * Clear search input.
   */
  clearSearch(): void {
    this.searchService.clear();
  }

  /**
   * Handle status filter change.
   */
  onStatusFilterChange(event: Event): void {
    const select = event.target as HTMLSelectElement;
    this.statusFilter.set(select.value);
  }

  /**
   * Format date string for display.
   */
  formatDate(dateString: string | undefined): string {
    if (!dateString) return 'N/A';
    try {
      return new Date(dateString).toLocaleString('en-KE', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch {
      return dateString;
    }
  }

  /**
   * Get status badge class.
   */
  getStatusBadge(status: string): string {
    switch(status) {
      case 'INITIATED': return 'bg-yellow-100 text-yellow-800';
      case 'PENDING': return 'bg-blue-100 text-blue-800';
      case 'PAID': return 'bg-green-100 text-green-800';
      case 'FAILED': return 'bg-red-100 text-red-800';
      case 'CANCELLED': return 'bg-gray-100 text-gray-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  }

  /**
   * Get status icon class.
   */
  getStatusIcon(status: string): string {
    switch(status) {
      case 'INITIATED': return 'fa-hourglass-start';
      case 'PENDING': return 'fa-spinner fa-spin';
      case 'PAID': return 'fa-check-circle';
      case 'FAILED': return 'fa-exclamation-circle';
      case 'CANCELLED': return 'fa-ban';
      default: return 'fa-question-circle';
    }
  }

  /**
   * Safely slice a string (e.g., for IDs).
   */
  safeSlice(value: string | undefined, length: number = 8): string {
    return value ? value.slice(0, length) : '';
  }

  /**
   * Check if status is active (not terminal).
   */
  isActiveStatus(status: string): boolean {
    return status === 'INITIATED' || status === 'PENDING';
  }

  /**
   * Get status history step label.
   */
  getHistoryStepLabel(fromStatus: string | null, toStatus: string): string {
    const labels: Record<string, string> = {
      'INITIATED': 'Initiated',
      'PENDING': 'Processing',
      'PAID': 'Paid',
      'FAILED': 'Failed',
      'CANCELLED': 'Cancelled'
    };
    const from = fromStatus ? labels[fromStatus] || fromStatus : 'Start';
    const to = labels[toStatus] || toStatus;
    return `${from} → ${to}`;
  }
}
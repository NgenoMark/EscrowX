// src/app/features/payment-intents/payment-intents.ts
import { Component, inject, signal, OnInit, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/services/api.service';
import { SearchService } from '../../core/services/search';
import { PaymentIntent } from '../../core/models/payment-intent';
import { CacheService } from '../../core/services/cache.service';
import { NgxSmkSkeletonDirective } from 'ngxsmk-skeleton-loader';

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

  // 👇 Local loading signal (writable)
  isLoading = signal<boolean>(true);

  private intentsSignal = signal<PaymentIntent[]>([]);

  filteredIntents = computed(() => {
    const term = this.searchService.query().toLowerCase().trim();
    const list = this.intentsSignal();
    if (!term) return list;
    return list.filter(item =>
      item.id.toLowerCase().includes(term) ||
      item.buyerId.toLowerCase().includes(term) ||
      item.sellerId.toLowerCase().includes(term) ||
      item.status.toLowerCase().includes(term) ||
      (item.mpesaReceiptNumber && item.mpesaReceiptNumber.toLowerCase().includes(term))
    );
  });

  searchTerm = this.searchService.query;

  // Modal state
  selectedIntent = signal<PaymentIntent | null>(null);
  showModal = signal(false);
  statusHistory = signal<any[]>([]);
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
    if (cached) {
      console.log('✅ Payment intents loaded from cache:', cached.length, 'intents');
      this.intentsSignal.set(cached);
      this.isLoading.set(false);
      return;
    }

    console.log('⏳ No cache found, fetching payment intents from API...');

    // 2️⃣ No cache – fetch from API
    this.isLoading.set(true);
    this.apiService.getTransactions({ size: 100 }).subscribe({
      next: (response) => {
        const mapped: PaymentIntent[] = response.content.map((item: any) => ({
          id: item.id,
          transactionId: item.id,
          buyerId: item.buyerId || '',
          sellerId: item.sellerId || '',
          amount: item.amount,
          status: item.status,
          mpesaReceiptNumber: item.mpesaReceiptNumber || item.reference || '',
          createdAt: item.createdAt || new Date().toISOString(),
          provider: item.provider,
          providerRef: item.providerRef,
          currency: item.currency,
          paymentMethod: item.paymentMethod,
          phoneNumber: item.phoneNumber,
          checkoutRequestId: item.checkoutRequestId,
          merchantRequestId: item.merchantRequestId,
          providerResponseCode: item.providerResponseCode,
          providerResponseDescription: item.providerResponseDescription,
          paidAt: item.paidAt || null,
          updatedAt: item.updatedAt
        }));
        console.log('✅ Payment intents fetched from API:', mapped.length, 'intents');
        this.intentsSignal.set(mapped);
        this.cacheService.set(CACHE_KEY_PAYMENT_INTENTS, mapped);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('❌ Failed to load payment intents:', err);
        this.isLoading.set(false);
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
    this.isLoadingHistory.set(true);
    this.apiService.getTransactionStatusHistory(transactionId).subscribe({
      next: (response) => {
        this.statusHistory.set(response.data || []);
        this.isLoadingHistory.set(false);
      },
      error: (err) => {
        console.error('Failed to load status history:', err);
        this.statusHistory.set([]);
        this.isLoadingHistory.set(false);
      }
    });
  }

  closeModal(): void {
    this.showModal.set(false);
    this.selectedIntent.set(null);
    this.statusHistory.set([]);
  }

  clearSearch(): void {
    this.searchService.clear();
  }

  formatDate(dateString: string | undefined): string {
    if (!dateString) return 'N/A';
    return new Date(dateString).toLocaleString();
  }

  getStatusBadge(status: string): string {
    switch(status) {
      case 'FUNDS_HELD': return 'bg-yellow-100 text-yellow-800';
      case 'COMPLETED': return 'bg-green-100 text-green-800';
      case 'DISPUTED': return 'bg-red-100 text-red-800';
      case 'REFUNDED': return 'bg-gray-100 text-gray-800';
      case 'CANCELLED': return 'bg-gray-100 text-gray-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  }

  safeSlice(value: string | undefined, length: number = 8): string {
    return value ? value.slice(0, length) : '';
  }
}
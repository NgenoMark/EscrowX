// src/app/features/payouts/payouts.ts
import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/services/api.service';
import { SearchService } from '../../core/services/search';
import { CacheService } from '../../core/services/cache.service';
import { NgxSmkSkeletonDirective } from 'ngxsmk-skeleton-loader';

const CACHE_KEY_PAYOUTS = 'admin_payouts';

@Component({
  selector: 'app-payouts',
  standalone: true,
  imports: [CommonModule, FormsModule, NgxSmkSkeletonDirective],
  templateUrl: './payouts.html',
  styleUrls: ['./payouts.css']
})
export class PayoutsComponent implements OnInit {
  private apiService = inject(ApiService);
  private searchService = inject(SearchService);
  private cacheService = inject(CacheService);

  // 👇 Local loading signal (writable)
  isLoading = signal<boolean>(true);

  statusFilter = signal<string>('all');
  searchTerm = this.searchService.query;
  payoutsSignal = signal<any[]>([]);

  filteredPayouts = computed(() => {
    let list = this.payoutsSignal();
    if (this.statusFilter() !== 'all') {
      list = list.filter(p => p.status === this.statusFilter());
    }
    const term = this.searchTerm().toLowerCase();
    if (term) {
      list = list.filter(p =>
        p.id?.toLowerCase().includes(term) ||
        p.transactionId?.toLowerCase().includes(term) ||
        p.conversationId?.toLowerCase().includes(term) ||
        p.resultDescription?.toLowerCase().includes(term)
      );
    }
    return list;
  });

  stats = computed(() => {
    const list = this.payoutsSignal();
    const total = list.length;
    const paid = list.filter(p => p.status === 'PAID').length;
    const failed = list.filter(p => p.status === 'FAILED').length;
    const processing = list.filter(p => p.status === 'PROCESSING' || p.status === 'INITIATED').length;
    const totalAmount = list.reduce((sum, p) => sum + Number(p.amount || 0), 0);
    return { total, paid, failed, processing, totalAmount };
  });

  ngOnInit() {
    this.loadData();
  }

  /**
   * Load payouts – first check cache, then API if needed.
   */
  loadData(forceRefresh = false): void {
    if (forceRefresh) {
      console.log('🔄 Force refreshing payouts, clearing cache...');
      this.cacheService.clear(CACHE_KEY_PAYOUTS);
    }

    // 1️⃣ Try cache
    const cached = this.cacheService.get<any[]>(CACHE_KEY_PAYOUTS);
    if (cached) {
      console.log('✅ Payouts loaded from cache:', cached.length, 'payouts');
      this.payoutsSignal.set(cached);
      this.isLoading.set(false);
      return;
    }

    console.log('⏳ No cache found, fetching payouts from API...');

    // 2️⃣ No cache – fetch from API
    this.isLoading.set(true);
    this.apiService.getPayouts({ limit: 500 }).subscribe({
      next: (res) => {
        const rawData = (res as any).data || res;
        const data = Array.isArray(rawData) ? rawData : [rawData];
        console.log('✅ Payouts fetched from API:', data.length, 'payouts');
        this.payoutsSignal.set(data || []);
        this.cacheService.set(CACHE_KEY_PAYOUTS, data || []);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('❌ Failed to load payouts:', err);
        this.payoutsSignal.set([]);
        this.isLoading.set(false);
      }
    });
  }

  /**
   * Refresh payouts – clear cache and reload.
   */
  refreshPayouts(): void {
    this.loadData(true);
  }

  onStatusFilterChange(e: Event) {
    this.statusFilter.set((e.target as HTMLSelectElement).value);
  }

  onSearch(e: Event) {
    this.searchService.setQuery((e.target as HTMLInputElement).value);
  }

  getStatusBadge(status: string): string {
    const map: Record<string, string> = {
      'INITIATED': 'bg-yellow-100 text-yellow-800',
      'PROCESSING': 'bg-blue-100 text-blue-800',
      'PAID': 'bg-green-100 text-green-800',
      'FAILED': 'bg-red-100 text-red-800',
      'CANCELLED': 'bg-gray-100 text-gray-800'
    };
    return map[status] || 'bg-gray-100 text-gray-800';
  }

  formatDate(dateString: string): string {
    if (!dateString) return '-';
    return new Date(dateString).toLocaleString('en-KE', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }
}
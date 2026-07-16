// src/app/features/transactions/transaction-details/transaction-details.ts

import { Component, inject, signal, OnInit, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { DataService } from '../../core/services/data';
import { ApiService } from '../../core/services/api.service';
import { NotificationService } from '../../core/services/notifications';
import { Transaction } from '../../core/models/transaction';
import { DeliveryAssignmentStatus } from '../../core/models/rider';

// Extend Transaction with additional delivery fields
interface TransactionWithDelivery extends Transaction {
  assignmentStatus?: DeliveryAssignmentStatus;
  assignmentId?: string;
  pickupAddress?: string;
  dropoffAddress?: string;
  pickedUpAt?: string;
  deliveredAt?: string;
}

@Component({
  selector: 'app-transaction-details',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="p-6 max-w-4xl mx-auto">
      <!-- Header -->
      <div class="flex items-center gap-4 mb-6">
        <a routerLink="/rider-assignments" class="text-indigo-600 hover:text-indigo-800 transition inline-flex items-center gap-1">
          <i class="fas fa-arrow-left mr-1"></i> Back to Assignments
        </a>
        <h2 class="text-2xl font-bold text-gray-800">Transaction Details</h2>
      </div>

      <!-- Loading State -->
      @if (loading()) {
        <div class="flex flex-col items-center justify-center py-16">
          <i class="fas fa-spinner fa-spin text-4xl text-indigo-600 mb-4"></i>
          <p class="text-gray-500">Loading transaction details...</p>
        </div>
      }

      <!-- Transaction Details -->
      @if (transaction(); as tx) {
        <div class="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
          <!-- Header with Status -->
          <div class="p-6 border-b border-gray-100 bg-gradient-to-r from-gray-50 to-white">
            <div class="flex flex-wrap items-center justify-between gap-4">
              <div>
                <p class="text-xs text-gray-500 font-semibold uppercase tracking-wider">Transaction</p>
                <p class="text-lg font-bold text-gray-800 font-mono">{{ tx.id }}</p>
                @if (tx.reference) {
                  <p class="text-sm text-gray-500">Ref: {{ tx.reference }}</p>
                }
              </div>
              <div class="flex flex-wrap items-center gap-3">
                <span class="inline-flex px-3 py-1 text-sm font-semibold rounded-full" [class]="getStatusBadge(tx.status)">
                  <i class="fas" [class]="getStatusIcon(tx.status)"></i>
                  {{ tx.status }}
                </span>
                @if (tx.riderId) {
                  <span class="inline-flex px-3 py-1 text-sm font-semibold rounded-full bg-purple-100 text-purple-800">
                    <i class="fas fa-motorcycle mr-1"></i> Assigned
                  </span>
                }
              </div>
            </div>
          </div>

          <!-- Details Grid -->
          <div class="p-6">
            <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
              <!-- Left Column -->
              <div class="space-y-4">
                <div class="bg-gray-50 rounded-lg p-4">
                  <p class="text-xs text-gray-500 font-semibold uppercase tracking-wider">Amount</p>
                  <p class="text-2xl font-bold text-indigo-600">KES {{ tx.amount | number:'1.2-2' }}</p>
                  @if (tx.initialDepositAmount && tx.initialDepositAmount !== tx.amount) {
                    <p class="text-sm text-gray-500">Deposit: KES {{ tx.initialDepositAmount | number:'1.2-2' }}</p>
                  }
                  @if (tx.currency) {
                    <p class="text-xs text-gray-400">Currency: {{ tx.currency }}</p>
                  }
                </div>

                <div class="bg-gray-50 rounded-lg p-4">
                  <p class="text-xs text-gray-500 font-semibold uppercase tracking-wider">Created</p>
                  <p class="font-medium text-gray-800">{{ tx.createdAt | date:'medium' }}</p>
                  @if (tx.updatedAt && tx.updatedAt !== tx.createdAt) {
                    <p class="text-xs text-gray-400">Updated: {{ tx.updatedAt | date:'medium' }}</p>
                  }
                  @if (tx.completedAt) {
                    <p class="text-xs text-green-600">Completed: {{ tx.completedAt | date:'medium' }}</p>
                  }
                </div>

                @if (tx.description || tx.productDescription) {
                  <div class="bg-gray-50 rounded-lg p-4">
                    <p class="text-xs text-gray-500 font-semibold uppercase tracking-wider">Description</p>
                    <p class="text-gray-700">{{ tx.description || tx.productDescription }}</p>
                  </div>
                }
              </div>

              <!-- Right Column -->
              <div class="space-y-4">
                <div class="bg-gray-50 rounded-lg p-4">
                  <p class="text-xs text-gray-500 font-semibold uppercase tracking-wider">Parties</p>
                  <div class="mt-2 space-y-2">
                    <div>
                      <p class="text-xs text-gray-400">Buyer</p>
                      <p class="font-medium text-gray-800">{{ tx.buyer }}</p>
                      @if (tx.buyerId) {
                        <p class="text-xs text-gray-400 font-mono">{{ tx.buyerId }}</p>
                      }
                    </div>
                    <div>
                      <p class="text-xs text-gray-400">Seller</p>
                      <p class="font-medium text-gray-800">{{ tx.seller }}</p>
                      @if (tx.sellerId) {
                        <p class="text-xs text-gray-400 font-mono">{{ tx.sellerId }}</p>
                      }
                    </div>
                  </div>
                </div>

                @if (tx.riderId) {
                  <div class="bg-purple-50 rounded-lg p-4 border border-purple-100">
                    <p class="text-xs text-purple-600 font-semibold uppercase tracking-wider">
                      <i class="fas fa-motorcycle mr-1"></i> Rider
                    </p>
                    <p class="font-medium text-gray-800">{{ tx.riderName || 'Assigned' }}</p>
                    <p class="text-xs text-gray-400 font-mono">ID: {{ tx.riderId }}</p>
                    <!-- Use getDeliveryField() method to safely access delivery fields -->
                    @if (getDeliveryField('assignmentStatus', tx)) {
                      <p class="text-xs text-purple-600 mt-1">
                        Status: {{ getDeliveryField('assignmentStatus', tx) }}
                      </p>
                    }
                    @if (getDeliveryField('pickupAddress', tx)) {
                      <p class="text-xs text-gray-500 mt-1">
                        Pickup: {{ getDeliveryField('pickupAddress', tx) }}
                      </p>
                    }
                    @if (getDeliveryField('dropoffAddress', tx)) {
                      <p class="text-xs text-gray-500">
                        Dropoff: {{ getDeliveryField('dropoffAddress', tx) }}
                      </p>
                    }
                  </div>
                }

                @if (tx.autoReleaseDate || tx.autoReleaseAt) {
                  <div class="bg-yellow-50 rounded-lg p-4 border border-yellow-100">
                    <p class="text-xs text-yellow-600 font-semibold uppercase tracking-wider">
                      <i class="fas fa-clock mr-1"></i> Auto Release
                    </p>
                    <p class="font-medium text-gray-800">{{ (tx.autoReleaseDate || tx.autoReleaseAt) | date:'medium' }}</p>
                  </div>
                }

                @if (tx.deliveryDueAt) {
                  <div class="bg-blue-50 rounded-lg p-4 border border-blue-100">
                    <p class="text-xs text-blue-600 font-semibold uppercase tracking-wider">
                      <i class="fas fa-truck mr-1"></i> Delivery Due
                    </p>
                    <p class="font-medium text-gray-800">{{ tx.deliveryDueAt | date:'medium' }}</p>
                  </div>
                }
              </div>
            </div>

            <!-- Dispute Info -->
            @if (tx.disputeId) {
              <div class="mt-6 p-4 bg-red-50 rounded-lg border border-red-200">
                <p class="text-sm font-semibold text-red-700">
                  <i class="fas fa-gavel mr-2"></i> Dispute Open
                </p>
                <p class="text-sm text-red-600">Dispute ID: {{ tx.disputeId }}</p>
                <a [routerLink]="['/disputes']" class="text-sm text-red-700 hover:underline font-medium">
                  View Dispute →
                </a>
              </div>
            }

            <!-- Actions -->
            <div class="mt-6 pt-6 border-t border-gray-200 flex flex-wrap gap-3">
              <a routerLink="/rider-assignments" class="px-4 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition">
                <i class="fas fa-arrow-left mr-1"></i> Back
              </a>
              @if (tx.riderId) {
                <a [routerLink]="['/users', tx.riderId]" class="px-4 py-2 bg-purple-100 text-purple-700 rounded-lg hover:bg-purple-200 transition">
                  <i class="fas fa-motorcycle mr-1"></i> View Rider
                </a>
              }
              <a [routerLink]="['/transactions']" class="px-4 py-2 bg-indigo-50 text-indigo-700 rounded-lg hover:bg-indigo-100 transition">
                <i class="fas fa-list mr-1"></i> All Transactions
              </a>
              @if (tx.status === 'FUNDS_HELD' && !tx.riderId) {
                <a [routerLink]="['/rider-assignments']" class="px-4 py-2 bg-emerald-50 text-emerald-700 rounded-lg hover:bg-emerald-100 transition">
                  <i class="fas fa-motorcycle mr-1"></i> Assign Rider
                </a>
              }
            </div>
          </div>
        </div>
      }

      <!-- Not Found State -->
      @if (!loading() && !transaction()) {
        <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-12 text-center">
          <i class="fas fa-inbox text-5xl text-gray-300 mb-4 block"></i>
          <h3 class="text-xl font-semibold text-gray-700">Transaction Not Found</h3>
          <p class="text-gray-400 mt-2">The transaction you're looking for doesn't exist or has been removed.</p>
          <a routerLink="/rider-assignments" class="inline-block mt-4 px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition">
            <i class="fas fa-arrow-left mr-1"></i> Back to Assignments
          </a>
        </div>
      }
    </div>
  `,
  styles: [`
    .bg-gradient-to-r {
      background-image: linear-gradient(to right, #f9fafb, #ffffff);
    }
  `]
})
export class TransactionDetailsComponent implements OnInit {
  private dataService = inject(DataService);
  private apiService = inject(ApiService);
  private route = inject(ActivatedRoute);
  private notificationService = inject(NotificationService);

  loading = signal(true);
  transaction = signal<Transaction | null>(null);

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadTransaction(id);
    } else {
      this.loading.set(false);
      this.notificationService.add('Error', 'Transaction ID not found', 'danger');
    }
  }

  /**
   * Safely get delivery field from transaction
   * This avoids using TypeScript 'as' in templates
   */
  getDeliveryField(field: keyof TransactionWithDelivery, tx: Transaction): string | undefined {
    const txWithDelivery = tx as TransactionWithDelivery;
    const value = txWithDelivery[field];
    return value as string | undefined;
  }

  /**
   * Load transaction details - tries DataService first, then API
   */
  loadTransaction(id: string) {
    this.loading.set(true);

    // 1️⃣ Try DataService first (mock data or cached)
    const dataServiceTx = this.dataService.getTransactionById(id);
    if (dataServiceTx) {
      console.log('✅ Transaction loaded from DataService:', id);
      this.transaction.set(dataServiceTx);
      this.loading.set(false);
      return;
    }

    // 2️⃣ Try API as fallback
    console.log('⏳ Transaction not in DataService, fetching from API:', id);
    this.apiService.getTransactionById(id).subscribe({
      next: (response: any) => {
        if (response?.data) {
          // Map API response to Transaction model
          const tx = this.mapApiTransaction(response.data);
          this.transaction.set(tx);
          this.loading.set(false);
        } else {
          console.error('Invalid transaction data received');
          this.notificationService.add('Error', 'Invalid transaction data', 'danger');
          this.loading.set(false);
        }
      },
      error: (err: any) => {
        console.error('Failed to load transaction:', err);
        
        // 3️⃣ Try DataService one more time (maybe it loaded in the meantime)
        const retryTx = this.dataService.getTransactionById(id);
        if (retryTx) {
          console.log('✅ Transaction loaded from DataService (retry):', id);
          this.transaction.set(retryTx);
          this.notificationService.add('Info', 'Using mock data for transaction.', 'info');
        } else {
          this.notificationService.add('Error', 'Could not load transaction details. Please try again.', 'danger');
          this.transaction.set(null);
        }
        this.loading.set(false);
      }
    });
  }

  /**
   * Map API transaction to frontend Transaction model
   */
  private mapApiTransaction(apiTx: any): Transaction {
    return {
      id: apiTx.id || '',
      reference: apiTx.reference || apiTx.id,
      buyer: apiTx.buyer || apiTx.buyerId || 'Unknown Buyer',
      buyerId: apiTx.buyerId || '',
      seller: apiTx.seller || apiTx.sellerId || 'Unknown Seller',
      sellerId: apiTx.sellerId || '',
      title: apiTx.title || apiTx.productDescription || 'Transaction',
      productDescription: apiTx.productDescription || apiTx.description || '',
      amount: Number(apiTx.amount) || 0,
      initialDepositAmount: apiTx.initialDepositAmount ? Number(apiTx.initialDepositAmount) : undefined,
      currency: apiTx.currency || 'KES',
      status: apiTx.status || 'CREATED',
      created: apiTx.createdAt || apiTx.created || new Date().toISOString(),
      createdAt: apiTx.createdAt || apiTx.created || new Date().toISOString(),
      updatedAt: apiTx.updatedAt || apiTx.updated || undefined,
      completedAt: apiTx.completedAt || apiTx.completed || undefined,
      deliveryDueAt: apiTx.deliveryDueAt || undefined,
      autoReleaseDate: apiTx.autoReleaseDate || apiTx.autoReleaseAt || undefined,
      autoReleaseAt: apiTx.autoReleaseAt || apiTx.autoReleaseDate || undefined,
      description: apiTx.description || apiTx.productDescription || '',
      deliveryTimeline: apiTx.deliveryTimeline || undefined,
      disputeId: apiTx.disputeId || undefined,
      riderId: apiTx.riderId || undefined,
      riderName: apiTx.riderName || undefined
    };
  }

  /**
   * Get status badge class
   */
  getStatusBadge(status: string): string {
    const badges: Record<string, string> = {
      'CREATED': 'bg-gray-100 text-gray-700',
      'PENDING_PAYMENT': 'bg-yellow-100 text-yellow-800',
      'FUNDS_HELD': 'bg-yellow-100 text-yellow-800',
      'SELLER_ACCEPTED': 'bg-blue-100 text-blue-800',
      'IN_DELIVERY': 'bg-blue-100 text-blue-800',
      'SELLER_DELIVERED': 'bg-purple-100 text-purple-800',
      'BUYER_CONFIRMED_DELIVERED': 'bg-green-100 text-green-800',
      'RELEASE_PENDING': 'bg-orange-100 text-orange-800',
      'RELEASE_PROCESSING': 'bg-orange-100 text-orange-800',
      'RELEASE_FAILED': 'bg-red-100 text-red-800',
      'COMPLETED': 'bg-green-100 text-green-800',
      'DISPUTED': 'bg-red-100 text-red-800',
      'REFUND_PENDING': 'bg-rose-100 text-rose-800',
      'REFUND_PROCESSING': 'bg-rose-100 text-rose-800',
      'REFUNDED': 'bg-gray-100 text-gray-700',
      'CANCELLED': 'bg-gray-100 text-gray-700',
      'EXPIRED': 'bg-gray-100 text-gray-700'
    };
    return badges[status] || 'bg-gray-100 text-gray-600';
  }

  /**
   * Get status icon
   */
  getStatusIcon(status: string): string {
    const icons: Record<string, string> = {
      'CREATED': 'fa-plus-circle',
      'PENDING_PAYMENT': 'fa-clock',
      'FUNDS_HELD': 'fa-lock',
      'SELLER_ACCEPTED': 'fa-check-circle',
      'IN_DELIVERY': 'fa-truck',
      'SELLER_DELIVERED': 'fa-box',
      'BUYER_CONFIRMED_DELIVERED': 'fa-check-double',
      'RELEASE_PENDING': 'fa-hourglass-half',
      'RELEASE_PROCESSING': 'fa-spinner fa-spin',
      'RELEASE_FAILED': 'fa-exclamation-circle',
      'COMPLETED': 'fa-check-circle',
      'DISPUTED': 'fa-gavel',
      'REFUND_PENDING': 'fa-undo-alt',
      'REFUND_PROCESSING': 'fa-spinner fa-spin',
      'REFUNDED': 'fa-undo-alt',
      'CANCELLED': 'fa-times-circle',
      'EXPIRED': 'fa-hourglass-end'
    };
    return icons[status] || 'fa-question-circle';
  }

  /**
   * Refresh transaction data
   */
  refreshTransaction(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadTransaction(id);
    }
  }
}
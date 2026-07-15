// src/app/features/transaction-history/transaction-history.ts

import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ApiService } from '../../core/services/api.service';
import { TransactionStatusHistory } from '../../core/models/transaction-history';

@Component({
  selector: 'app-transaction-history',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './transaction-history.html',
  styleUrls: ['./transaction-history.css']
})
export class TransactionHistoryComponent implements OnInit {
  private apiService = inject(ApiService);
  private route = inject(ActivatedRoute);

  // Transaction ID from route parameter
  transactionId = '';

  // Signal for history entries
  history = signal<TransactionStatusHistory[]>([]);

  // Loading state
  isLoading = signal(false);

  // Error state (optional)
  error = signal<string | null>(null);

  ngOnInit() {
    // Get transaction ID from route
    this.transactionId = this.route.snapshot.paramMap.get('id') || '';

    if (this.transactionId) {
      this.loadHistory();
    } else {
      this.error.set('No transaction ID provided.');
    }
  }

  /**
   * Load transaction status history from the API.
   * Uses the new getTransactionStatusHistory method.
   */
  loadHistory(): void {
    this.isLoading.set(true);
    this.error.set(null);

    this.apiService.getTransactionStatusHistory(this.transactionId).subscribe({
      next: (response) => {
        // Handle both ApiResponse wrapper and direct array
        let historyData: TransactionStatusHistory[] = [];

        if (response && Array.isArray(response)) {
          // Direct array response
          historyData = response;
        } else if (response && response.data && Array.isArray(response.data)) {
          // ApiResponse wrapper
          historyData = response.data;
        }

        this.history.set(historyData);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Failed to load transaction history:', err);
        this.error.set(err.message || 'Failed to load status history.');
        this.isLoading.set(false);
        this.history.set([]);
      }
    });
  }

  /**
   * Format date string for display.
   */
  formatDate(dateString: string): string {
    if (!dateString) return 'N/A';
    try {
      return new Date(dateString).toLocaleString('en-KE', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
      });
    } catch {
      return dateString;
    }
  }

  /**
   * Get a human-readable label for the status change.
   */
  getStatusLabel(status: string): string {
    const statusMap: Record<string, string> = {
      'CREATED': 'Created',
      'PENDING_PAYMENT': 'Awaiting Payment',
      'FUNDS_HELD': 'Funds Held',
      'SELLER_ACCEPTED': 'Seller Accepted',
      'IN_DELIVERY': 'In Delivery',
      'SELLER_DELIVERED': 'Seller Delivered',
      'BUYER_CONFIRMED_DELIVERED': 'Buyer Confirmed Delivery',
      'RELEASE_PENDING': 'Release Pending',
      'RELEASE_PROCESSING': 'Releasing Funds',
      'RELEASE_FAILED': 'Release Failed',
      'COMPLETED': 'Completed',
      'DISPUTED': 'Disputed',
      'REFUND_PENDING': 'Refund Pending',
      'REFUND_PROCESSING': 'Refunding',
      'REFUNDED': 'Refunded',
      'CANCELLED': 'Cancelled',
      'EXPIRED': 'Expired'
    };
    return statusMap[status] || status;
  }

  /**
   * Get the color class for a status badge.
   */
  getStatusColor(status: string): string {
    const colorMap: Record<string, string> = {
      'CREATED': 'bg-gray-100 text-gray-700',
      'PENDING_PAYMENT': 'bg-blue-100 text-blue-700',
      'FUNDS_HELD': 'bg-yellow-100 text-yellow-700',
      'SELLER_ACCEPTED': 'bg-indigo-100 text-indigo-700',
      'IN_DELIVERY': 'bg-purple-100 text-purple-700',
      'SELLER_DELIVERED': 'bg-teal-100 text-teal-700',
      'BUYER_CONFIRMED_DELIVERED': 'bg-cyan-100 text-cyan-700',
      'RELEASE_PENDING': 'bg-amber-100 text-amber-700',
      'RELEASE_PROCESSING': 'bg-orange-100 text-orange-700',
      'RELEASE_FAILED': 'bg-red-100 text-red-700',
      'COMPLETED': 'bg-green-100 text-green-700',
      'DISPUTED': 'bg-red-100 text-red-700',
      'REFUND_PENDING': 'bg-rose-100 text-rose-700',
      'REFUND_PROCESSING': 'bg-pink-100 text-pink-700',
      'REFUNDED': 'bg-gray-100 text-gray-700',
      'CANCELLED': 'bg-gray-100 text-gray-700',
      'EXPIRED': 'bg-gray-100 text-gray-700'
    };
    return colorMap[status] || 'bg-gray-100 text-gray-700';
  }

  /**
   * Refresh the history data.
   */
  refresh(): void {
    if (this.transactionId) {
      this.loadHistory();
    }
  }
}
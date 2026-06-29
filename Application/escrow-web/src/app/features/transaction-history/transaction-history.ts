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

  transactionId = '';
  history = signal<TransactionStatusHistory[]>([]);
  isLoading = signal(false);

  ngOnInit() {
    this.transactionId = this.route.snapshot.paramMap.get('id') || '';
    if (this.transactionId) {
      this.loadHistory();
    }
  }

  loadHistory() {
    this.isLoading.set(true);
    this.apiService.getTransactionHistory(this.transactionId).subscribe({
      next: (res) => {
        this.history.set(res.data || []);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Failed to load transaction history', err);
        this.isLoading.set(false);
        this.history.set([]);
      }
    });
  }

  formatDate(dateString: string): string {
    return new Date(dateString).toLocaleString('en-KE', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }
}
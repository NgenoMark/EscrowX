import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ApiService } from '../../core/services/api.service';
import { EscrowLedgerEntry } from '../../core/models/escrow-ledger-entry';

@Component({
  selector: 'app-ledger',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './ledger.html',
  styleUrls: ['./ledger.css']
})
export class LedgerComponent implements OnInit {
  private apiService = inject(ApiService);
  private route = inject(ActivatedRoute);

  transactionId = '';
  entries = signal<EscrowLedgerEntry[]>([]);
  isLoading = signal(false);

  ngOnInit() {
    this.transactionId = this.route.snapshot.paramMap.get('id') || '';
    if (this.transactionId) {
      this.loadEntries();
    }
  }

  loadEntries() {
    this.isLoading.set(true);
    this.apiService.getLedgerEntries(this.transactionId).subscribe({
      next: (res) => {
        this.entries.set(res.data || []);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Failed to load ledger entries', err);
        this.isLoading.set(false);
        this.entries.set([]);
      }
    });
  }

  getTotalCredit(): number {
    return this.entries()
      .filter(e => e.direction === 'CREDIT')
      .reduce((sum, e) => sum + e.amount, 0);
  }

  getTotalDebit(): number {
    return this.entries()
      .filter(e => e.direction === 'DEBIT')
      .reduce((sum, e) => sum + e.amount, 0);
  }

  getBalance(): number {
    return this.getTotalCredit() - this.getTotalDebit();
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

  getEntryTypeBadge(type: string): string {
    const map: Record<string, string> = {
      'HOLD': 'bg-yellow-100 text-yellow-800',
      'RELEASE': 'bg-green-100 text-green-800',
      'REFUND': 'bg-red-100 text-red-800',
      'FEE': 'bg-gray-100 text-gray-800'
    };
    return map[type] || 'bg-gray-100';
  }

  getDirectionBadge(direction: string): string {
    return direction === 'CREDIT' 
      ? 'bg-emerald-100 text-emerald-800' 
      : 'bg-rose-100 text-rose-800';
  }
}
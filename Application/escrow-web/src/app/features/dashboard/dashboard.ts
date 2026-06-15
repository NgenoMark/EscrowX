import { Component, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { DataService } from '../../core/services/data';
import { SearchService } from '../../core/services/search';
import { Transaction } from '../../core/models/transaction';
import { StatCardComponent } from '../../shared/ui/stat-card/stat-card';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, StatCardComponent],
  templateUrl: './dashboard.html',
  styleUrls: ['./dashboard.css']
})

export class DashboardComponent {
  private dataService = inject(DataService);
  private searchService = inject(SearchService);

  totalVolume = computed(() => this.dataService.getTotalVolume());
  platformFees = computed(() => this.dataService.getPlatformFees());
  activeEscrows = computed(() => this.dataService.getActiveEscrows());
  openDisputes = computed(() => this.dataService.getOpenDisputes());
  recentTransactions = computed(() => this.dataService.getRecentTransactions(5));

  getStatusClass(status: string): string {
    switch(status) {
      case 'FUNDS_HELD': return 'bg-yellow-100 text-yellow-800';
      case 'COMPLETED': return 'bg-green-100 text-green-800';
      case 'DISPUTED': return 'bg-red-100 text-red-800';
      case 'REFUNDED': return 'bg-gray-100 text-gray-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  }

  filteredRecentTransactions(): Transaction[] {
    const term = this.searchService.query().toLowerCase().trim();
    const recentTransactions = this.recentTransactions();
    if (!term) return recentTransactions;

    return recentTransactions.filter(tx =>
      tx.id.toLowerCase().includes(term) ||
      tx.buyer.toLowerCase().includes(term) ||
      tx.seller.toLowerCase().includes(term) ||
      tx.status.toLowerCase().includes(term) ||
      tx.amount.toString().includes(term)
    );
  }
}

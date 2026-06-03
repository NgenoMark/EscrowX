import { Component, inject, OnInit } from '@angular/core';
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
export class DashboardComponent implements OnInit {
  private dataService = inject(DataService);
  private searchService = inject(SearchService);
  
  // Dashboard metrics
  totalVolume: number = 0;
  platformFees: number = 0;
  activeEscrows: number = 0;
  openDisputes: number = 0;
  recentTransactions: Transaction[] = [];

  ngOnInit() {
    this.loadDashboardData();
  }

  loadDashboardData() {
    this.totalVolume = this.dataService.getTotalVolume();
    this.platformFees = this.dataService.getPlatformFees();
    this.activeEscrows = this.dataService.getActiveEscrows();
    this.openDisputes = this.dataService.getOpenDisputes();
    this.recentTransactions = this.dataService.getRecentTransactions(5);
  }

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
    if (!term) return this.recentTransactions;

    return this.recentTransactions.filter(tx =>
      tx.id.toLowerCase().includes(term) ||
      tx.buyer.toLowerCase().includes(term) ||
      tx.seller.toLowerCase().includes(term) ||
      tx.status.toLowerCase().includes(term) ||
      tx.amount.toString().includes(term)
    );
  }
}

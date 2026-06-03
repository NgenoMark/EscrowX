import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DataService } from '../../core/services/data';
import { SearchService } from '../../core/services/search';
import { Transaction } from '../../core/models/transaction';

@Component({
  selector: 'app-transactions',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './transactions.html',
  styleUrls: ['./transactions.css']
})
export class TransactionsComponent implements OnInit {
  private dataService = inject(DataService);
  private searchService = inject(SearchService);
  
  // Filters
  statusFilter = signal<string>('all');
  searchTerm = this.searchService.query;
  
  // Selected transaction for modal
  selectedTransaction = signal<Transaction | null>(null);
  showModal = signal<boolean>(false);
  
  // Filtered transactions (computed - auto-updates)
  filteredTransactions = computed(() => {
    return this.dataService.getFilteredTransactions(
      this.statusFilter(), 
      this.searchTerm()
    );
  });
  
  // Transaction statistics
  stats = computed(() => this.dataService.getTransactionStats());
  
  ngOnInit() {
    // No manual load needed - signals handle reactivity
  }
  
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
  
  forceRelease(transaction: Transaction): void {
    if (confirm(`⚠️ FORCE RELEASE FUNDS\n\nTransaction: ${transaction.id}\nAmount: KES ${transaction.amount.toLocaleString()}\nSeller: ${transaction.seller}\n\nThis will immediately release the funds to the seller. Continue?`)) {
      this.dataService.forceReleaseFunds(transaction.id);
    }
  }
  
  forceRefund(transaction: Transaction): void {
    if (confirm(`⚠️ FORCE REFUND\n\nTransaction: ${transaction.id}\nAmount: KES ${transaction.amount.toLocaleString()}\nBuyer: ${transaction.buyer}\n\nThis will immediately refund the buyer. Continue?`)) {
      this.dataService.forceRefund(transaction.id);
    }
  }
  
  cancelTransaction(transaction: Transaction): void {
    if (confirm(`❌ CANCEL TRANSACTION\n\nTransaction: ${transaction.id}\nAmount: KES ${transaction.amount.toLocaleString()}\n\nThis will cancel the transaction and refund the buyer. Continue?`)) {
      this.dataService.cancelTransaction(transaction.id);
    }
  }
  
  viewTransactionDetails(transaction: Transaction): void {
    this.selectedTransaction.set(transaction);
    this.showModal.set(true);
  }
  
  closeModal(): void {
    this.showModal.set(false);
    this.selectedTransaction.set(null);
  }
  
  getStatusBadgeClass(status: string): string {
    switch(status) {
      case 'FUNDS_HELD': return 'bg-yellow-100 text-yellow-800';
      case 'COMPLETED': return 'bg-green-100 text-green-800';
      case 'DISPUTED': return 'bg-red-100 text-red-800';
      case 'REFUNDED': return 'bg-gray-100 text-gray-800';
      case 'CANCELLED': return 'bg-gray-100 text-gray-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  }
  
  getStatusIcon(status: string): string {
    switch(status) {
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
}

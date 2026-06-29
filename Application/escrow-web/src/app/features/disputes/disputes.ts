// src/app/features/disputes/disputes.ts
import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DataService } from '../../core/services/data';
import { SearchService } from '../../core/services/search';
import { Dispute } from '../../core/models/dispute';
import { Transaction } from '../../core/models/transaction';
import { NgxSmkSkeletonDirective } from 'ngxsmk-skeleton-loader';

@Component({
  selector: 'app-disputes',
  standalone: true,
  imports: [CommonModule, FormsModule, NgxSmkSkeletonDirective],
  templateUrl: './disputes.html',
  styleUrls: ['./disputes.css']
})

export class DisputesComponent implements OnInit {
  private dataService = inject(DataService);
  private searchService = inject(SearchService);

  // ---- State ----
  isLoading = this.dataService.isLoading;
  statusFilter = signal<string>('all');
  searchTerm = this.searchService.query;
  searchInput = signal<string>('');

  selectedDispute = signal<Dispute | null>(null);
  associatedTransaction = signal<Transaction | null>(null);

  // Partial settlement
  partialAmount = signal<number>(0);
  partialPercentage = signal<number>(50);
  adminNotes = signal<string>('');
  showPartialSettlement = signal<boolean>(false);

  // Evidence modal
  showEvidenceModal = signal<boolean>(false);
  selectedEvidence = signal<string>('');
  imageError = signal<boolean>(false);

  // ---- Computed ----
  filteredDisputes = computed(() => {
    const term = this.searchTerm().toLowerCase().trim();
    const disputes = this.dataService.getFilteredDisputes(this.statusFilter());

    if (!term) return disputes;

    return disputes.filter(dispute =>
      dispute.id.toLowerCase().includes(term) ||
      dispute.transactionId.toLowerCase().includes(term) ||
      dispute.raisedByName.toLowerCase().includes(term) ||
      (dispute.category || '').toLowerCase().includes(term) ||
      dispute.status.toLowerCase().includes(term)
    );
  });

  stats = computed(() => this.dataService.getDisputeStats());

  ngOnInit() {
    this.searchInput.set(this.searchTerm());
  }

  // ---- Search ----
  searchDisputes(): void {
    this.searchService.setQuery(this.searchInput().trim());
  }

  clearSearch(): void {
    this.searchInput.set('');
    this.searchService.clear();
  }

  onSearchKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter') {
      event.preventDefault();
      this.searchDisputes();
    }
  }

  // ---- Filter ----
  onStatusFilterChange(event: Event): void {
    const select = event.target as HTMLSelectElement;
    this.statusFilter.set(select.value);
  }

  // ---- Selection – fetches full dispute details ----
  async selectDispute(dispute: Dispute): Promise<void> {
    // Show summary immediately
    this.selectedDispute.set(dispute);

    // Fetch full details from backend
    const full = await this.dataService.fetchFullDispute(dispute.id);
    if (full) {
      this.selectedDispute.set(full);
      const transaction = this.dataService.getTransactionById(full.transactionId);
      this.associatedTransaction.set(transaction || null);
    } else {
      // Fallback: use summary data
      const transaction = this.dataService.getTransactionById(dispute.transactionId);
      this.associatedTransaction.set(transaction || null);
    }

    // Initialize partial settlement values
    this.partialAmount.set(Math.floor(dispute.amount * 0.5));
    this.partialPercentage.set(50);
    this.adminNotes.set('');
    this.showPartialSettlement.set(false);
  }

  clearSelection(): void {
    this.selectedDispute.set(null);
    this.associatedTransaction.set(null);
    this.partialAmount.set(0);
    this.partialPercentage.set(50);
    this.adminNotes.set('');
    this.showPartialSettlement.set(false);
  }

  // ---- Partial Settlement ----
  togglePartialSettlement(): void {
    this.showPartialSettlement.set(!this.showPartialSettlement());
    if (this.showPartialSettlement() && this.selectedDispute()) {
      this.partialAmount.set(Math.floor(this.selectedDispute()!.amount * 0.5));
      this.partialPercentage.set(50);
    }
  }

  updatePartialAmount(): void {
    const dispute = this.selectedDispute();
    if (dispute) {
      this.partialPercentage.set(Math.floor((this.partialAmount() / dispute.amount) * 100));
    }
  }

  // ---- Resolution Actions ----
  resolveRefundBuyer(): void {
    const dispute = this.selectedDispute();
    if (dispute && confirm(`REFUND BUYER\n\nAmount: KES ${dispute.amount.toLocaleString()}\nBuyer: ${dispute.raisedByName}\n\nProceed?`)) {
      this.dataService.resolveDisputeRefundBuyer(dispute.id);
      this.clearSelection();
    }
  }

  resolveReleaseSeller(): void {
    const dispute = this.selectedDispute();
    if (dispute && confirm(`RELEASE TO SELLER\n\nAmount: KES ${dispute.amount.toLocaleString()}\nSeller: ${dispute.against || 'N/A'}\n\nProceed?`)) {
      this.dataService.resolveDisputeReleaseSeller(dispute.id);
      this.clearSelection();
    }
  }

  resolvePartialSettlement(): void {
    const dispute = this.selectedDispute();
    if (dispute && this.partialAmount() > 0 && this.partialAmount() < dispute.amount) {
      const notes = this.adminNotes() || 'Partial settlement agreed';
      if (confirm(`PARTIAL SETTLEMENT\n\nTotal: KES ${dispute.amount.toLocaleString()}\nBuyer gets: KES ${this.partialAmount().toLocaleString()}\nSeller gets: KES ${(dispute.amount - this.partialAmount()).toLocaleString()}\n\nProceed?`)) {
        this.dataService.resolveDisputePartial(dispute.id, this.partialAmount(), notes);
        this.clearSelection();
      }
    } else {
      alert('Invalid partial amount. Must be between 1 and ' + (dispute?.amount ? dispute.amount - 1 : 0));
    }
  }

  updateStatus(status: 'PENDING' | 'UNDER_REVIEW' | 'ESCALATED'): void {
    const dispute = this.selectedDispute();
    if (dispute) {
      this.dataService.updateDisputeStatus(dispute.id, status);
      const updated = this.dataService.getDisputeById(dispute.id);
      if (updated) this.selectedDispute.set(updated);
    }
  }

  saveAdminNote(): void {
    const dispute = this.selectedDispute();
    const note = this.adminNotes();
    if (dispute && note.trim()) {
      this.dataService.addAdminNote(dispute.id, note);
      this.adminNotes.set('');
      const updated = this.dataService.getDisputeById(dispute.id);
      if (updated) this.selectedDispute.set(updated);
      alert('Admin note saved');
    }
  }

  // ---- Evidence Modal ----
  viewEvidence(url: string): void {
    this.selectedEvidence.set(url);
    this.imageError.set(false);
    this.showEvidenceModal.set(true);
  }

  closeEvidenceModal(): void {
    this.showEvidenceModal.set(false);
    this.selectedEvidence.set('');
    this.imageError.set(false);
  }

  isImage(url: string): boolean {
    if (!url) return false;
    return /\.(jpg|jpeg|png|gif|webp|bmp|svg)$/i.test(url) || url.includes('image');
  }

  getFileName(url: string): string {
    if (!url) return 'file';
    return url.split('/').pop() || 'file';
  }

  // ---- Helpers ----
  getStatusBadgeClass(status: string): string {
    const map: Record<string, string> = {
      'PENDING': 'bg-yellow-100 text-yellow-800',
      'UNDER_REVIEW': 'bg-blue-100 text-blue-800',
      'RESOLVED': 'bg-green-100 text-green-800',
      'ESCALATED': 'bg-red-100 text-red-800',
      'OPEN': 'bg-yellow-100 text-yellow-800'
    };
    return map[status] || 'bg-gray-100 text-gray-800';
  }

  getStatusIcon(status: string): string {
    const map: Record<string, string> = {
      'PENDING': 'fa-clock',
      'UNDER_REVIEW': 'fa-search',
      'RESOLVED': 'fa-check-circle',
      'ESCALATED': 'fa-exclamation-triangle',
      'OPEN': 'fa-clock'
    };
    return map[status] || 'fa-question-circle';
  }

  formatDate(dateString: string): string {
    if (!dateString) return 'N/A';
    return new Date(dateString).toLocaleDateString('en-KE');
  }
}
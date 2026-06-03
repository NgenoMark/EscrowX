import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DataService } from '../../core/services/data';
import { SearchService } from '../../core/services/search';
import { Dispute } from '../../core/models/dispute';
import { Transaction } from '../../core/models/transaction';

@Component({
  selector: 'app-disputes',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './disputes.html',
  styleUrls: ['./disputes.css']
})
export class DisputesComponent implements OnInit {
  private dataService = inject(DataService);
  private searchService = inject(SearchService);
  
  // Filters
  statusFilter = signal<string>('all');
  searchTerm = this.searchService.query;
  
  // Selected dispute for mediation
  selectedDispute = signal<Dispute | null>(null);
  associatedTransaction = signal<Transaction | null>(null);
  
  // Partial settlement values
  partialAmount = signal<number>(0);
  partialPercentage = signal<number>(50);
  adminNotes = signal<string>('');
  showPartialSettlement = signal<boolean>(false);
  
  // Show evidence modal
  showEvidenceModal = signal<boolean>(false);
  selectedEvidence = signal<string>('');
  
  // Filtered disputes (computed)
  filteredDisputes = computed(() => {
    const term = this.searchTerm().toLowerCase().trim();
    const disputes = this.dataService.getFilteredDisputes(this.statusFilter());

    if (!term) return disputes;

    return disputes.filter(dispute =>
      dispute.id.toLowerCase().includes(term) ||
      dispute.txId.toLowerCase().includes(term) ||
      dispute.raisedBy.toLowerCase().includes(term) ||
      dispute.against.toLowerCase().includes(term) ||
      dispute.reason.toLowerCase().includes(term) ||
      dispute.status.toLowerCase().includes(term)
    );
  });
  
  // Dispute statistics
  stats = computed(() => this.dataService.getDisputeStats());
  
  ngOnInit() {
    // No manual load needed - signals handle reactivity
  }
  
  onStatusFilterChange(event: Event): void {
    const select = event.target as HTMLSelectElement;
    this.statusFilter.set(select.value);
  }
  
  selectDispute(dispute: Dispute): void {
    this.selectedDispute.set(dispute);
    // Load associated transaction
    const transaction = this.dataService.getTransactionById(dispute.txId);
    this.associatedTransaction.set(transaction || null);
    
    // Reset form values
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
  
  togglePartialSettlement(): void {
    this.showPartialSettlement.set(!this.showPartialSettlement());
    // Initialize partial amount when opening
    if (this.showPartialSettlement() && this.selectedDispute()) {
      this.partialAmount.set(Math.floor(this.selectedDispute()!.amount * 0.5));
      this.partialPercentage.set(50);
    }
  }
  
  updatePartialAmount(): void {
    const dispute = this.selectedDispute();
    if (dispute) {
      this.partialPercentage.set(Math.floor(this.partialAmount() / dispute.amount * 100));
    }
  }
  
  updatePartialPercentage(): void {
    const dispute = this.selectedDispute();
    if (dispute) {
      this.partialAmount.set(Math.floor(dispute.amount * this.partialPercentage() / 100));
    }
  }
  
  resolveRefundBuyer(): void {
    const dispute = this.selectedDispute();
    if (dispute && confirm(`REFUND BUYER\n\nAmount: KES ${dispute.amount.toLocaleString()}\nBuyer: ${dispute.raisedBy}\n\nThis will refund the full amount to the buyer. Continue?`)) {
      this.dataService.resolveDisputeRefundBuyer(dispute.id);
      this.clearSelection();
    }
  }
  
  resolveReleaseSeller(): void {
    const dispute = this.selectedDispute();
    if (dispute && confirm(`RELEASE TO SELLER\n\nAmount: KES ${dispute.amount.toLocaleString()}\nSeller: ${dispute.against}\n\nThis will release the full amount to the seller. Continue?`)) {
      this.dataService.resolveDisputeReleaseSeller(dispute.id);
      this.clearSelection();
    }
  }
  
  resolvePartialSettlement(): void {
    const dispute = this.selectedDispute();
    if (dispute && this.partialAmount() > 0 && this.partialAmount() < dispute.amount) {
      const notes = this.adminNotes() || 'Partial settlement agreed';
      if (confirm(`PARTIAL SETTLEMENT\n\nTotal Amount: KES ${dispute.amount.toLocaleString()}\nPartial Amount: KES ${this.partialAmount().toLocaleString()} (${this.partialPercentage()}%)\nBuyer gets: KES ${this.partialAmount().toLocaleString()}\nSeller gets: KES ${(dispute.amount - this.partialAmount()).toLocaleString()}\n\nContinue?`)) {
        this.dataService.resolveDisputePartial(dispute.id, this.partialAmount(), notes);
        this.clearSelection();
      }
    } else {
      alert('Please enter a valid partial amount between 1 and ' + (dispute?.amount ? dispute.amount - 1 : 0));
    }
  }
  
  updateStatus(status: 'PENDING' | 'UNDER_REVIEW' | 'ESCALATED'): void {
    const dispute = this.selectedDispute();
    if (dispute) {
      this.dataService.updateDisputeStatus(dispute.id, status);
      // Refresh selected dispute
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
      // Refresh selected dispute
      const updated = this.dataService.getDisputeById(dispute.id);
      if (updated) this.selectedDispute.set(updated);
      alert('Admin note saved successfully');
    }
  }
  
  viewEvidence(evidenceUrl: string): void {
    this.selectedEvidence.set(evidenceUrl);
    this.showEvidenceModal.set(true);
  }
  
  closeEvidenceModal(): void {
    this.showEvidenceModal.set(false);
    this.selectedEvidence.set('');
  }
  
  getStatusBadgeClass(status: string): string {
    switch(status) {
      case 'PENDING': return 'bg-yellow-100 text-yellow-800';
      case 'UNDER_REVIEW': return 'bg-blue-100 text-blue-800';
      case 'RESOLVED': return 'bg-green-100 text-green-800';
      case 'ESCALATED': return 'bg-red-100 text-red-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  }
  
  getStatusIcon(status: string): string {
    switch(status) {
      case 'PENDING': return 'fa-clock';
      case 'UNDER_REVIEW': return 'fa-search';
      case 'RESOLVED': return 'fa-check-circle';
      case 'ESCALATED': return 'fa-exclamation-triangle';
      default: return 'fa-question-circle';
    }
  }
  
  getResolutionBadgeClass(resolution?: string): string {
    switch(resolution) {
      case 'REFUND_BUYER': return 'bg-red-100 text-red-800';
      case 'RELEASE_SELLER': return 'bg-green-100 text-green-800';
      case 'PARTIAL_SETTLEMENT': return 'bg-purple-100 text-purple-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  }
  
  formatDate(dateString: string): string {
    if (!dateString) return 'N/A';
    return new Date(dateString).toLocaleDateString('en-KE');
  }
}

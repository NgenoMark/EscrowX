// src/app/features/riders/assignments/assignments.ts

import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { DataService } from '../../core/services/data';
import { ApiService } from '../../core/services/api.service';
import { NotificationService } from '../../core/services/notifications';
import { AppEnvironmentService } from '../../core/config/app-environment';
import { Rider, DeliveryAssignmentStatus } from '../../core/models/rider';
import { Transaction } from '../../core/models/transaction';
import { AssignRiderModalComponent } from './assign-rider-modal/assign-rider-modal.component';
import { RiderDetailsModalComponent } from './rider-details-modal/rider-details-modal.component';

// Extend Transaction with delivery assignment fields
interface EscrowWithDelivery extends Transaction {
  riderId?: string;
  riderName?: string;
  assignmentId?: string;
  assignmentStatus?: DeliveryAssignmentStatus;
  pickupAddress?: string;
  dropoffAddress?: string;
  pickedUpAt?: string;
  deliveredAt?: string;
}

@Component({
  selector: 'app-rider-assignments',
  standalone: true,
  imports: [CommonModule, FormsModule, AssignRiderModalComponent, RiderDetailsModalComponent],
  templateUrl: './assignments.html',
  styleUrls: ['./assignments.css']
})
export class RiderAssignmentsComponent implements OnInit {
  private dataService = inject(DataService);
  private apiService = inject(ApiService);
  private notificationService = inject(NotificationService);
  private router = inject(Router);
  private appEnvironment = inject(AppEnvironmentService);

  // ===== STATE =====
  loading = signal(true);
  escrows = signal<EscrowWithDelivery[]>([]);
  riders = signal<Rider[]>([]);
  filterStatus = signal<string>('ALL');
  searchTerm = signal<string>('');
  maxConcurrentDeliveries = 4;

  // ===== MODAL STATE =====
  showAssignModal = signal(false);
  selectedEscrowId = signal('');
  selectedEscrowTitle = signal('');

  showRiderDetailsModal = signal(false);
  selectedRiderIdForDetails = signal('');
  showAssignButtonInDetails = signal(false);

  // ===== COMPUTED =====
  filteredEscrows = computed(() => {
    let items = this.escrows();
    const status = this.filterStatus();
    
    if (status !== 'ALL') {
      if (status === 'UNASSIGNED') {
        items = items.filter(e => !e.riderId);
      } else if (status === 'ASSIGNED') {
        items = items.filter(e => 
          e.riderId && 
          ['ASSIGNED', 'ACCEPTED', 'PICKED_UP', 'IN_TRANSIT', 'ARRIVED_AT_BUYER'].includes(e.assignmentStatus || '')
        );
      } else if (status === 'IN_TRANSIT') {
        items = items.filter(e => e.assignmentStatus === 'IN_TRANSIT');
      } else if (status === 'DELIVERED') {
        items = items.filter(e => e.assignmentStatus === 'DELIVERED_TO_BUYER');
      } else if (status === 'COMPLETED') {
        items = items.filter(e => e.status === 'COMPLETED');
      } else if (status === 'DISPUTED') {
        items = items.filter(e => e.status === 'DISPUTED');
      }
    }

    const term = this.searchTerm().toLowerCase();
    if (term) {
      items = items.filter(e =>
        e.id.toLowerCase().includes(term) ||
        e.buyer?.toLowerCase().includes(term) ||
        e.seller?.toLowerCase().includes(term) ||
        e.buyerId?.toLowerCase().includes(term) ||
        e.sellerId?.toLowerCase().includes(term)
      );
    }

    return items;
  });

  totalEscrows = computed(() => this.escrows().length);
  assignedEscrows = computed(() => this.escrows().filter(e => e.riderId && e.assignmentStatus !== 'CANCELLED').length);
  unassignedEscrows = computed(() => this.escrows().filter(e => !e.riderId).length);
  inTransitEscrows = computed(() => this.escrows().filter(e => e.assignmentStatus === 'IN_TRANSIT').length);

  // ===== LIFECYCLE =====
  ngOnInit() {
    console.log('🔄 RiderAssignmentsComponent initialized');
    this.loadEscrows();
    this.loadRiders();
  }

  // ===== DATA LOADING =====
  async loadEscrows() {
    console.log('📥 Loading escrows...');
    this.loading.set(true);

    // Wait for DataService to load data (mock or API)
    await this.dataService.waitForData();

    // Now try to get transactions from DataService
    const dataServiceEscrows = this.dataService.transactions();
    if (dataServiceEscrows && dataServiceEscrows.length > 0) {
      console.log('✅ Using escrows from DataService:', dataServiceEscrows.length);
      const mappedEscrows = dataServiceEscrows.map((tx: Transaction) => ({
        ...tx,
        riderId: tx.riderId,
        riderName: tx.riderName,
        status: tx.status || 'FUNDS_HELD',
        assignmentStatus: tx.riderId ? 'ASSIGNED' as DeliveryAssignmentStatus : undefined
      }));
      this.escrows.set(mappedEscrows as EscrowWithDelivery[]);
      this.loading.set(false);
      return;
    }

    // If still no data, try API
    console.log('⏳ No data in DataService, trying API...');
    this.apiService.getTransactions({ size: 100 }).subscribe({
      next: (response: any) => {
        console.log('✅ Escrows loaded from API:', response);
        const escrows = (response.content || []).map((tx: any) => ({
          ...tx,
          riderId: tx.riderId || undefined,
          riderName: tx.riderName || undefined,
          status: tx.status || 'FUNDS_HELD',
          assignmentStatus: tx.riderId ? 'ASSIGNED' as DeliveryAssignmentStatus : undefined
        }));
        this.escrows.set(escrows);
        this.loading.set(false);
      },
      error: (err: any) => {
        console.error('❌ Failed to load escrows:', err);
        this.loading.set(false);
        this.notificationService.add('Error', 'Could not load escrows.', 'danger');
      }
    });
  }

  async loadRiders() {
    console.log('📥 Loading riders...');

    // Wait for DataService to load data (mock or API)
    await this.dataService.waitForData();

    // Now try to get riders from DataService
    const dataServiceRiders = this.dataService.users().filter(u => u.role === 'RIDER') as Rider[];
    if (dataServiceRiders && dataServiceRiders.length > 0) {
      console.log('✅ Using riders from DataService:', dataServiceRiders.length);
      this.riders.set(dataServiceRiders);
      return;
    }

    // If still no data, try API
    console.log('⏳ No riders in DataService, trying API...');
    this.apiService.getRiders({ size: 100 }).subscribe({
      next: (response: any) => {
        console.log('✅ Riders loaded from API:', response);
        const riders = (response.content || []).map((r: any) => ({
          ...r,
          role: 'RIDER'
        })) as Rider[];
        this.riders.set(riders);
      },
      error: (err: any) => {
        console.error('❌ Failed to load riders:', err);
        this.notificationService.add('Error', 'Could not load riders.', 'danger');
      }
    });
  }

  refreshData() {
    this.loadEscrows();
    this.loadRiders();
  }

  filterEscrows() {
    // Computed will update automatically
  }

  // ===== STATUS BADGE HELPERS =====

  getStatusBadge(status: string): string {
    const badges: Record<string, string> = {
      'FUNDS_HELD': 'bg-yellow-100 text-yellow-800',
      'IN_DELIVERY': 'bg-blue-100 text-blue-800',
      'DELIVERED_TO_BUYER': 'bg-green-100 text-green-800',
      'COMPLETED': 'bg-green-100 text-green-800',
      'DISPUTED': 'bg-red-100 text-red-800',
      'CANCELLED': 'bg-gray-100 text-gray-800',
      'ASSIGNED': 'bg-purple-100 text-purple-800',
      'ACCEPTED': 'bg-indigo-100 text-indigo-800',
      'PICKED_UP': 'bg-cyan-100 text-cyan-800',
      'IN_TRANSIT': 'bg-blue-100 text-blue-800',
      'ARRIVED_AT_BUYER': 'bg-teal-100 text-teal-800',
      'FAILED': 'bg-red-100 text-red-800'
    };
    return badges[status] || 'bg-gray-100 text-gray-600';
  }

  getAssignmentStatusBadge(status: string): string {
    const badges: Record<string, string> = {
      'ASSIGNED': 'bg-purple-100 text-purple-800',
      'ACCEPTED': 'bg-indigo-100 text-indigo-800',
      'PICKED_UP': 'bg-cyan-100 text-cyan-800',
      'IN_TRANSIT': 'bg-blue-100 text-blue-800',
      'ARRIVED_AT_BUYER': 'bg-teal-100 text-teal-800',
      'DELIVERED_TO_BUYER': 'bg-green-100 text-green-800',
      'FAILED': 'bg-red-100 text-red-800',
      'CANCELLED': 'bg-gray-100 text-gray-800'
    };
    return badges[status] || 'bg-gray-100 text-gray-600';
  }

  // ========== MODAL METHODS ==========

  openAssignModal(escrow: EscrowWithDelivery) {
    console.log('🟢 Opening assign modal for:', escrow.id);
    this.selectedEscrowId.set(escrow.id);
    this.selectedEscrowTitle.set(escrow.title || 'Escrow');
    this.showAssignModal.set(true);
  }

  openAssignModalForCurrentEscrow() {
    const escrow = this.escrows().find(e => e.id === this.selectedEscrowId());
    if (escrow) {
      this.openAssignModal(escrow);
    } else {
      this.notificationService.add('Error', 'No escrow selected to assign.', 'warning');
    }
  }

  closeAssignModal() {
    console.log('🔴 Closing assign modal');
    this.showAssignModal.set(false);
    this.selectedEscrowId.set('');
    this.selectedEscrowTitle.set('');
  }

  onRiderAssigned(event: { riderId: string; riderName: string }) {
    console.log('✅ Rider assigned:', event);
    this.escrows.update(items =>
      items.map(e =>
        e.id === this.selectedEscrowId()
          ? { 
              ...e, 
              riderId: event.riderId, 
              riderName: event.riderName,
              assignmentStatus: 'ASSIGNED' as DeliveryAssignmentStatus
            }
          : e
      )
    );
    this.closeAssignModal();
    this.loadRiders();
  }

  viewRiderDetails(riderId: string) {
    console.log('🔍 Opening rider details for:', riderId);
    this.selectedRiderIdForDetails.set(riderId);
    this.showAssignButtonInDetails.set(!!this.selectedEscrowId());
    this.showRiderDetailsModal.set(true);
  }

  closeRiderDetailsModal() {
    console.log('🔴 Closing rider details modal');
    this.showRiderDetailsModal.set(false);
    this.selectedRiderIdForDetails.set('');
    this.showAssignButtonInDetails.set(false);
  }

  // ========== NAVIGATION ==========

  viewDetails(escrowId: string) {
    console.log('🔍 Navigating to escrow details:', escrowId);
    this.router.navigate(['/transactions', escrowId]);
  }

  viewRider(riderId: string) {
    console.log('🔍 View rider:', riderId);
    this.viewRiderDetails(riderId);
  }

  // ========== DISPLAY HELPERS FOR RIDER LIST ==========

  getDisplayNameInitial(rider: any): string {
    if (!rider || !rider.displayName) return 'R';
    return rider.displayName.charAt(0).toUpperCase();
  }

  getRiderStatus(rider: any): string {
    if (rider.status !== 'ACTIVE') {
      return `🔴 ${rider.status}`;
    }
    if (rider.riderStatus === 'BUSY') {
      return `🟡 Busy (${rider.activeDeliveries}/${rider.maxDeliveries})`;
    }
    if (!rider.isAvailable) {
      return `🔴 At capacity (${rider.activeDeliveries}/${rider.maxDeliveries})`;
    }
    return `✅ Available (${rider.activeDeliveries}/${rider.maxDeliveries})`;
  }

  getRiderStatusClass(rider: any): string {
    if (rider.status !== 'ACTIVE') {
      return 'text-red-600';
    }
    if (rider.riderStatus === 'BUSY') {
      return 'text-yellow-600';
    }
    if (!rider.isAvailable) {
      return 'text-red-600';
    }
    return 'text-green-600';
  }

  // ========== AVAILABLE RIDERS FOR ASSIGN MODAL ==========

  get availableRiders(): any[] {
    const term = this.searchTerm().toLowerCase();
    let filtered = this.riders();
    
    if (term) {
      filtered = filtered.filter((r: Rider) =>
        r.displayName?.toLowerCase().includes(term) ||
        r.phone?.toLowerCase().includes(term)
      );
    }

    return filtered.map((rider: Rider) => {
      const activeDeliveries = this.escrows().filter(e => 
        e.riderId === rider.id && 
        ['FUNDS_HELD', 'ASSIGNED', 'ACCEPTED', 'PICKED_UP', 'IN_TRANSIT', 'ARRIVED_AT_BUYER'].includes(e.status || '')
      ).length;
      
      let riderStatus: 'AVAILABLE' | 'BUSY' | 'OFFLINE' | 'SUSPENDED' = 'OFFLINE';
      if (rider.status === 'ACTIVE' && activeDeliveries === 0) {
        riderStatus = 'AVAILABLE';
      } else if (rider.status === 'ACTIVE' && activeDeliveries > 0) {
        riderStatus = 'BUSY';
      } else if (rider.status === 'SUSPENDED') {
        riderStatus = 'SUSPENDED';
      }
      
      return {
        ...rider,
        activeDeliveries,
        maxDeliveries: this.maxConcurrentDeliveries,
        isAvailable: activeDeliveries < this.maxConcurrentDeliveries && rider.status === 'ACTIVE',
        riderStatus: riderStatus
      };
    });
  }
}
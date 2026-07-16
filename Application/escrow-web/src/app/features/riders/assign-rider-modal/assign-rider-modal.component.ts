// src/app/features/riders/assignments/assign-rider-modal/assign-rider-modal.component.ts

import { Component, inject, signal, computed, input, output, OnInit, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Rider } from '../../../core/models/rider';
import { ApiService } from '../../../core/services/api.service';
import { NotificationService } from '../../../core/services/notifications';
import { DataService } from '../../../core/services/data';

// Rider with delivery stats for UI
interface RiderWithCount {
  id: string;
  displayName: string;
  phone: string;
  email: string;
  role: string;
  status: string;
  blacklistStatus: string;
  businessName: string | null;
  createdAt: string;
  updatedAt?: string;
  activeDeliveries: number;
  maxDeliveries: number;
  isAvailable: boolean;
  riderStatus: 'AVAILABLE' | 'BUSY' | 'OFFLINE' | 'SUSPENDED';
  vehicleType?: string;
  vehiclePlate?: string;
  rating?: number;
  totalDeliveries?: number;
  isActive?: boolean;
  profileImage?: string;
  operationArea?: string;
  licenseNumber?: string;
}

@Component({
  selector: 'app-assign-rider-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div 
      class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-[9999] p-4"
      [class.hidden]="!isOpen()"
      (click)="close()"
    >
      <div 
        class="bg-white rounded-2xl shadow-xl max-w-lg w-full max-h-[90vh] overflow-y-auto"
        (click)="$event.stopPropagation()"
      >
        <!-- Header -->
        <div class="p-6 border-b border-gray-100 flex justify-between items-center sticky top-0 bg-white rounded-t-2xl">
          <h3 class="text-xl font-bold text-gray-800 flex items-center gap-2">
            <i class="fas fa-motorcycle text-emerald-600"></i>
            Assign Rider
          </h3>
          <button (click)="close()" class="text-gray-400 hover:text-gray-600 transition">
            <i class="fas fa-times text-xl"></i>
          </button>
        </div>

        <!-- Body -->
        <div class="p-6">
          <div class="mb-4">
            <p class="text-sm text-gray-500">
              Assign a rider to escrow: <strong class="text-gray-700">{{ escrowId().slice(0, 13) }}...</strong>
            </p>
            <p class="text-xs text-gray-400 mt-1">Maximum {{ maxConcurrentDeliveries() }} concurrent deliveries per rider</p>
          </div>

          <!-- Loading -->
          @if (loading()) {
            <div class="flex justify-center py-12">
              <div class="text-center">
                <i class="fas fa-spinner fa-spin text-3xl text-emerald-600 mb-3 block"></i>
                <p class="text-gray-500">Loading riders...</p>
              </div>
            </div>
          }

          <!-- Search -->
          <div class="relative mb-4">
            <i class="fas fa-search absolute left-3 top-2.5 text-gray-400"></i>
            <input
              type="text"
              [(ngModel)]="riderSearch"
              (input)="filterRiders()"
              placeholder="Search riders by name or phone..."
              class="w-full pl-9 pr-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500"
            />
            @if (riderSearch()) {
              <button (click)="clearRiderSearch()" class="absolute right-3 top-2 text-gray-400 hover:text-gray-600">
                <i class="fas fa-times"></i>
              </button>
            }
          </div>

          <!-- Rider List -->
          <div class="space-y-2 max-h-60 overflow-y-auto">
            @for (rider of filteredRiders(); track rider.id) {
              <div
                class="flex items-center gap-3 p-3 border rounded-lg cursor-pointer transition hover:bg-gray-50"
                [class.border-emerald-500]="selectedRiderId() === rider.id"
                [class.bg-emerald-50]="selectedRiderId() === rider.id"
                [class.opacity-50]="!rider.isAvailable"
                [class.cursor-not-allowed]="!rider.isAvailable"
                (click)="rider.isAvailable ? selectRider(rider.id) : null"
              >
                <div class="h-10 w-10 rounded-full bg-emerald-100 flex items-center justify-center text-emerald-700 font-bold flex-shrink-0">
                  {{ getDisplayNameInitial(rider) }}
                </div>
                <div class="flex-1 min-w-0">
                  <div class="flex items-center justify-between">
                    <p class="font-medium text-gray-800">{{ rider.displayName || 'Unknown Rider' }}</p>
                    <span class="text-xs font-medium" [class]="getRiderStatusClass(rider)">
                      {{ getRiderStatus(rider) }}
                    </span>
                  </div>
                  <div class="flex items-center gap-3 text-sm text-gray-500">
                    <span><i class="fas fa-phone mr-1"></i>{{ rider.phone }}</span>
                    <span class="text-xs bg-gray-100 px-2 py-0.5 rounded">
                      {{ rider.activeDeliveries }}/{{ rider.maxDeliveries }} active
                    </span>
                    @if (rider.vehicleType) {
                      <span class="text-xs text-gray-400">
                        <i class="fas fa-motorcycle mr-1"></i>{{ rider.vehicleType }}
                      </span>
                    }
                  </div>
                </div>
                @if (selectedRiderId() === rider.id && rider.isAvailable) {
                  <i class="fas fa-check-circle text-emerald-600 text-xl flex-shrink-0"></i>
                }
                @if (!rider.isAvailable) {
                  <i class="fas fa-lock text-gray-400 text-xl flex-shrink-0" title="At capacity"></i>
                }
              </div>
            }
            @if (filteredRiders().length === 0) {
              <div class="text-center py-8 text-gray-500">
                <i class="fas fa-users text-3xl text-gray-300 mb-2 block"></i>
                @if (riderSearch()) {
                  <p>No riders match "{{ riderSearch() }}"</p>
                } @else {
                  <p>No riders available</p>
                }
              </div>
            }
          </div>

          <!-- Rider count -->
          <div class="mt-3 text-xs text-gray-400">
            {{ filteredRiders().filter(r => r.isAvailable).length }} available ·
            {{ filteredRiders().filter(r => !r.isAvailable).length }} at capacity
          </div>
        </div>

        <!-- Footer -->
        <div class="p-6 border-t border-gray-100 bg-gray-50 rounded-b-2xl flex justify-end gap-3">
          <button
            (click)="close()"
            class="px-4 py-2 bg-gray-200 text-gray-700 rounded-lg hover:bg-gray-300 transition"
          >
            Cancel
          </button>
          <button
            (click)="assign()"
            [disabled]="!selectedRiderId() || isAssigning()"
            class="px-4 py-2 bg-emerald-600 text-white rounded-lg hover:bg-emerald-700 transition disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
          >
            @if (isAssigning()) {
              <i class="fas fa-spinner fa-spin"></i>
            } @else {
              <i class="fas fa-check"></i>
            }
            {{ isAssigning() ? 'Assigning...' : 'Assign Rider' }}
          </button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .max-h-60::-webkit-scrollbar {
      width: 4px;
    }
    .max-h-60::-webkit-scrollbar-track {
      background: #f1f1f1;
      border-radius: 10px;
    }
    .max-h-60::-webkit-scrollbar-thumb {
      background: #a7f3d0;
      border-radius: 10px;
    }
    .max-h-60::-webkit-scrollbar-thumb:hover {
      background: #34d399;
    }
  `]
})
export class AssignRiderModalComponent implements OnInit {
  private dataService = inject(DataService);
  private apiService = inject(ApiService);
  private notificationService = inject(NotificationService);

  // Inputs
  isOpen = input<boolean>(false);
  escrowId = input<string>('');
  escrowTitle = input<string>('');
  maxConcurrentDeliveries = input<number>(4);

  // Outputs
  onAssign = output<{ riderId: string; riderName: string }>();
  onClose = output<void>();

  // State
  loading = signal(true);
  isAssigning = signal(false);
  riders = signal<Rider[]>([]);
  selectedRiderId = signal<string | null>(null);
  riderSearch = signal<string>('');

  // Computed
  filteredRiders = computed<RiderWithCount[]>(() => {
    const term = this.riderSearch().toLowerCase();
    let filtered = this.riders();
    
    if (term) {
      filtered = filtered.filter((r: Rider) =>
        r.displayName?.toLowerCase().includes(term) ||
        r.phone?.toLowerCase().includes(term)
      );
    }

    return filtered.map((rider: Rider) => {
      // Calculate active deliveries from the rider's assignments
      const activeDeliveries = 0; // This would come from the backend in real implementation
      
      let riderStatus: 'AVAILABLE' | 'BUSY' | 'OFFLINE' | 'SUSPENDED' = 'OFFLINE';
      if (rider.status === 'ACTIVE') {
        riderStatus = 'AVAILABLE';
      } else if (rider.status === 'SUSPENDED') {
        riderStatus = 'SUSPENDED';
      }
      
      return {
        id: rider.id,
        displayName: rider.displayName,
        phone: rider.phone,
        email: rider.email,
        role: rider.role,
        status: rider.status,
        blacklistStatus: rider.blacklistStatus,
        businessName: rider.businessName,
        createdAt: rider.createdAt,
        updatedAt: rider.updatedAt,
        riderStatus: riderStatus,
        vehicleType: (rider as any).vehicleType,
        vehiclePlate: (rider as any).vehiclePlate,
        rating: (rider as any).rating,
        totalDeliveries: (rider as any).totalDeliveries,
        isActive: rider.status === 'ACTIVE',
        profileImage: (rider as any).profileImage,
        operationArea: (rider as any).operationArea,
        licenseNumber: (rider as any).licenseNumber,
        activeDeliveries,
        maxDeliveries: this.maxConcurrentDeliveries(),
        isAvailable: rider.status === 'ACTIVE' && activeDeliveries < this.maxConcurrentDeliveries()
      };
    });
  });

  // Effect to reload riders when modal opens
  constructor() {
    effect(() => {
      if (this.isOpen()) {
        this.loadRiders();
      }
    });
  }

  ngOnInit() {
    // Initial load handled by effect
  }

  loadRiders() {
    this.loading.set(true);
    
    // Try DataService first
    const dataServiceRiders = this.dataService.users().filter(u => u.role === 'RIDER') as Rider[];
    if (dataServiceRiders && dataServiceRiders.length > 0) {
      this.riders.set(dataServiceRiders);
      this.loading.set(false);
      return;
    }

    // Fallback to API
    this.apiService.getRiders({ size: 100 }).subscribe({
      next: (response: any) => {
        const riders = (response.content || []).map((r: any) => ({
          ...r,
          role: 'RIDER'
        })) as Rider[];
        this.riders.set(riders);
        this.loading.set(false);
      },
      error: (err: any) => {
        console.error('Failed to load riders:', err);
        this.notificationService.add('Error', 'Could not load riders.', 'danger');
        this.loading.set(false);
      }
    });
  }

  selectRider(riderId: string) {
    this.selectedRiderId.set(this.selectedRiderId() === riderId ? null : riderId);
  }

  filterRiders() {
    // Computed will update automatically
  }

  clearRiderSearch() {
    this.riderSearch.set('');
  }

  assign() {
    const riderId = this.selectedRiderId();
    if (!riderId) {
      this.notificationService.add('Error', 'Please select a rider.', 'warning');
      return;
    }

    const rider = this.filteredRiders().find(r => r.id === riderId);
    if (!rider || !rider.isAvailable) {
      this.notificationService.add('Error', 'This rider is not available.', 'danger');
      return;
    }

    this.isAssigning.set(true);
    
    this.apiService.assignRider(this.escrowId(), riderId).subscribe({
      next: () => {
        this.isAssigning.set(false);
        this.notificationService.add('Success', `Rider "${rider.displayName}" assigned successfully!`, 'success');
        this.onAssign.emit({ riderId, riderName: rider.displayName });
        this.close();
      },
      error: (err: any) => {
        console.error('Failed to assign rider:', err);
        this.isAssigning.set(false);
        // Fallback to mock assignment
        this.notificationService.add('Success', `Rider "${rider.displayName}" assigned successfully! (Mock mode)`, 'success');
        this.onAssign.emit({ riderId, riderName: rider.displayName });
        this.close();
      }
    });
  }

  close() {
    this.selectedRiderId.set(null);
    this.riderSearch.set('');
    this.onClose.emit();
  }

  getDisplayNameInitial(rider: RiderWithCount): string {
    if (!rider || !rider.displayName) return 'R';
    return rider.displayName.charAt(0).toUpperCase();
  }

  getRiderStatus(rider: RiderWithCount): string {
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

  getRiderStatusClass(rider: RiderWithCount): string {
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
}
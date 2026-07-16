// src/app/features/transactions/components/assign-rider-modal/assign-rider-modal.component.ts

import { Component, inject, signal, input, output, OnInit, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService, ApiUserDetails, RiderProfileResponse } from '../../../../core/services/api.service';
import { NotificationService } from '../../../../core/services/notifications';

@Component({
  selector: 'app-assign-rider-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    @if (isOpen()) {
      <div class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4" (click)="close()">
        <div class="bg-white rounded-2xl shadow-xl max-w-lg w-full max-h-[90vh] overflow-y-auto" (click)="$event.stopPropagation()">
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
                Assign a rider to escrow: <strong class="text-gray-700">{{ transactionId() }}</strong>
              </p>
              <p class="text-xs text-gray-400 mt-1">
                Select a rider from the list below. Only active riders are shown.
              </p>
            </div>

            <!-- Loading -->
            @if (loading()) {
              <div class="flex justify-center py-12">
                <div class="text-center">
                  <i class="fas fa-spinner fa-spin text-3xl text-emerald-600 mb-3 block"></i>
                  <p class="text-gray-500">Loading available riders...</p>
                </div>
              </div>
            }

            <!-- Rider List -->
            @if (!loading()) {
              <!-- Search -->
              <div class="relative mb-4">
                <input
                  type="text"
                  [(ngModel)]="searchTerm"
                  (input)="filterRiders()"
                  placeholder="Search riders by name or phone..."
                  class="w-full px-3 py-2 pl-9 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-emerald-500 text-sm"
                />
                <i class="fas fa-search absolute left-3 top-2.5 text-gray-400"></i>
                @if (searchTerm()) {
                  <button (click)="clearSearch()" class="absolute right-3 top-2.5 text-gray-400 hover:text-gray-600">
                    <i class="fas fa-times"></i>
                  </button>
                }
              </div>

              <div class="space-y-2 max-h-60 overflow-y-auto">
                @for (rider of filteredRiders(); track rider.id) {
                  <div
                    class="flex items-center gap-3 p-3 border rounded-lg cursor-pointer transition hover:bg-gray-50"
                    [class.border-emerald-500]="selectedRiderId() === rider.id"
                    [class.bg-emerald-50]="selectedRiderId() === rider.id"
                    (click)="selectRider(rider.id)"
                  >
                    <!-- Avatar -->
                    <div class="h-10 w-10 rounded-full bg-emerald-100 flex items-center justify-center text-emerald-700 font-bold flex-shrink-0">
                      {{ rider.displayName?.charAt(0)?.toUpperCase() || 'R' }}
                    </div>

                    <!-- Info -->
                    <div class="flex-1 min-w-0">
                      <p class="font-medium text-gray-800">{{ rider.displayName || 'Unknown Rider' }}</p>
                      <div class="flex items-center gap-3 text-sm text-gray-500">
                        <span><i class="fas fa-phone mr-1"></i>{{ rider.phone }}</span>
                        <span class="text-xs bg-gray-100 px-2 py-0.5 rounded">{{ rider.role || 'RIDER' }}</span>
                      </div>
                    </div>

                    <!-- Checkmark -->
                    @if (selectedRiderId() === rider.id) {
                      <i class="fas fa-check-circle text-emerald-600 text-xl flex-shrink-0"></i>
                    }
                  </div>
                }

                @if (filteredRiders().length === 0) {
                  <div class="text-center py-8 text-gray-500">
                    <i class="fas fa-users text-4xl text-gray-300 mb-2 block"></i>
                    @if (searchTerm()) {
                      <p>No riders match "{{ searchTerm() }}"</p>
                    } @else {
                      <p>No riders available</p>
                    }
                  </div>
                }
              </div>

              <!-- Rider count -->
              <div class="mt-3 text-xs text-gray-400">
                {{ filteredRiders().length }} rider{{ filteredRiders().length !== 1 ? 's' : '' }} available
              </div>
            }
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
              [disabled]="!selectedRiderId() || assigning()"
              class="px-4 py-2 bg-emerald-600 text-white rounded-lg hover:bg-emerald-700 transition disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
            >
              @if (assigning()) {
                <i class="fas fa-spinner fa-spin"></i>
              } @else {
                <i class="fas fa-check"></i>
              }
              {{ assigning() ? 'Assigning...' : 'Assign Rider' }}
            </button>
          </div>
        </div>
      </div>
    }
  `
})
export class AssignRiderModalComponent implements OnInit {
  private apiService = inject(ApiService);
  private notificationService = inject(NotificationService);

  // Inputs
  isOpen = input<boolean>(false);
  transactionId = input<string>('');
  escrowTitle = input<string>('');

  // Outputs
  onAssign = output<{ transactionId: string; riderId: string; riderName: string }>();
  onClose = output<void>();

  // State
  loading = signal(true);
  assigning = signal(false);
  riders = signal<ApiUserDetails[]>([]);
  selectedRiderId = signal<string | null>(null);
  searchTerm = signal('');

  // Computed
  filteredRiders = computed(() => {
    const term = this.searchTerm().toLowerCase();
    if (!term) return this.riders();
    return this.riders().filter(rider =>
      rider.displayName?.toLowerCase().includes(term) ||
      rider.phone?.toLowerCase().includes(term) ||
      rider.email?.toLowerCase().includes(term)
    );
  });

  ngOnInit() {
    if (this.isOpen()) {
      this.loadRiders();
    }
  }

  loadRiders() {
    this.loading.set(true);
    this.apiService.getRiders({ size: 100 }).subscribe({
      next: (response) => {
        this.riders.set(response.content || []);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Failed to load riders:', err);
        this.notificationService.add('Error', 'Could not load riders. Please try again.', 'danger');
        this.loading.set(false);
      }
    });
  }

  selectRider(riderId: string) {
    this.selectedRiderId.set(this.selectedRiderId() === riderId ? null : riderId);
  }

  filterRiders() {
    // The computed filteredRiders will update automatically
  }

  clearSearch() {
    this.searchTerm.set('');
  }

  assign() {
    const riderId = this.selectedRiderId();
    const transactionId = this.transactionId();

    if (!riderId || !transactionId) return;

    this.assigning.set(true);
    this.apiService.assignRider(transactionId, riderId).subscribe({
      next: () => {
        const riderName = this.riders().find(r => r.id === riderId)?.displayName || 'Unknown Rider';
        this.assigning.set(false);
        this.notificationService.add('Success', `Rider "${riderName}" assigned successfully!`, 'success');
        this.onAssign.emit({ transactionId, riderId, riderName });
        this.close();
      },
      error: (err) => {
        console.error('Failed to assign rider:', err);
        this.assigning.set(false);
        this.notificationService.add('Error', 'Failed to assign rider. Please try again.', 'danger');
      }
    });
  }

  close() {
    this.selectedRiderId.set(null);
    this.searchTerm.set('');
    this.onClose.emit();
  }
}
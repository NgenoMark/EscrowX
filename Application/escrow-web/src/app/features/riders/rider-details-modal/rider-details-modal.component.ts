// src/app/features/riders/assignments/rider-details-modal/rider-details-modal.component.ts

import { Component, inject, signal, input, output, OnInit, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { ApiService } from '../../../core/services/api.service';
import { NotificationService } from '../../../core/services/notifications';
import { DataService } from '../../../core/services/data';
import { AppEnvironmentService } from '../../../core/config/app-environment';
import { Rider } from '../../../core/models/rider';

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
  selector: 'app-rider-details-modal',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div 
      class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-[9999] p-4"
      [class.hidden]="!isOpen()"
      (click)="close()"
    >
      <div 
        class="bg-white rounded-2xl shadow-xl max-w-2xl w-full max-h-[90vh] overflow-y-auto"
        (click)="$event.stopPropagation()"
      >
        <!-- Header -->
        <div class="p-6 border-b border-gray-100 flex justify-between items-center sticky top-0 bg-white rounded-t-2xl z-10">
          <h3 class="text-xl font-bold text-gray-800 flex items-center gap-2">
            <i class="fas fa-motorcycle text-emerald-600"></i>
            Rider Details
          </h3>
          <button (click)="close()" class="text-gray-400 hover:text-gray-600 transition">
            <i class="fas fa-times text-xl"></i>
          </button>
        </div>

        <!-- Loading State -->
        @if (loading()) {
          <div class="flex flex-col items-center justify-center py-16">
            <i class="fas fa-spinner fa-spin text-4xl text-emerald-600 mb-4"></i>
            <p class="text-gray-500">Loading rider details...</p>
          </div>
        }

        <!-- Rider Details -->
        @if (rider(); as rider) {
          <div class="p-6">
            <!-- Profile Header -->
            <div class="flex items-center gap-4 mb-6 pb-6 border-b border-gray-100">
              <div class="h-20 w-20 rounded-full bg-emerald-100 flex items-center justify-center text-emerald-700 text-3xl font-bold flex-shrink-0">
                {{ getDisplayNameInitial(rider) }}
              </div>
              <div class="flex-1">
                <div class="flex items-center gap-3 flex-wrap">
                  <h4 class="text-2xl font-bold text-gray-800">{{ rider.displayName }}</h4>
                  <span class="inline-flex px-2 py-0.5 text-xs font-medium rounded-full" [class]="getRiderStatusClass(rider)">
                    {{ rider.riderStatus || rider.status }}
                  </span>
                  @if (rider.isAvailable) {
                    <span class="inline-flex px-2 py-0.5 text-xs font-medium rounded-full bg-green-100 text-green-800">
                      <i class="fas fa-check-circle mr-1"></i> Available
                    </span>
                  } @else {
                    <span class="inline-flex px-2 py-0.5 text-xs font-medium rounded-full bg-red-100 text-red-800">
                      <i class="fas fa-times-circle mr-1"></i> Unavailable
                    </span>
                  }
                </div>
                <p class="text-sm text-gray-500 flex items-center gap-2 mt-1">
                  <i class="fas fa-envelope"></i> {{ rider.email }}
                </p>
                <p class="text-sm text-gray-500 flex items-center gap-2">
                  <i class="fas fa-phone"></i> {{ rider.phone }}
                </p>
                <p class="text-xs text-gray-400 font-mono mt-1">ID: {{ rider.id }}</p>
              </div>
              <div class="text-right">
                <div class="flex items-center gap-1 text-sm">
                  <span class="text-yellow-500">⭐</span>
                  <span class="font-semibold">{{ rider.rating || 'N/A' }}</span>
                </div>
                <p class="text-xs text-gray-400">{{ rider.totalDeliveries || 0 }} deliveries</p>
              </div>
            </div>

            <!-- Details Grid -->
            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
              <!-- Status Card -->
              <div class="bg-gray-50 rounded-xl p-4">
                <p class="text-xs text-gray-500 font-semibold uppercase tracking-wider">Status</p>
                <div class="mt-2 space-y-1">
                  <div class="flex justify-between">
                    <span class="text-sm text-gray-600">Account Status</span>
                    <span class="text-sm font-medium" [class.text-green-600]="rider.status === 'ACTIVE'" [class.text-red-600]="rider.status !== 'ACTIVE'">
                      {{ rider.status }}
                    </span>
                  </div>
                  <div class="flex justify-between">
                    <span class="text-sm text-gray-600">Rider Status</span>
                    <span class="text-sm font-medium" [class.text-green-600]="rider.riderStatus === 'AVAILABLE'" [class.text-yellow-600]="rider.riderStatus === 'BUSY'" [class.text-gray-600]="rider.riderStatus === 'OFFLINE'">
                      {{ rider.riderStatus }}
                    </span>
                  </div>
                  <div class="flex justify-between">
                    <span class="text-sm text-gray-600">Blacklist</span>
                    <span class="text-sm font-medium" [class.text-red-600]="rider.blacklistStatus !== 'NOT_BLACKLISTED'">
                      {{ rider.blacklistStatus }}
                    </span>
                  </div>
                </div>
              </div>

              <!-- Vehicle Info -->
              <div class="bg-gray-50 rounded-xl p-4">
                <p class="text-xs text-gray-500 font-semibold uppercase tracking-wider">Vehicle</p>
                <div class="mt-2 space-y-1">
                  <div class="flex justify-between">
                    <span class="text-sm text-gray-600">Type</span>
                    <span class="text-sm font-medium">{{ rider.vehicleType || 'N/A' }}</span>
                  </div>
                  <div class="flex justify-between">
                    <span class="text-sm text-gray-600">Plate</span>
                    <span class="text-sm font-medium font-mono">{{ rider.vehiclePlate || 'N/A' }}</span>
                  </div>
                  @if (rider.operationArea) {
                    <div class="flex justify-between">
                      <span class="text-sm text-gray-600">Operation Area</span>
                      <span class="text-sm font-medium">{{ rider.operationArea }}</span>
                    </div>
                  }
                  @if (rider.licenseNumber) {
                    <div class="flex justify-between">
                      <span class="text-sm text-gray-600">License</span>
                      <span class="text-sm font-medium font-mono">{{ rider.licenseNumber }}</span>
                    </div>
                  }
                </div>
              </div>

              <!-- Delivery Stats -->
              <div class="bg-gray-50 rounded-xl p-4">
                <p class="text-xs text-gray-500 font-semibold uppercase tracking-wider">Delivery Stats</p>
                <div class="mt-2 space-y-1">
                  <div class="flex justify-between">
                    <span class="text-sm text-gray-600">Active Deliveries</span>
                    <span class="text-sm font-medium text-yellow-600">{{ rider.activeDeliveries }}</span>
                  </div>
                  <div class="flex justify-between">
                    <span class="text-sm text-gray-600">Max Capacity</span>
                    <span class="text-sm font-medium">{{ rider.maxDeliveries }}</span>
                  </div>
                  <div class="flex justify-between">
                    <span class="text-sm text-gray-600">Total Deliveries</span>
                    <span class="text-sm font-medium text-emerald-600">{{ rider.totalDeliveries || 0 }}</span>
                  </div>
                  <div class="flex justify-between">
                    <span class="text-sm text-gray-600">Availability</span>
                    <span class="text-sm font-medium" [class.text-green-600]="rider.isAvailable" [class.text-red-600]="!rider.isAvailable">
                      {{ rider.isAvailable ? 'Available' : 'Unavailable' }}
                    </span>
                  </div>
                </div>
              </div>

              <!-- Rider Actions -->
              <div class="bg-gray-50 rounded-xl p-4">
                <p class="text-xs text-gray-500 font-semibold uppercase tracking-wider">Actions</p>
                <div class="mt-3 flex flex-wrap gap-2">
                  @if (rider.isAvailable && showAssignButton()) {
                    <button 
                      (click)="onAssignToCurrent.emit(); close()"
                      class="px-3 py-1.5 bg-emerald-100 text-emerald-700 rounded-lg text-xs font-medium hover:bg-emerald-200 transition"
                    >
                      <i class="fas fa-motorcycle mr-1"></i> Assign to Current
                    </button>
                  }
                  <button 
                    (click)="navigateToUser(rider.id)"
                    class="px-3 py-1.5 bg-indigo-100 text-indigo-700 rounded-lg text-xs font-medium hover:bg-indigo-200 transition"
                  >
                    <i class="fas fa-user mr-1"></i> View Profile
                  </button>
                  <button 
                    (click)="close()"
                    class="px-3 py-1.5 bg-gray-100 text-gray-700 rounded-lg text-xs font-medium hover:bg-gray-200 transition"
                  >
                    <i class="fas fa-times mr-1"></i> Close
                  </button>
                </div>
              </div>
            </div>

            <!-- Created At -->
            <div class="mt-4 pt-4 border-t border-gray-100 text-xs text-gray-400">
              <p>Joined: {{ rider.createdAt | date:'medium' }}</p>
              @if (rider.updatedAt && rider.updatedAt !== rider.createdAt) {
                <p>Last Updated: {{ rider.updatedAt | date:'medium' }}</p>
              }
            </div>
          </div>
        }

        <!-- Not Found State -->
        @if (!loading() && !rider()) {
          <div class="p-12 text-center">
            <i class="fas fa-user-slash text-5xl text-gray-300 mb-4 block"></i>
            <h4 class="text-lg font-semibold text-gray-700">Rider Not Found</h4>
            <p class="text-sm text-gray-400 mt-2">The rider you're looking for could not be found.</p>
            <button 
              (click)="close()" 
              class="mt-4 px-4 py-2 bg-gray-200 text-gray-700 rounded-lg hover:bg-gray-300 transition"
            >
              Close
            </button>
          </div>
        }

        <!-- Footer -->
        @if (rider()) {
          <div class="p-6 border-t border-gray-100 bg-gray-50 rounded-b-2xl flex justify-end">
            <button 
              (click)="close()" 
              class="px-4 py-2 bg-gray-200 text-gray-700 rounded-lg hover:bg-gray-300 transition"
            >
              Close
            </button>
          </div>
        }
      </div>
    </div>
  `,
  styles: []
})
export class RiderDetailsModalComponent implements OnInit, OnChanges {
  private apiService = inject(ApiService);
  private notificationService = inject(NotificationService);
  private router = inject(Router);
  private dataService = inject(DataService);
  private appEnvironment = inject(AppEnvironmentService);

  // Inputs
  isOpen = input<boolean>(false);
  riderId = input<string>('');
  showAssignButton = input<boolean>(false);

  // Outputs
  onClose = output<void>();
  onAssignToCurrent = output<void>();

  // State
  loading = signal(true);
  rider = signal<RiderWithCount | null>(null);

  ngOnInit() {
    // Initial load handled by ngOnChanges
  }

  ngOnChanges(changes: SimpleChanges) {
    // When the modal opens and riderId changes, load the data
    if ((changes['isOpen'] && this.isOpen() && this.riderId()) ||
        (changes['riderId'] && this.isOpen() && this.riderId())) {
      this.loadRiderDetails();
    }
  }

  loadRiderDetails() {
    console.log('📥 Loading rider details for ID:', this.riderId());
    console.log('🔧 useMockData:', this.appEnvironment.useMockData);
    this.loading.set(true);

    // Check if we're in mock mode
    if (this.appEnvironment.useMockData) {
      console.log('📦 Using mock data for rider details');
      this.loadRiderFromMockData();
      return;
    }

    // Otherwise, try API
    this.loadRiderFromApi();
  }

  loadRiderFromMockData() {
    // Find the rider in DataService (mock data)
    const dataServiceRiders = this.dataService.users().filter(u => u.role === 'RIDER') as Rider[];
    const foundRider = dataServiceRiders.find(r => r.id === this.riderId());
    
    if (foundRider) {
      console.log('✅ Rider found in DataService (mock):', foundRider.displayName);
      const mappedRider: RiderWithCount = {
        id: foundRider.id,
        displayName: foundRider.displayName,
        phone: foundRider.phone,
        email: foundRider.email,
        role: foundRider.role,
        status: foundRider.status,
        blacklistStatus: foundRider.blacklistStatus,
        businessName: foundRider.businessName,
        createdAt: foundRider.createdAt,
        updatedAt: foundRider.updatedAt,
        activeDeliveries: 0,
        maxDeliveries: 4,
        isAvailable: foundRider.status === 'ACTIVE',
        riderStatus: foundRider.status === 'ACTIVE' ? 'AVAILABLE' : 'OFFLINE',
        vehicleType: (foundRider as any).vehicleType,
        vehiclePlate: (foundRider as any).vehiclePlate,
        rating: (foundRider as any).rating || 0,
        totalDeliveries: (foundRider as any).totalDeliveries || 0,
        isActive: foundRider.status === 'ACTIVE',
        profileImage: (foundRider as any).profileImage || '',
        operationArea: (foundRider as any).operationArea,
        licenseNumber: (foundRider as any).licenseNumber
      };
      this.rider.set(mappedRider);
      this.loading.set(false);
    } else {
      console.error('❌ Rider not found in mock data:', this.riderId());
      this.loading.set(false);
      this.notificationService.add('Info', 'Rider not found in mock data.', 'info');
    }
  }

  loadRiderFromApi() {
    console.log('⏳ Loading rider from API...');
    this.apiService.getRiderProfile(this.riderId()).subscribe({
      next: (response: any) => {
        console.log('✅ Rider details loaded from API:', response);
        const riderData = response?.data;
        if (riderData) {
          const mappedRider: RiderWithCount = {
            id: riderData.userId || riderData.id,
            displayName: riderData.fullName || riderData.displayName,
            phone: riderData.phoneNumber || riderData.phone,
            email: riderData.email || '',
            role: 'RIDER',
            status: riderData.isActive ? 'ACTIVE' : 'INACTIVE',
            blacklistStatus: 'NOT_BLACKLISTED',
            businessName: null,
            createdAt: riderData.createdAt || new Date().toISOString(),
            updatedAt: riderData.updatedAt || riderData.createdAt,
            activeDeliveries: 0,
            maxDeliveries: 4,
            isAvailable: riderData.isActive || false,
            riderStatus: riderData.riderStatus || 'OFFLINE',
            vehicleType: riderData.vehicleType,
            vehiclePlate: riderData.vehiclePlate,
            rating: riderData.rating || 0,
            totalDeliveries: riderData.totalDeliveries || 0,
            isActive: riderData.isActive || false,
            profileImage: riderData.profileImage || '',
            operationArea: riderData.operationArea,
            licenseNumber: riderData.licenseNumber
          };
          this.rider.set(mappedRider);
        }
        this.loading.set(false);
      },
      error: (err: any) => {
        console.error('❌ Failed to load rider details from API:', err);
        this.loading.set(false);
        this.notificationService.add('Error', 'Could not load rider details. Please try again.', 'danger');
      }
    });
  }

  close() {
    console.log('🔴 Closing rider details modal');
    this.onClose.emit();
  }

  navigateToUser(userId: string) {
    this.router.navigate(['/users', userId]);
    this.close();
  }

  getDisplayNameInitial(rider: RiderWithCount): string {
    if (!rider || !rider.displayName) return 'R';
    return rider.displayName.charAt(0).toUpperCase();
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
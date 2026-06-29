// src/app/features/dashboard/dashboard.ts
import { Component, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { DataService } from '../../core/services/data';
import { SearchService } from '../../core/services/search';
import { AuthService } from '../../core/services/auth.service';
import { StatCardComponent } from '../../shared/ui/stat-card/stat-card';
import { NgxSmkSkeletonDirective } from 'ngxsmk-skeleton-loader';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    StatCardComponent,
    NgxSmkSkeletonDirective
  ],
  templateUrl: './dashboard.html',
  styleUrls: ['./dashboard.css']
})
export class DashboardComponent {
  private dataService = inject(DataService);
  private searchService = inject(SearchService);
  private authService = inject(AuthService);
  private router = inject(Router);

  // Global loading state
  isLoading = this.dataService.isLoading;

  // --- Time-based greeting ---
  greeting = computed(() => {
    const hour = new Date().getHours();
    let timeGreeting = 'Good morning';
    if (hour >= 12 && hour < 17) timeGreeting = 'Good afternoon';
    else if (hour >= 17) timeGreeting = 'Good evening';

    const user = this.authService.user();
    const name = user?.email?.split('@')[0] || 'Admin';
    return `${timeGreeting}, ${name}`;
  });

  // --- Today's date ---
  todayDate = computed(() => {
    const now = new Date();
    return now.toLocaleDateString('en-KE', {
      weekday: 'long',
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  });

  // --- Ops Metrics ---
  activeEscrows = computed(() => {
    return this.dataService.transactions().filter(tx => tx.status === 'FUNDS_HELD').length;
  });

  openDisputes = computed(() => {
    return this.dataService.disputes().filter(d =>
      d.status === 'PENDING' || d.status === 'UNDER_REVIEW' || d.status === 'OPEN'
    ).length;
  });

  autoReleasesToday = computed(() => {
    const today = new Date().toDateString();
    return this.dataService.transactions().filter(tx => {
      if (!tx.autoReleaseDate) return false;
      return new Date(tx.autoReleaseDate).toDateString() === today;
    }).length;
  });

  todayVolume = computed(() => {
    const today = new Date().toDateString();
    return this.dataService.transactions()
      .filter(tx => new Date(tx.created).toDateString() === today)
      .reduce((sum, tx) => sum + tx.amount, 0);
  });

  // --- Action Queue ---
  stalledEscrows = computed(() => {
    return this.dataService.transactions()
      .filter(tx => tx.status === 'FUNDS_HELD')
      .sort((a, b) => new Date(a.created).getTime() - new Date(b.created).getTime())
      .slice(0, 5);
  });

  urgentDisputes = computed(() => {
    return this.dataService.disputes()
      .filter(d => d.status === 'PENDING' || d.status === 'UNDER_REVIEW' || d.status === 'OPEN')
      .sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime())
      .slice(0, 5);
  });

  // --- Navigation ---
  goToTransactions(): void {
    this.router.navigate(['/transactions']);
  }

  goToDisputes(): void {
    this.router.navigate(['/disputes']);
  }

  goToAnalytics(): void {
    this.router.navigate(['/analytics']);
  }

  goToPayouts(): void {
    this.router.navigate(['/payouts']);
  }

  navigateToTransaction(id: string): void {
    this.searchService.setQuery(id);
    this.router.navigate(['/transactions']);
  }

  navigateToDispute(id: string): void {
    this.searchService.setQuery(id);
    this.router.navigate(['/disputes']);
  }

  // --- Helpers ---
  getStatusClass(status: string): string {
    switch(status) {
      case 'FUNDS_HELD': return 'bg-yellow-100 text-yellow-800';
      case 'COMPLETED': return 'bg-green-100 text-green-800';
      case 'DISPUTED': return 'bg-red-100 text-red-800';
      case 'REFUNDED': return 'bg-gray-100 text-gray-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  }

  getDisputeStatusClass(status: string): string {
    switch(status) {
      case 'PENDING': return 'bg-yellow-100 text-yellow-800';
      case 'UNDER_REVIEW': return 'bg-blue-100 text-blue-800';
      case 'RESOLVED': return 'bg-green-100 text-green-800';
      case 'ESCALATED': return 'bg-red-100 text-red-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  }

  daysSince(dateString: string): number {
    const diff = new Date().getTime() - new Date(dateString).getTime();
    return Math.floor(diff / (1000 * 60 * 60 * 24));
  }

  formatCurrency(value: number): string {
    return 'KES ' + value.toLocaleString();
  }
}
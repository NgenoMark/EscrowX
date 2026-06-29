// src/app/features/analytics/analytics.ts
import { Component, inject, signal, computed, OnInit, OnDestroy, ElementRef, ViewChild, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DataService } from '../../core/services/data';
import { SearchService } from '../../core/services/search';
import Chart from 'chart.js/auto';

@Component({
  selector: 'app-analytics',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './analytics.html',
  styleUrls: ['./analytics.css']
})
export class AnalyticsComponent implements OnInit, OnDestroy, AfterViewInit {
  private dataService = inject(DataService);
  private searchService = inject(SearchService);

  // Date range filter
  dateRange = signal<'week' | 'month' | 'quarter' | 'year'>('month');

  // Chart references
  private volumeChart: Chart | null = null;
  private disputeChart: Chart | null = null;
  private distributionChart: Chart | null = null;
  private growthChart: Chart | null = null;

  // DOM element references
  @ViewChild('volumeChartCanvas') volumeChartCanvas!: ElementRef<HTMLCanvasElement>;
  @ViewChild('disputeChartCanvas') disputeChartCanvas!: ElementRef<HTMLCanvasElement>;
  @ViewChild('distributionChartCanvas') distributionChartCanvas!: ElementRef<HTMLCanvasElement>;
  @ViewChild('growthChartCanvas') growthChartCanvas!: ElementRef<HTMLCanvasElement>;

  // --- Shared Color Palette for Distribution Chart & Custom Legend ---
  private distributionColors = [
    '#3b82f6', // Blue
    '#10b981', // Emerald
    '#f59e0b', // Amber
    '#ef4444', // Red
    '#8b5cf6', // Violet
    '#ec4899', // Pink
    '#14b8a6', // Teal
    '#f97316'  // Orange
  ];

  // --- 1. Date Range Helper ---
  private getDateRangeLimits() {
    const now = new Date();
    let startDate = new Date();
    switch (this.dateRange()) {
      case 'week': startDate.setDate(now.getDate() - 7); break;
      case 'month': startDate.setMonth(now.getMonth() - 1); break;
      case 'quarter': startDate.setMonth(now.getMonth() - 3); break;
      case 'year': startDate.setFullYear(now.getFullYear() - 1); break;
    }
    return startDate;
  }

  // Filter transactions by the selected date range
  private filteredTransactions = computed(() => {
    const start = this.getDateRangeLimits();
    return this.dataService.transactions().filter(tx => new Date(tx.created) >= start);
  });

  // Filter disputes by the selected date range
  private filteredDisputes = computed(() => {
    const start = this.getDateRangeLimits();
    return this.dataService.disputes().filter(d => new Date(d.createdAt) >= start);
  });

  // --- 2. Macro BI Metrics (Strategic) ---
  totalVolume = computed(() => {
    return this.filteredTransactions().reduce((sum, tx) => sum + tx.amount, 0);
  });

  totalFees = computed(() => {
    return this.totalVolume() * 0.025;
  });

  avgTransactionValue = computed(() => {
    const count = this.filteredTransactions().length;
    return count === 0 ? 0 : this.totalVolume() / count;
  });

  successRate = computed(() => {
    const total = this.filteredTransactions().length;
    if (total === 0) return 0;
    const completed = this.filteredTransactions().filter(tx => tx.status === 'COMPLETED').length;
    return (completed / total) * 100;
  });

  totalUsers = computed(() => {
    return this.dataService.users().length;
  });

  // --- 3. Month-over-Month Growth ---
  monthOverMonthVolume = computed(() => {
    const now = new Date();
    const currentMonthStart = new Date(now.getFullYear(), now.getMonth(), 1);
    const lastMonthStart = new Date(now.getFullYear(), now.getMonth() - 1, 1);
    const lastMonthEnd = new Date(now.getFullYear(), now.getMonth(), 0);

    const currentVolume = this.dataService.transactions()
      .filter(tx => new Date(tx.created) >= currentMonthStart)
      .reduce((sum, tx) => sum + tx.amount, 0);

    const lastVolume = this.dataService.transactions()
      .filter(tx => new Date(tx.created) >= lastMonthStart && new Date(tx.created) <= lastMonthEnd)
      .reduce((sum, tx) => sum + tx.amount, 0);

    if (lastVolume === 0) return currentVolume > 0 ? 100 : 0;
    return ((currentVolume - lastVolume) / lastVolume) * 100;
  });

  // --- 4. Top Sellers ---
  topSellers = computed(() => {
    const sellers = new Map<string, { name: string; volume: number; transactions: number }>();
    this.filteredTransactions().forEach(tx => {
      const current = sellers.get(tx.seller) || { name: tx.seller, volume: 0, transactions: 0 };
      current.volume += tx.amount;
      current.transactions += 1;
      sellers.set(tx.seller, current);
    });
    return Array.from(sellers.values())
      .sort((a, b) => b.volume - a.volume)
      .slice(0, 5);
  });

  // --- 5. Chart Data Helpers (Computed for Template) ---
  volumeChartData = computed(() => {
    const data = this.filteredTransactions();
    const map = new Map<string, number>();
    data.forEach(tx => {
      const date = new Date(tx.created).toLocaleDateString('en-KE', { month: 'short', day: 'numeric' });
      map.set(date, (map.get(date) || 0) + tx.amount);
    });
    const sorted = Array.from(map.entries()).sort((a, b) => new Date(a[0]).getTime() - new Date(b[0]).getTime());
    return { labels: sorted.map(s => s[0]), data: sorted.map(s => s[1]) };
  });

  disputeTrendData = computed(() => {
    const data = this.filteredDisputes();
    const map = new Map<string, number>();
    data.forEach(d => {
      const date = new Date(d.createdAt).toLocaleDateString('en-KE', { month: 'short', day: 'numeric' });
      map.set(date, (map.get(date) || 0) + 1);
    });
    const sorted = Array.from(map.entries()).sort((a, b) => new Date(a[0]).getTime() - new Date(b[0]).getTime());
    return { labels: sorted.map(s => s[0]), data: sorted.map(s => s[1]) };
  });

  // 👇 Exposed to the template for the custom legend
  distributionData = computed(() => {
    const txns = this.filteredTransactions();
    const map = new Map<string, number>();
    txns.forEach(tx => {
      map.set(tx.status, (map.get(tx.status) || 0) + 1);
    });
    return { labels: Array.from(map.keys()), data: Array.from(map.values()) };
  });

  growthData = computed(() => {
    const users = this.dataService.users();
    const map = new Map<string, number>();
    users.forEach(u => {
      const date = new Date(u.createdAt).toLocaleDateString('en-KE', { month: 'short', year: 'numeric' });
      map.set(date, (map.get(date) || 0) + 1);
    });
    const sorted = Array.from(map.entries()).sort((a, b) => new Date(a[0]).getTime() - new Date(b[0]).getTime());
    return { labels: sorted.map(s => s[0]), data: sorted.map(s => s[1]) };
  });

  // 👇 Helper for template to get the correct color for each custom legend item
  getDistributionColor(index: number): string {
    return this.distributionColors[index % this.distributionColors.length];
  }

  // --- Lifecycle ---
  ngOnInit() {
    console.log('Analytics (BI Hub) initialized');
  }

  ngAfterViewInit() {
    setTimeout(() => this.initCharts(), 300);
  }

  ngOnDestroy() {
    this.destroyCharts();
  }

  onDateRangeChange(): void {
    this.refreshCharts();
  }

  // --- Chart Management ---
  private initCharts(): void {
    this.initVolumeChart();
    this.initDisputeChart();
    this.initDistributionChart();
    this.initGrowthChart();
  }

  private destroyCharts(): void {
    this.volumeChart?.destroy();
    this.disputeChart?.destroy();
    this.distributionChart?.destroy();
    this.growthChart?.destroy();
    this.volumeChart = null;
    this.disputeChart = null;
    this.distributionChart = null;
    this.growthChart = null;
  }

  private refreshCharts(): void {
    this.destroyCharts();
    this.initCharts();
  }

  // --- Volume Chart ---
  private initVolumeChart(): void {
    if (!this.volumeChartCanvas?.nativeElement) return;
    const data = this.volumeChartData();
    this.volumeChart = new Chart(this.volumeChartCanvas.nativeElement, {
      type: 'line',
      data: {
        labels: data.labels,
        datasets: [{
          label: 'Transaction Volume (KES)',
          data: data.data,
          borderColor: '#2c7be5',
          backgroundColor: 'rgba(44, 123, 229, 0.1)',
          tension: 0.3,
          fill: true,
          pointBackgroundColor: '#ffffff',
          pointBorderColor: '#2c7be5',
          pointBorderWidth: 2,
          pointRadius: 0,
          pointHoverRadius: 6,
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false },
          tooltip: {
            callbacks: {
              label: (ctx: any) => `KES ${ctx.parsed.y.toLocaleString()}`
            }
          }
        },
        scales: {
          y: { beginAtZero: true, position: 'right', grid: { color: '#e9edf4' } },
          x: { grid: { display: false } }
        }
      }
    });
  }

  // --- Disputes Bar Chart ---
  private initDisputeChart(): void {
    if (!this.disputeChartCanvas?.nativeElement) return;
    const data = this.disputeTrendData();
    this.disputeChart = new Chart(this.disputeChartCanvas.nativeElement, {
      type: 'bar',
      data: {
        labels: data.labels,
        datasets: [{
          label: 'Disputes Filed',
          data: data.data,
          backgroundColor: '#ef4444',
          borderRadius: 6,
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: true,
        plugins: { legend: { display: false } },
        scales: { y: { beginAtZero: true, ticks: { stepSize: 1 } } }
      }
    });
  }

  // --- FIXED: Distribution Doughnut Chart (Legend DISABLED) ---
  private initDistributionChart(): void {
    if (!this.distributionChartCanvas?.nativeElement) return;
    const data = this.distributionData();

    this.distributionChart = new Chart(this.distributionChartCanvas.nativeElement, {
      type: 'doughnut',
      data: {
        labels: data.labels,
        datasets: [{
          data: data.data,
          backgroundColor: this.distributionColors.slice(0, data.labels.length),
          borderColor: '#ffffff',
          borderWidth: 2.5,
          hoverOffset: 10
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: true,
        cutout: '65%',
        plugins: {
          // 👇 LEGEND DISABLED – we are using a custom HTML legend
          legend: { display: false },
          tooltip: {
            callbacks: {
              label: (ctx: any) => {
                const total = ctx.dataset.data.reduce((a: number, b: number) => a + b, 0);
                if (total === 0) return `${ctx.label}: 0 (0%)`;
                const percentage = ((ctx.parsed / total) * 100).toFixed(1);
                return `${ctx.label}: ${ctx.parsed} (${percentage}%)`;
              }
            }
          }
        }
      }
    });
  }

  // --- User Growth Bar Chart ---
  private initGrowthChart(): void {
    if (!this.growthChartCanvas?.nativeElement) return;
    const data = this.growthData();
    this.growthChart = new Chart(this.growthChartCanvas.nativeElement, {
      type: 'bar',
      data: {
        labels: data.labels,
        datasets: [{
          label: 'New Users',
          data: data.data,
          backgroundColor: '#10b981',
          borderRadius: 6,
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: true,
        plugins: { legend: { display: false } },
        scales: { y: { beginAtZero: true } }
      }
    });
  }

  // --- Helpers ---
  formatCurrency(value: number): string {
    return 'KES ' + value.toLocaleString();
  }

  formatPercentage(value: number): string {
    return value.toFixed(1) + '%';
  }

  formatGrowth(value: number): string {
    const sign = value > 0 ? '+' : '';
    return `${sign}${value.toFixed(1)}%`;
  }

  exportToCSV(): void {
    const data = this.topSellers().map(s => ({
      'Seller': s.name,
      'Volume (KES)': s.volume,
      'Transactions': s.transactions,
      'Avg Tx': (s.volume / s.transactions).toFixed(2)
    }));
    if (!data.length) return alert('No data to export');
    const headers = Object.keys(data[0]);
    const rows = [headers.join(',')];
    data.forEach(row => rows.push(headers.map(h => row[h as keyof typeof row]).join(',')));
    const blob = new Blob([rows.join('\n')], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `escrowx-analytics-${new Date().toISOString().split('T')[0]}.csv`;
    a.click();
    window.URL.revokeObjectURL(url);
  }
}
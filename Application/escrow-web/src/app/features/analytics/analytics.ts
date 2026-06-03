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
  
  // Analytics data
  transactionStats = computed(() => this.dataService.getTransactionStats());
  disputeStats = computed(() => this.dataService.getDisputeStats());
  userStats = computed(() => this.dataService.getUserStats());
  topSellers = computed(() => {
    const term = this.searchService.query().toLowerCase().trim();
    const sellers = this.dataService.getTopSellers(5);

    if (!term) return sellers;

    return sellers.filter(seller =>
      seller.name.toLowerCase().includes(term) ||
      seller.volume.toString().includes(term) ||
      seller.transactions.toString().includes(term)
    );
  });
  
  // Summary metrics
  totalVolume = computed(() => this.transactionStats().totalVolume);
  totalFees = computed(() => this.transactionStats().totalFees);
  successRate = computed(() => {
    const completed = this.transactionStats().completed;
    const total = this.transactionStats().total;
    return total === 0 ? 0 : ((completed / total) * 100).toFixed(1);
  });
  
  averageTransactionValue = computed(() => {
    const total = this.transactionStats().total;
    return total === 0 ? 0 : (this.totalVolume() / total);
  });
  
  ngOnInit() {
    console.log('Analytics component initialized');
  }
  
  ngAfterViewInit() {
    // Initialize charts after view is ready
    setTimeout(() => {
      this.initCharts();
    }, 500);
  }
  
  ngOnDestroy() {
    this.destroyCharts();
  }
  
  onDateRangeChange(): void {
    this.refreshCharts();
  }
  
  private initCharts(): void {
    console.log('Initializing charts...');
    this.initVolumeChart();
    this.initDisputeChart();
    this.initDistributionChart();
    this.initGrowthChart();
  }
  
  private destroyCharts(): void {
    if (this.volumeChart) { this.volumeChart.destroy(); this.volumeChart = null; }
    if (this.disputeChart) { this.disputeChart.destroy(); this.disputeChart = null; }
    if (this.distributionChart) { this.distributionChart.destroy(); this.distributionChart = null; }
    if (this.growthChart) { this.growthChart.destroy(); this.growthChart = null; }
  }
  
  private refreshCharts(): void {
    this.destroyCharts();
    this.initCharts();
  }
  
  private initVolumeChart(): void {
    if (!this.volumeChartCanvas?.nativeElement) {
      console.error('Volume chart canvas not found');
      return;
    }
    
    let volumeData;
    switch(this.dateRange()) {
      case 'week':
        volumeData = this.dataService.getTransactionVolumeByDay(7);
        break;
      case 'month':
        volumeData = this.dataService.getTransactionVolumeByDay(30);
        break;
      case 'quarter':
        volumeData = this.dataService.getTransactionVolumeByDay(90);
        break;
      default:
        volumeData = this.dataService.getTransactionVolumeByDay(30);
    }
    
    this.volumeChart = new Chart(this.volumeChartCanvas.nativeElement, {
      type: 'line',
      data: {
        labels: volumeData.labels,
        datasets: [{
          label: 'Transaction Volume (KES)',
          data: volumeData.data,
          borderColor: '#4f46e5',
          backgroundColor: 'rgba(79, 70, 229, 0.1)',
          tension: 0.4,
          fill: true,
          pointBackgroundColor: '#4f46e5',
          pointBorderColor: '#fff',
          pointBorderWidth: 2,
          pointRadius: 4,
          pointHoverRadius: 6
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: true,
        plugins: {
          legend: { position: 'top' },
          tooltip: { 
            callbacks: {
              label: (context: any) => {
                const value = context.parsed?.y;
                if (value === null || value === undefined) return 'KES 0';
                return `KES ${value.toLocaleString()}`;
              }
            }
          }
        },
        scales: {
          y: {
            beginAtZero: true,
            ticks: {
              callback: (value: any) => {
                if (value === null || value === undefined) return 'KES 0';
                return `KES ${value.toLocaleString()}`;
              }
            }
          }
        }
      }
    });
    console.log('Volume chart created');
  }
  
  private initDisputeChart(): void {
    if (!this.disputeChartCanvas?.nativeElement) {
      console.error('Dispute chart canvas not found');
      return;
    }
    
    const disputeData = this.dataService.getDisputeTrendByWeek(12);
    
    this.disputeChart = new Chart(this.disputeChartCanvas.nativeElement, {
      type: 'bar',
      data: {
        labels: disputeData.labels,
        datasets: [{
          label: 'Disputes Filed',
          data: disputeData.data,
          backgroundColor: '#ef4444',
          borderRadius: 8,
          barPercentage: 0.7
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: true,
        plugins: {
          legend: { position: 'top' },
          tooltip: { 
            callbacks: {
              label: (context: any) => {
                const value = context.parsed?.y;
                if (value === null || value === undefined) return '0 dispute(s)';
                return `${value} dispute(s)`;
              }
            }
          }
        },
        scales: {
          y: {
            beginAtZero: true,
            ticks: { stepSize: 1 }
          }
        }
      }
    });
    console.log('Dispute chart created');
  }
  
  private initDistributionChart(): void {
    if (!this.distributionChartCanvas?.nativeElement) {
      console.error('Distribution chart canvas not found');
      return;
    }
    
    const distribution = this.dataService.getTransactionStatusDistribution();
    
    this.distributionChart = new Chart(this.distributionChartCanvas.nativeElement, {
      type: 'doughnut',
      data: {
        labels: distribution.labels,
        datasets: [{
          data: distribution.data,
          backgroundColor: ['#22c55e', '#eab308', '#ef4444', '#6b7280', '#9ca3af'],
          borderWidth: 0
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: true,
        plugins: {
          legend: { position: 'bottom' },
          tooltip: { 
            callbacks: {
              label: (context: any) => {
                const value = context.parsed;
                if (value === null || value === undefined) return '0 transactions';
                return `${context.label}: ${value} transactions`;
              }
            }
          }
        }
      }
    });
    console.log('Distribution chart created');
  }
  
  private initGrowthChart(): void {
    if (!this.growthChartCanvas?.nativeElement) {
      console.error('Growth chart canvas not found');
      return;
    }
    
    const growthData = this.dataService.getUserGrowth();
    
    this.growthChart = new Chart(this.growthChartCanvas.nativeElement, {
      type: 'bar',
      data: {
        labels: growthData.labels,
        datasets: [{
          label: 'New Users',
          data: growthData.data,
          backgroundColor: '#10b981',
          borderRadius: 8,
          barPercentage: 0.6
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: true,
        plugins: {
          legend: { position: 'top' }
        },
        scales: {
          y: {
            beginAtZero: true,
            ticks: { stepSize: 50 }
          }
        }
      }
    });
    console.log('Growth chart created');
  }
  
  exportToCSV(): void {
    const data = this.topSellers().map(seller => ({
      'Seller Name': seller.name,
      'Transaction Volume (KES)': seller.volume,
      'Number of Transactions': seller.transactions,
      'Average Transaction (KES)': (seller.volume / seller.transactions).toFixed(2)
    }));
    
    if (data.length === 0) {
      alert('No data to export');
      return;
    }
    
    const headers = Object.keys(data[0]);
    const csvRows = [headers.join(',')];
    
    for (const row of data) {
      const values = headers.map(header => {
        const value = row[header as keyof typeof row];
        return typeof value === 'string' && value.includes(',') ? `"${value}"` : value;
      });
      csvRows.push(values.join(','));
    }
    
    const blob = new Blob([csvRows.join('\n')], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `escrowx-analytics-${new Date().toISOString().split('T')[0]}.csv`;
    a.click();
    window.URL.revokeObjectURL(url);
  }
  
  formatCurrency(value: number): string {
    return 'KES ' + value.toLocaleString();
  }
}

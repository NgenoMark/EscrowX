// src/app/shared/ui/stat-card/stat-card.ts
import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-stat-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './stat-card.html',
  styleUrls: ['./stat-card.css']
})
export class StatCardComponent {
  @Input() label = '';
  @Input() value: string | number = '';
  @Input() note = '';
  @Input() icon = 'fa-chart-line';
  @Input() tone: 'default' | 'danger' = 'default';
}
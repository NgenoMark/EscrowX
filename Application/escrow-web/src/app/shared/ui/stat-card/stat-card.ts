import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-stat-card',
  standalone: true,
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

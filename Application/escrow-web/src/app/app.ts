import { Component } from '@angular/core';
import { LayoutComponent } from './shared/layout/layout';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [LayoutComponent],
  templateUrl: './app.html',
  styleUrls: ['./app.css']
})
export class AppComponent {
  title = 'escrow-web';
}

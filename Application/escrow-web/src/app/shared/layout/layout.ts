import { Component, OnInit } from '@angular/core';
import { Router, NavigationEnd, RouterOutlet } from '@angular/router';
import { SidebarComponent } from './sidebar/sidebar';
import { HeaderComponent } from './header/header';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [RouterOutlet, SidebarComponent, HeaderComponent],
  templateUrl: './layout.html',
  styleUrls: ['./layout.css']
})
export class LayoutComponent implements OnInit {
  pageTitle: string = 'Dashboard';
  sidebarCollapsed = false;
  mobileSidebarOpen = false;

  constructor(private router: Router) {}

  ngOnInit() {
    this.router.events.subscribe(event => {
      if (event instanceof NavigationEnd) {
        const path = event.urlAfterRedirects.split('/')[1];
        switch(path) {
          case 'users': this.pageTitle = 'User Management'; break;
          case 'transactions': this.pageTitle = 'Transaction Monitoring'; break;
          case 'disputes': this.pageTitle = 'Dispute Mediation'; break;
          case 'analytics': this.pageTitle = 'Analytics & Reports'; break;
          case 'audit': this.pageTitle = 'Audit Logs'; break;
          default: this.pageTitle = 'Dashboard';
        }
        this.mobileSidebarOpen = false;
      }
    });
  }

  toggleSidebar() {
    if (window.innerWidth < 768) {
      this.mobileSidebarOpen = !this.mobileSidebarOpen;
      return;
    }
    this.sidebarCollapsed = !this.sidebarCollapsed;
  }

  closeMobileSidebar() {
    this.mobileSidebarOpen = false;
  }
}
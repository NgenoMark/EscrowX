import { Routes } from '@angular/router';
import { DashboardComponent } from './features/dashboard/dashboard';
import { UsersComponent } from './features/users/users';
import { TransactionsComponent } from './features/transactions/transactions';
import { DisputesComponent } from './features/disputes/disputes';
import { AnalyticsComponent } from './features/analytics/analytics';
import { AuditComponent } from './features/audit/audit';

export const routes: Routes = [
  { path: 'dashboard', component: DashboardComponent },
  { path: 'users', component: UsersComponent },
  { path: 'transactions', component: TransactionsComponent },
  { path: 'disputes', component: DisputesComponent },
  { path: 'analytics', component: AnalyticsComponent },
  { path: 'audit', component: AuditComponent },
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
  { path: '**', redirectTo: '/dashboard' }
];

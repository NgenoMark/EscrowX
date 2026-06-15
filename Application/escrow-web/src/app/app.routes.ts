import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { DashboardComponent } from './features/dashboard/dashboard';
import { UsersComponent } from './features/users/users';
import { TransactionsComponent } from './features/transactions/transactions';
import { DisputesComponent } from './features/disputes/disputes';
import { AnalyticsComponent } from './features/analytics/analytics';
import { AuditComponent } from './features/audit/audit';
import { AuthCardComponent } from './features/auth/components/auth-card/auth-card';
import { LayoutComponent } from './shared/layout/layout';

export const routes: Routes = [
  { path: 'login', component: AuthCardComponent },
  {
    path: '',
    component: LayoutComponent,
    canActivate: [authGuard],
    children: [
      { path: 'dashboard', component: DashboardComponent },
      { path: 'users', component: UsersComponent },
      { path: 'transactions', component: TransactionsComponent },
      { path: 'disputes', component: DisputesComponent },
      { path: 'analytics', component: AnalyticsComponent },
      { path: 'audit', component: AuditComponent },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  },
  { path: '**', redirectTo: 'dashboard' }
];

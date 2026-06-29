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
import { PaymentIntentsComponent } from './features/payment-intents/payment-intents';
import { PayoutsComponent } from './features/payouts/payouts';
import { TransactionHistoryComponent } from './features/transaction-history/transaction-history';
import { LedgerComponent } from './features/ledger/ledger'; 
import { BuyersComponent } from './features/users/buyers/buyers';
import { AdminsComponent } from './features/users/admins/admins';
import { SellersComponent } from './features/users/sellers/sellers';
export const routes: Routes = [
  { path: 'login', component: AuthCardComponent },
  {
    path: '',
    component: LayoutComponent,
    canActivate: [authGuard],
    children: [
      { path: 'dashboard', component: DashboardComponent },
      { path: 'users', component: UsersComponent },
      { path: 'transactions/:id/history', component: TransactionHistoryComponent },
      { path: 'transactions/:id/ledger', component: LedgerComponent }, 
      { path: 'transactions', component: TransactionsComponent },
      { path: 'disputes', component: DisputesComponent },
      { path: 'analytics', component: AnalyticsComponent },
      { path: 'audit', component: AuditComponent },
      { path: 'admins', component: AdminsComponent},
      { path: 'sellers', component: SellersComponent},
      { path: 'buyers', component: BuyersComponent},
      { path: 'payments', component: PaymentIntentsComponent },
      { path: 'payouts', component: PayoutsComponent },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  },
  { path: '**', redirectTo: 'dashboard' }
];
import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  // Public routes
  { path: 'login',    loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent) },
  { path: 'register', loadComponent: () => import('./features/auth/register/register.component').then(m => m.RegisterComponent) },

  // Protected routes — guarded by authGuard
  {
    path: '',
    canActivate: [authGuard],
    children: [
      { path: '',              redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard',     loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent) },
      { path: 'accounts',      loadComponent: () => import('./features/accounts/account-list/account-list.component').then(m => m.AccountListComponent) },
      { path: 'accounts/:id',  loadComponent: () => import('./features/accounts/account-detail/account-detail.component').then(m => m.AccountDetailComponent) },
      { path: 'deposit',       loadComponent: () => import('./features/transactions/deposit/deposit.component').then(m => m.DepositComponent) },
      { path: 'withdrawal',    loadComponent: () => import('./features/transactions/withdrawal/withdrawal.component').then(m => m.WithdrawalComponent) },
      { path: 'transfer',      loadComponent: () => import('./features/transactions/transfer/transfer.component').then(m => m.TransferComponent) },
      { path: 'notifications', loadComponent: () => import('./features/notifications/notification-list/notification-list.component').then(m => m.NotificationListComponent) },
      { path: 'loans/apply',   loadComponent: () => import('./features/loans/loan-apply/loan-apply.component').then(m => m.LoanApplyComponent) },
      { path: 'loans',         loadComponent: () => import('./features/loans/loan-status/loan-status.component').then(m => m.LoanStatusComponent) },
    ]
  },

  { path: '**', redirectTo: 'dashboard' }
];

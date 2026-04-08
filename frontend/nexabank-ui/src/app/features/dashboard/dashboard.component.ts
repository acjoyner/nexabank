import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule, CurrencyPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { MatChipsModule } from '@angular/material/chips';
import { forkJoin } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { AccountService, AccountResponse } from '../../core/services/account.service';
import { TransactionService, TransactionResponse } from '../../core/services/transaction.service';
import { NotificationService } from '../../core/services/notification.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule, RouterLink, CurrencyPipe,
    MatCardModule, MatButtonModule, MatIconModule,
    MatDividerModule, MatChipsModule
  ],
  template: `
    <h1>Welcome back, {{ customer?.fullName || 'Customer' }}</h1>

    <!-- Account Summary Cards -->
    <div style="display:flex;gap:16px;flex-wrap:wrap;margin-bottom:24px">
      <mat-card *ngFor="let acc of accounts()" style="flex:1;min-width:250px">
        <mat-card-header>
          <mat-icon mat-card-avatar>{{ acc.accountType === 'CHECKING' ? 'account_balance_wallet' : 'savings' }}</mat-icon>
          <mat-card-title>{{ acc.accountType | titlecase }}</mat-card-title>
          <mat-card-subtitle>{{ acc.accountNumber }}</mat-card-subtitle>
        </mat-card-header>
        <mat-card-content>
          <div data-testid="account-balance"
               style="font-size:2rem;font-weight:500;color:#1976d2;margin-bottom:8px">
            {{ acc.balance | currency }}
          </div>
          <mat-chip-set>
            <mat-chip [color]="acc.status === 'ACTIVE' ? 'primary' : 'warn'" selected>
              {{ acc.status }}
            </mat-chip>
          </mat-chip-set>
        </mat-card-content>
        <mat-card-actions>
          <a mat-button [routerLink]="['/accounts', acc.id]">View Details</a>
        </mat-card-actions>
      </mat-card>
    </div>

    <!-- Quick Actions -->
    <mat-card style="margin-bottom:24px">
      <mat-card-header><mat-card-title>Quick Actions</mat-card-title></mat-card-header>
      <mat-card-content>
        <div style="display:flex;gap:12px;flex-wrap:wrap;padding:8px 0">
          <a mat-raised-button color="primary" routerLink="/deposit">
            <mat-icon>add_circle</mat-icon> Deposit
          </a>
          <a mat-raised-button routerLink="/withdrawal">
            <mat-icon>remove_circle</mat-icon> Withdraw
          </a>
          <a mat-raised-button color="accent" routerLink="/transfer">
            <mat-icon>swap_horiz</mat-icon> Transfer
          </a>
          <a mat-raised-button routerLink="/loans/apply">
            <mat-icon>request_page</mat-icon> Apply for Loan
          </a>
        </div>
      </mat-card-content>
    </mat-card>

    <!-- Recent Transactions -->
    <mat-card>
      <mat-card-header>
        <mat-card-title>Recent Transactions</mat-card-title>
      </mat-card-header>
      <mat-card-content>
        <div *ngIf="recentTransactions().length === 0" style="padding:16px;color:#666">
          No recent transactions.
        </div>
        <div *ngFor="let txn of recentTransactions()" style="padding:12px 0;border-bottom:1px solid #eee">
          <div style="display:flex;justify-content:space-between;align-items:center">
            <div>
              <mat-icon style="vertical-align:middle;margin-right:8px;color:#1976d2">
                {{ txn.transactionType === 'DEPOSIT' ? 'arrow_downward' :
                   txn.transactionType === 'WITHDRAWAL' ? 'arrow_upward' : 'swap_horiz' }}
              </mat-icon>
              <strong>{{ txn.transactionType }}</strong>
              <span style="color:#666;margin-left:8px">{{ txn.referenceNumber | slice:0:8 }}...</span>
            </div>
            <div data-testid="txn-amount"
                 [style.color]="txn.transactionType === 'DEPOSIT' ? '#4caf50' : '#f44336'"
                 style="font-size:1.1rem;font-weight:500">
              {{ txn.transactionType === 'DEPOSIT' ? '+' : '-' }}{{ txn.amount | currency }}
            </div>
          </div>
          <div style="color:#999;font-size:0.85rem;margin-left:32px">
            {{ txn.createdAt | date:'medium' }}
          </div>
        </div>
      </mat-card-content>
    </mat-card>
  `
})
export class DashboardComponent implements OnInit {
  private authService = inject(AuthService);
  private accountService = inject(AccountService);
  private transactionService = inject(TransactionService);

  customer = this.authService.getCustomer();
  accounts = signal<AccountResponse[]>([]);
  recentTransactions = signal<TransactionResponse[]>([]);

  ngOnInit(): void {
    const customerId = this.authService.getCustomerId();
    if (!customerId) return;

    this.accountService.getAccounts(customerId).subscribe(accounts => {
      this.accounts.set(accounts);
      if (accounts.length > 0) {
        this.transactionService.getTransactions(accounts[0].id).subscribe(
          txns => this.recentTransactions.set(txns.slice(0, 5))
        );
      }
    });
  }
}

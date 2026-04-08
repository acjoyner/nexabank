import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule, CurrencyPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { AuthService } from '../../../core/services/auth.service';
import { AccountService, AccountResponse } from '../../../core/services/account.service';

@Component({
  selector: 'app-account-list',
  standalone: true,
  imports: [CommonModule, RouterLink, CurrencyPipe, MatCardModule, MatButtonModule, MatIconModule, MatChipsModule],
  template: `
    <h2>My Accounts</h2>
    <div style="display:flex;gap:16px;flex-wrap:wrap">
      <mat-card *ngFor="let acc of accounts()" style="flex:1;min-width:280px">
        <mat-card-header>
          <mat-icon mat-card-avatar>{{ acc.accountType === 'CHECKING' ? 'account_balance_wallet' : 'savings' }}</mat-icon>
          <mat-card-title>{{ acc.accountType | titlecase }} Account</mat-card-title>
          <mat-card-subtitle>{{ acc.accountNumber }}</mat-card-subtitle>
        </mat-card-header>
        <mat-card-content>
          <div style="font-size:2rem;color:#1976d2">{{ acc.balance | currency }}</div>
          <div style="margin-top:8px">
            <mat-chip [color]="acc.status==='ACTIVE'?'primary':'warn'" selected>{{ acc.status }}</mat-chip>
            <span *ngIf="acc.interestRate" style="margin-left:8px;color:#666">{{ acc.interestRate | percent:'1.2-2' }} APY</span>
          </div>
          <div style="color:#999;font-size:0.85rem;margin-top:8px">Opened {{ acc.openedAt | date }}</div>
        </mat-card-content>
        <mat-card-actions>
          <a mat-button [routerLink]="['/accounts', acc.id]">View Transactions</a>
        </mat-card-actions>
      </mat-card>
    </div>
  `
})
export class AccountListComponent implements OnInit {
  private authService = inject(AuthService);
  private accountService = inject(AccountService);
  accounts = signal<AccountResponse[]>([]);
  ngOnInit() {
    const id = this.authService.getCustomerId();
    if (id) this.accountService.getAccounts(id).subscribe(a => this.accounts.set(a));
  }
}

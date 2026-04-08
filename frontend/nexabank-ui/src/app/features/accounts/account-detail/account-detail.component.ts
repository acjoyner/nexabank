import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { CommonModule, CurrencyPipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { AccountService, AccountResponse } from '../../../core/services/account.service';
import { TransactionService, TransactionResponse } from '../../../core/services/transaction.service';

@Component({
  selector: 'app-account-detail',
  standalone: true,
  imports: [CommonModule, CurrencyPipe, MatCardModule, MatIconModule, MatTableModule],
  template: `
    <div *ngIf="account()">
      <mat-card style="margin-bottom:16px">
        <mat-card-header>
          <mat-icon mat-card-avatar>account_balance</mat-icon>
          <mat-card-title>{{ account()!.accountType | titlecase }} — {{ account()!.accountNumber }}</mat-card-title>
          <mat-card-subtitle>Balance: {{ account()!.balance | currency }}</mat-card-subtitle>
        </mat-card-header>
      </mat-card>

      <mat-card>
        <mat-card-header><mat-card-title>Transaction History</mat-card-title></mat-card-header>
        <mat-card-content>
          <table mat-table [dataSource]="transactions()" style="width:100%">
            <ng-container matColumnDef="type">
              <th mat-header-cell *matHeaderCellDef>Type</th>
              <td mat-cell *matCellDef="let t">{{ t.transactionType }}</td>
            </ng-container>
            <ng-container matColumnDef="amount">
              <th mat-header-cell *matHeaderCellDef>Amount</th>
              <td mat-cell *matCellDef="let t">{{ t.amount | currency }}</td>
            </ng-container>
            <ng-container matColumnDef="status">
              <th mat-header-cell *matHeaderCellDef>Status</th>
              <td mat-cell *matCellDef="let t">{{ t.status }}</td>
            </ng-container>
            <ng-container matColumnDef="date">
              <th mat-header-cell *matHeaderCellDef>Date</th>
              <td mat-cell *matCellDef="let t">{{ t.createdAt | date:'short' }}</td>
            </ng-container>
            <ng-container matColumnDef="ref">
              <th mat-header-cell *matHeaderCellDef>Reference</th>
              <td mat-cell *matCellDef="let t">{{ t.referenceNumber | slice:0:8 }}...</td>
            </ng-container>
            <tr mat-header-row *matHeaderRowDef="['type','amount','status','date','ref']"></tr>
            <tr mat-row *matRowDef="let row; columns: ['type','amount','status','date','ref']"></tr>
          </table>
        </mat-card-content>
      </mat-card>
    </div>
  `
})
export class AccountDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private accountService = inject(AccountService);
  private transactionService = inject(TransactionService);
  account = signal<AccountResponse | null>(null);
  transactions = signal<TransactionResponse[]>([]);
  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.accountService.getAccount(id).subscribe(a => this.account.set(a));
    this.transactionService.getTransactions(id).subscribe(t => this.transactions.set(t));
  }
}

import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { AccountService, AccountResponse } from '../../../core/services/account.service';
import { TransactionService, TransactionResponse } from '../../../core/services/transaction.service';

@Component({
  selector: 'app-transfer',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, RouterLink,
    MatCardModule, MatFormFieldModule, MatInputModule,
    MatSelectModule, MatButtonModule, MatIconModule
  ],
  template: `
    <mat-card style="max-width:500px;margin:0 auto">
      <mat-card-header>
        <mat-icon mat-card-avatar color="accent">swap_horiz</mat-icon>
        <mat-card-title>Transfer Funds</mat-card-title>
        <mat-card-subtitle>Between your accounts</mat-card-subtitle>
      </mat-card-header>
      <mat-card-content style="margin-top:16px">
        <form [formGroup]="form" (ngSubmit)="submit()">
          <mat-form-field appearance="outline" style="width:100%">
            <mat-label>From Account</mat-label>
            <mat-select formControlName="sourceAccountId">
              <mat-option *ngFor="let acc of accounts()" [value]="acc.id">
                {{ acc.accountNumber }} — Balance: {{ acc.balance | number:'1.2-2' }}
              </mat-option>
            </mat-select>
          </mat-form-field>
          <mat-form-field appearance="outline" style="width:100%;margin-top:8px">
            <mat-label>To Account</mat-label>
            <mat-select formControlName="destAccountId">
              <mat-option *ngFor="let acc of accounts()" [value]="acc.id">
                {{ acc.accountNumber }}
              </mat-option>
            </mat-select>
          </mat-form-field>
          <mat-form-field appearance="outline" style="width:100%;margin-top:8px">
            <mat-label>Amount</mat-label>
            <span matTextPrefix>$&nbsp;</span>
            <input matInput type="number" formControlName="amount" min="0.01" step="0.01">
          </mat-form-field>
          <mat-form-field appearance="outline" style="width:100%;margin-top:8px">
            <mat-label>Description (optional)</mat-label>
            <input matInput formControlName="description">
          </mat-form-field>
          <p *ngIf="error" style="color:#f44336">{{ error }}</p>
          <div *ngIf="result()" style="padding:12px;background:#e8f5e9;border-radius:4px;margin-top:8px">
            <mat-icon style="color:#4caf50;vertical-align:middle">check_circle</mat-icon>
            Transfer complete! Ref: {{ result()!.referenceNumber | slice:0:8 }}...
          </div>
          <button mat-raised-button color="accent" type="submit"
                  [disabled]="form.invalid || loading" style="width:100%;margin-top:16px">
            {{ loading ? 'Processing...' : 'Transfer Funds' }}
          </button>
        </form>
      </mat-card-content>
    </mat-card>
  `
})
export class TransferComponent implements OnInit {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private accountService = inject(AccountService);
  private transactionService = inject(TransactionService);

  accounts = signal<AccountResponse[]>([]);
  result = signal<TransactionResponse | null>(null);
  loading = false;
  error = '';

  form: FormGroup = this.fb.group({
    sourceAccountId: [null, Validators.required],
    destAccountId:   [null, Validators.required],
    amount:          [null, [Validators.required, Validators.min(0.01)]],
    description:     ['']
  });

  ngOnInit(): void {
    const customerId = this.authService.getCustomerId();
    if (customerId) {
      this.accountService.getAccounts(customerId).subscribe(
        accs => this.accounts.set(accs)
      );
    }
  }

  submit(): void {
    this.loading = true;
    this.error = '';
    const { sourceAccountId, destAccountId, amount, description } = this.form.value;

    this.transactionService.transfer(sourceAccountId, destAccountId, amount, description).subscribe({
      next: res => { this.result.set(res); this.loading = false; },
      error: err => { this.error = err.error?.detail || 'Transfer failed.'; this.loading = false; }
    });
  }
}

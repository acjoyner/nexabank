import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule, CurrencyPipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../../core/services/auth.service';
import { AccountService, AccountResponse } from '../../../core/services/account.service';
import { TransactionService, TransactionResponse } from '../../../core/services/transaction.service';

@Component({
  selector: 'app-withdrawal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatCardModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatButtonModule, MatIconModule, CurrencyPipe],
  template: `
    <mat-card style="max-width:500px;margin:0 auto">
      <mat-card-header>
        <mat-icon mat-card-avatar style="color:#f44336">remove_circle</mat-icon>
        <mat-card-title>Withdraw Funds</mat-card-title>
      </mat-card-header>
      <mat-card-content style="margin-top:16px">
        <form [formGroup]="form" (ngSubmit)="submit()">
          <mat-form-field appearance="outline" style="width:100%">
            <mat-label>Account</mat-label>
            <mat-select formControlName="accountId">
              <mat-option *ngFor="let acc of accounts()" [value]="acc.id">{{ acc.accountNumber }} — {{ acc.balance | number:'1.2-2' }}</mat-option>
            </mat-select>
          </mat-form-field>
          <mat-form-field appearance="outline" style="width:100%;margin-top:8px">
            <mat-label>Amount</mat-label>
            <span matTextPrefix>$&nbsp;</span>
            <input matInput type="number" formControlName="amount" min="0.01">
          </mat-form-field>
          <p *ngIf="error" style="color:#f44336">{{ error }}</p>
          <div *ngIf="result()" style="padding:12px;background:#e8f5e9;border-radius:4px">
            <mat-icon style="color:#4caf50;vertical-align:middle">check_circle</mat-icon>
            Withdrawal of {{ result()!.amount | currency }} complete!
          </div>
          <button mat-raised-button color="warn" type="submit" [disabled]="form.invalid||loading" style="width:100%;margin-top:16px">
            {{ loading ? 'Processing...' : 'Withdraw' }}
          </button>
        </form>
      </mat-card-content>
    </mat-card>
  `
})
export class WithdrawalComponent implements OnInit {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private accountService = inject(AccountService);
  private transactionService = inject(TransactionService);
  accounts = signal<AccountResponse[]>([]);
  result = signal<TransactionResponse | null>(null);
  loading = false; error = '';
  form: FormGroup = this.fb.group({ accountId: [null, Validators.required], amount: [null, [Validators.required, Validators.min(0.01)]] });
  ngOnInit() {
    const id = this.authService.getCustomerId();
    if (id) this.accountService.getAccounts(id).subscribe(a => this.accounts.set(a));
  }
  submit() {
    this.loading = true; this.error = '';
    const { accountId, amount } = this.form.value;
    this.transactionService.withdrawal(accountId, amount).subscribe({
      next: r => {
        this.result.set(r);
        this.loading = false;
        const id = this.authService.getCustomerId();
        if (id) this.accountService.getAccounts(id).subscribe(a => this.accounts.set(a));
      },
      error: e => { this.error = e.error?.detail || 'Withdrawal failed.'; this.loading = false; }
    });
  }
}

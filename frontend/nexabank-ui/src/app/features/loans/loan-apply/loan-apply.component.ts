import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule, CurrencyPipe } from '@angular/common';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatStepperModule } from '@angular/material/stepper';
import { MatChipsModule } from '@angular/material/chips';
import { AuthService } from '../../../core/services/auth.service';
import { AccountService, AccountResponse } from '../../../core/services/account.service';
import { LoanService, LoanApplicationResponse } from '../../../core/services/loan.service';

/**
 * Loan Apply Component — demonstrates:
 * - Multi-step form (MatStepper) with ReactiveFormsModule
 * - Validators.required, Validators.min, Validators.max
 * - Real-time AI eligibility check on submit
 * - Extends the original LoanSearchComponent pattern with full form validation
 *
 * See docs/learning/09-angular-standalone-components.md
 */
@Component({
  selector: 'app-loan-apply',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, CurrencyPipe,
    MatCardModule, MatFormFieldModule, MatInputModule, MatSelectModule,
    MatButtonModule, MatIconModule, MatStepperModule, MatChipsModule
  ],
  template: `
    <mat-card style="max-width:700px;margin:0 auto">
      <mat-card-header>
        <mat-icon mat-card-avatar>request_page</mat-icon>
        <mat-card-title>Loan Application</mat-card-title>
        <mat-card-subtitle>AI-powered eligibility scoring</mat-card-subtitle>
      </mat-card-header>

      <mat-card-content>
        <mat-stepper [linear]="true" #stepper>

          <!-- Step 1: Loan Details -->
          <mat-step [stepControl]="loanForm" label="Loan Details">
            <form [formGroup]="loanForm" style="padding:16px 0">
              <mat-form-field appearance="outline" style="width:100%">
                <mat-label>Loan Type</mat-label>
                <mat-select formControlName="loanType">
                  <mat-option value="PERSONAL">Personal Loan</mat-option>
                  <mat-option value="AUTO">Auto Loan</mat-option>
                  <mat-option value="MORTGAGE">Mortgage</mat-option>
                </mat-select>
              </mat-form-field>

              <div style="display:grid;grid-template-columns:1fr 1fr;gap:16px;margin-top:8px">
                <mat-form-field appearance="outline">
                  <mat-label>Requested Amount ($)</mat-label>
                  <input matInput type="number" formControlName="requestedAmount" min="1000">
                  <mat-error *ngIf="loanForm.get('requestedAmount')?.invalid">$1,000 - $1,000,000</mat-error>
                </mat-form-field>

                <mat-form-field appearance="outline">
                  <mat-label>Term (months)</mat-label>
                  <input matInput type="number" formControlName="termMonths" min="12" max="360">
                  <mat-error *ngIf="loanForm.get('termMonths')?.invalid">12 - 360 months</mat-error>
                </mat-form-field>
              </div>

              <div style="margin-top:16px">
                <button mat-raised-button color="primary" matStepperNext [disabled]="loanForm.invalid">
                  Next <mat-icon>arrow_forward</mat-icon>
                </button>
              </div>
            </form>
          </mat-step>

          <!-- Step 2: Financial Info -->
          <mat-step [stepControl]="financialForm" label="Financial Info">
            <form [formGroup]="financialForm" style="padding:16px 0">
              <mat-form-field appearance="outline" style="width:100%">
                <mat-label>Annual Income ($)</mat-label>
                <input matInput type="number" formControlName="annualIncome" min="10000">
                <mat-error *ngIf="financialForm.get('annualIncome')?.invalid">Minimum $10,000</mat-error>
              </mat-form-field>

              <mat-form-field appearance="outline" style="width:100%;margin-top:8px">
                <mat-label>Credit Score (300-850)</mat-label>
                <input matInput type="number" formControlName="creditScore" min="300" max="850">
                <mat-hint>Your FICO score — leave blank if unknown</mat-hint>
              </mat-form-field>

              <mat-form-field appearance="outline" style="width:100%;margin-top:8px">
                <mat-label>Account</mat-label>
                <mat-select formControlName="accountId">
                  <mat-option *ngFor="let acc of accounts()" [value]="acc.id">
                    {{ acc.accountNumber }} ({{ acc.balance | currency }})
                  </mat-option>
                </mat-select>
              </mat-form-field>

              <div style="display:flex;gap:8px;margin-top:16px">
                <button mat-button matStepperPrevious>Back</button>
                <button mat-raised-button color="primary" (click)="submit()" [disabled]="financialForm.invalid || loading">
                  {{ loading ? 'Processing...' : 'Submit Application' }}
                </button>
              </div>
            </form>
          </mat-step>

          <!-- Step 3: Result -->
          <mat-step label="AI Decision">
            <div *ngIf="result()" style="padding:24px 0;text-align:center">
              <mat-icon [style.color]="getDecisionColor(result()!.aiDecision)"
                        style="font-size:64px;height:64px;width:64px">
                {{ result()!.aiDecision === 'ELIGIBLE' ? 'check_circle' :
                   result()!.aiDecision === 'INELIGIBLE' ? 'cancel' : 'pending' }}
              </mat-icon>
              <h2 [style.color]="getDecisionColor(result()!.aiDecision)">
                {{ result()!.aiDecision === 'ELIGIBLE' ? 'Approved!' :
                   result()!.aiDecision === 'INELIGIBLE' ? 'Not Approved' : 'Under Review' }}
              </h2>
              <p style="max-width:400px;margin:0 auto;color:#666">{{ result()!.aiReason }}</p>
              <mat-chip style="margin-top:12px" [color]="result()!.strategyUsed === 'AI' ? 'primary' : 'warn'" selected>
                Scored by: {{ result()!.strategyUsed }}
              </mat-chip>
              <div style="margin-top:24px">
                <button mat-raised-button color="primary" routerLink="/loans">View My Loans</button>
              </div>
            </div>
          </mat-step>
        </mat-stepper>

        <p *ngIf="error" style="color:#f44336;margin-top:8px">{{ error }}</p>
      </mat-card-content>
    </mat-card>
  `
})
export class LoanApplyComponent implements OnInit {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private accountService = inject(AccountService);
  private loanService = inject(LoanService);

  accounts = signal<AccountResponse[]>([]);
  result = signal<LoanApplicationResponse | null>(null);
  loading = false;
  error = '';

  loanForm: FormGroup = this.fb.group({
    loanType:        ['PERSONAL', Validators.required],
    requestedAmount: [null, [Validators.required, Validators.min(1000), Validators.max(1000000)]],
    termMonths:      [36,   [Validators.required, Validators.min(12), Validators.max(360)]]
  });

  financialForm: FormGroup = this.fb.group({
    annualIncome: [null, [Validators.required, Validators.min(10000)]],
    creditScore:  [null, [Validators.min(300), Validators.max(850)]],
    accountId:    [null, Validators.required]
  });

  ngOnInit(): void {
    const customerId = this.authService.getCustomerId();
    if (customerId) {
      this.accountService.getAccounts(customerId).subscribe(
        accounts => this.accounts.set(accounts)
      );
    }
  }

  submit(): void {
    const customerId = this.authService.getCustomerId();
    if (!customerId) return;

    this.loading = true;
    this.error = '';

    this.loanService.apply({
      customerId,
      ...this.loanForm.value,
      ...this.financialForm.value
    }).subscribe({
      next: (res) => {
        this.result.set(res);
        this.loading = false;
      },
      error: (err) => {
        this.error = err.error?.detail || 'Application failed. Please try again.';
        this.loading = false;
      }
    });
  }

  getDecisionColor(decision?: string): string {
    return decision === 'ELIGIBLE' ? '#4caf50' :
           decision === 'INELIGIBLE' ? '#f44336' : '#ff9800';
  }
}

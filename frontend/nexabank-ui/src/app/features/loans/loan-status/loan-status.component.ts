import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule, CurrencyPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { AuthService } from '../../../core/services/auth.service';
import { LoanService, LoanApplicationResponse } from '../../../core/services/loan.service';

@Component({
  selector: 'app-loan-status',
  standalone: true,
  imports: [CommonModule, RouterLink, CurrencyPipe, MatCardModule, MatButtonModule, MatIconModule, MatChipsModule],
  template: `
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
      <h2>My Loan Applications</h2>
      <a mat-raised-button color="primary" routerLink="/loans/apply">
        <mat-icon>add</mat-icon> New Application
      </a>
    </div>
    <div *ngIf="loans().length === 0" style="text-align:center;padding:48px;color:#666">
      No loan applications yet. <a routerLink="/loans/apply">Apply now</a>
    </div>
    <div style="display:flex;flex-direction:column;gap:16px">
      <mat-card *ngFor="let loan of loans()">
        <mat-card-header>
          <mat-icon mat-card-avatar [style.color]="getStatusColor(loan.status)">request_page</mat-icon>
          <mat-card-title>{{ loan.loanType }} Loan — {{ loan.requestedAmount | currency }}</mat-card-title>
          <mat-card-subtitle>{{ loan.termMonths }} months · Applied {{ loan.appliedAt | date }}</mat-card-subtitle>
        </mat-card-header>
        <mat-card-content>
          <mat-chip [color]="getStatusChipColor(loan.status)" selected>{{ loan.status }}</mat-chip>
          <span *ngIf="loan.aiDecision" style="margin-left:8px">
            AI: <strong>{{ loan.aiDecision }}</strong>
          </span>
          <p *ngIf="loan.aiReason" style="color:#666;margin-top:8px;font-style:italic">{{ loan.aiReason }}</p>
        </mat-card-content>
      </mat-card>
    </div>
  `
})
export class LoanStatusComponent implements OnInit {
  private authService = inject(AuthService);
  private loanService = inject(LoanService);
  loans = signal<LoanApplicationResponse[]>([]);
  ngOnInit() {
    const id = this.authService.getCustomerId();
    if (id) this.loanService.getByCustomer(id).subscribe(l => this.loans.set(l));
  }
  getStatusColor(status: string): string {
    return status === 'APPROVED' ? '#4caf50' : status === 'REJECTED' ? '#f44336' : '#ff9800';
  }
  getStatusChipColor(status: string): 'primary' | 'accent' | 'warn' {
    return status === 'APPROVED' ? 'primary' : status === 'REJECTED' ? 'warn' : 'accent';
  }
}

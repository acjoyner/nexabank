import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface LoanApplicationResponse {
  id: number;
  customerId: number;
  loanType: 'PERSONAL' | 'AUTO' | 'MORTGAGE';
  requestedAmount: number;
  termMonths: number;
  annualIncome: number;
  creditScore?: number;
  status: 'SUBMITTED' | 'AI_REVIEW' | 'APPROVED' | 'REJECTED' | 'CANCELLED';
  aiDecision?: string;
  aiReason?: string;
  strategyUsed?: string;
  appliedAt: string;
  decidedAt?: string;
}

@Injectable({ providedIn: 'root' })
export class LoanService {
  private http = inject(HttpClient);
  private readonly API = '/api/loans';

  apply(data: {
    customerId: number;
    accountId: number;
    loanType: string;
    requestedAmount: number;
    termMonths: number;
    annualIncome: number;
    creditScore?: number;
  }): Observable<LoanApplicationResponse> {
    return this.http.post<LoanApplicationResponse>(`${this.API}/apply`, data);
  }

  getByCustomer(customerId: number): Observable<LoanApplicationResponse[]> {
    return this.http.get<LoanApplicationResponse[]>(`${this.API}/customer/${customerId}`);
  }

  getById(id: number): Observable<LoanApplicationResponse> {
    return this.http.get<LoanApplicationResponse>(`${this.API}/${id}`);
  }
}

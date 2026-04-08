import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface TransactionResponse {
  id: number;
  referenceNumber: string;
  sourceAccountId: number;
  destAccountId?: number;
  transactionType: 'DEPOSIT' | 'WITHDRAWAL' | 'TRANSFER';
  amount: number;
  currency: string;
  status: 'PENDING' | 'COMPLETED' | 'FAILED' | 'REVERSED';
  description?: string;
  createdAt: string;
  completedAt?: string;
}

@Injectable({ providedIn: 'root' })
export class TransactionService {
  private http = inject(HttpClient);
  private readonly API = '/api/transactions';

  deposit(accountId: number, amount: number, description?: string): Observable<TransactionResponse> {
    return this.http.post<TransactionResponse>(`${this.API}/deposit`, { accountId, amount, description });
  }

  withdrawal(accountId: number, amount: number, description?: string): Observable<TransactionResponse> {
    return this.http.post<TransactionResponse>(`${this.API}/withdrawal`, { accountId, amount, description });
  }

  transfer(sourceAccountId: number, destAccountId: number, amount: number, description?: string): Observable<TransactionResponse> {
    return this.http.post<TransactionResponse>(`${this.API}/transfer`, {
      sourceAccountId, destAccountId, amount, description
    });
  }

  getTransactions(accountId: number): Observable<TransactionResponse[]> {
    return this.http.get<TransactionResponse[]>(`${this.API}/account/${accountId}`);
  }
}

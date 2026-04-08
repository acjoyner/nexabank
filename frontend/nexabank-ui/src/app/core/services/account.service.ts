import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface AccountResponse {
  id: number;
  accountNumber: string;
  accountType: 'CHECKING' | 'SAVINGS';
  balance: number;
  status: 'ACTIVE' | 'FROZEN' | 'CLOSED';
  interestRate?: number;
  openedAt: string;
  customerId: number;
  customerEmail: string;
}

export interface BalanceResponse {
  accountId: number;
  accountNumber: string;
  balance: number;
  status: string;
}

@Injectable({ providedIn: 'root' })
export class AccountService {
  private http = inject(HttpClient);
  private readonly API = '/api/accounts';

  getAccounts(customerId: number): Observable<AccountResponse[]> {
    return this.http.get<AccountResponse[]>(`${this.API}/customer/${customerId}`);
  }

  getAccount(accountId: number): Observable<AccountResponse> {
    return this.http.get<AccountResponse>(`${this.API}/${accountId}`);
  }

  getBalance(accountId: number): Observable<BalanceResponse> {
    return this.http.get<BalanceResponse>(`${this.API}/${accountId}/balance`);
  }

  openAccount(customerId: number, accountType: string, initialDeposit?: number): Observable<AccountResponse> {
    return this.http.post<AccountResponse>(`${this.API}/customer/${customerId}`, {
      accountType, initialDeposit
    });
  }
}

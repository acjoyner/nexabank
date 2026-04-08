import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';

export interface AuthResponse {
  token: string;
  tokenType: string;
  expiresAt: string;
  customerId: number;
  email: string;
  fullName: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);

  private readonly TOKEN_KEY = 'nexabank_token';
  private readonly CUSTOMER_KEY = 'nexabank_customer';
  private readonly API = '/api/auth';

  register(data: { email: string; password: string; firstName: string; lastName: string; phone?: string }): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.API}/register`, data).pipe(
      tap(res => this.storeAuth(res))
    );
  }

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.API}/login`, { email, password }).pipe(
      tap(res => this.storeAuth(res))
    );
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.CUSTOMER_KEY);
    this.router.navigate(['/login']);
  }

  isLoggedIn(): boolean {
    return !!localStorage.getItem(this.TOKEN_KEY);
  }

  getCustomer(): AuthResponse | null {
    const data = localStorage.getItem(this.CUSTOMER_KEY);
    return data ? JSON.parse(data) : null;
  }

  getCustomerId(): number | null {
    return this.getCustomer()?.customerId ?? null;
  }

  private storeAuth(res: AuthResponse): void {
    localStorage.setItem(this.TOKEN_KEY, res.token);
    localStorage.setItem(this.CUSTOMER_KEY, JSON.stringify(res));
  }
}
